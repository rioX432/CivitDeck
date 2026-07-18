# SPIKE #982 — Server-side receiver for "send CivitAI model to plain ComfyUI"

Status: research only (no implementation). Decision note.

## Decision

**Ship a tiny companion custom node (Option B) as the primary receiver. Do NOT rely on
ComfyUI-Manager's model-install endpoint (Option A) as the general path.**

The spike's original assumption was "capability-probe A, fall back to guided B." The spike
**overturns that ordering**: Option A's `install_model` endpoint is gated by a curated
**whitelist** (`check_whitelist_for_model`) that matches an incoming request against
Manager's bundled `model-list.json` by `save_path` + `base` + `filename`. An arbitrary model
a user browses on CivitAI is almost never in that list, so the endpoint returns
`400 "Invalid model install request is detected"`. Option A therefore cannot serve as a
general receiver for arbitrary CivitAI downloads — it only installs models Manager already
curates. Option B (a companion node modeled on Civicomfy) resolves the target folder through
ComfyUI's own `folder_paths` API, has no whitelist, and is the only path that actually solves
the "wrong-folder / pure-ComfyUI user gets nothing" pain. Recommendation: **B primary; A kept
only as documentation, not a runtime fast-path.**

---

## Option A findings — ComfyUI-Manager model install

Verified against Manager source `glob/manager_server.py` at `pyproject.toml` **version = "3.41"**
(current `main`).

- **Endpoint:** `POST /manager/queue/install_model`
  Source: https://github.com/Comfy-Org/ComfyUI-Manager/blob/main/glob/manager_server.py (line ~1638, `async def install_model`)
- **Payload** (fields read by the handler / queue item): `ui_id`, `filename`, `url`, `type`,
  `save_path`, `base`. The task is enqueued: `task_queue.put(("install-model", (ui_id, json_data)))`.
- **Type → folder** resolved by `model_dir_name_map` (same file, line ~103):
  ```
  checkpoints/checkpoint/unclip → checkpoints   text_encoders/clip → text_encoders
  vae → vae                                      lora → loras
  t2i-adapter/t2i-style/controlnet → controlnet  clip_vision → clip_vision
  gligen → gligen                                upscale → upscale_models
  embedding/embeddings → embeddings              unet/diffusion_model → diffusion_models
  ```
  Note: keys are lowercased (`data['type'].lower()`), and there is **no `lora` plural, no
  `hypernetwork`, no `motion*`** in this map — those types cannot be installed via Manager.
- **Blockers (the reason A is rejected):**
  1. **Whitelist gate** — `check_whitelist_for_model()` requires the exact
     (`save_path`, `base`, `filename`) triple to already exist in `model-list.json`
     (cache or local). Arbitrary CivitAI models fail with HTTP 400.
  2. **Security-level gate** — requires `is_allowed_security_level('middle')`; non-`.safetensors`
     filenames additionally require `'high'` AND a whitelisted URL. On a non-loopback
     (LAN/remote) ComfyUI the default security level can reject the call with HTTP 403.
- **Progress:** queue-based. Poll `GET /manager/queue/status`
  (`{total_count, done_count, in_progress_count, is_processing}`), start with
  `POST /manager/queue/start`; live updates arrive over ComfyUI's websocket as
  `cm-queue-status` events. (Same source file, routes at ~1326 / ~1432.)
- **Version sensitivity:** the `/manager/queue/*` queue API is the **3.x** shape. Older 2.x
  Manager exposed a different, non-queue `install_model` handler; payloads and the
  `task_queue` mechanism differ. Any A integration would have to pin **Manager ≥ 3.x** and
  probe the version. Given the whitelist blocker this is moot for us.

**Conclusion on A:** endpoint verified from real source, but functionally unusable for
arbitrary CivitAI models due to the curated whitelist. Do not build on it.

---

## Option B findings — companion custom node (Civicomfy pattern)

Verified against Civicomfy source (https://github.com/MoonGoblinDev/Civicomfy).

- **Route registration** — plain aiohttp routes on ComfyUI's server instance:
  ```python
  import server
  prompt_server = server.PromptServer.instance
  @prompt_server.routes.post("/civitai/download")
  async def route_download_model(request): ...
  ```
  (`server/routes/DownloadModel.py`; package auto-registers every route module via
  `server/routes/__init__.py`.) A CivitDeck node would register e.g. `POST /civitdeck/install`.
- **Folder resolution** — done through ComfyUI's canonical `folder_paths` API, NOT a hardcoded
  map (`utils/helpers.py::get_model_dir` → `get_model_folder_paths` →
  `folder_paths.get_folder_paths(key)`), with alias normalization
  (`lora`↔`loras`, `embedding`↔`embeddings`, `upscaler`→`upscale_models`,
  `motionmodule`→`motion_models`, `unet`→`diffusion_models`, `clip`→`text_encoders`).
  This respects `extra_model_paths.yaml`, symlinks, and user-customized roots — the correct
  approach and the direct fix for the "wrong-folder" pain. Falls back to
  `models_dir/<folder_name>` when the type is unknown.
- **Download** — chunked/multi-connection streaming into the resolved dir
  (`downloader/chunk_downloader.py`, `downloader/manager.py` with a background queue thread).
- **Hash verification — NOT present in Civicomfy.** `chunk_downloader.py` verifies **size**
  only (segment size + final file size), not SHA256. So the issue's "verify sha256" is an
  **addition CivitDeck must implement**, not something Civicomfy proves. It is trivial: CivitAI's
  version API exposes `files[].hashes.SHA256`; the node computes `hashlib.sha256` over the
  streamed bytes and rejects on mismatch. Low risk, but call it out as new code.
- **Model-list refresh — essentially automatic.** ComfyUI's `folder_paths.get_filename_list()`
  cache self-invalidates by comparing `os.path.getmtime(folder)` to the stored mtime
  (ComfyUI `folder_paths.py::cached_filename_list_`). Writing a new file into the target folder
  changes the folder mtime, so the next `/object_info` fetch or generation sees the new model
  with no explicit refresh call. The node may optionally warm it by calling
  `folder_paths.get_filename_list(folder_name)` after the write.
  Sources: https://github.com/comfyanonymous/ComfyUI/blob/master/folder_paths.py
- **Architectural note:** Civicomfy takes a CivitAI URL/ID and fetches metadata + download URL
  **server-side** (needs a CivitAI API key on the box). CivitDeck can instead have the phone
  resolve the CDN URL + SHA256 (it already holds them) and POST `{url, type, filename, sha256}`
  to the node — the lean "receiver" shape the issue describes. Either works; the phone-resolves
  variant avoids putting a CivitAI key on the GPU box.

---

## Capability probe (connect-time detection)

Concrete, ordered probe from the mobile client against `ComfyUIConnection.baseUrl`:

1. **Plain ComfyUI present?** `GET /system_stats` (or `GET /object_info`) → 200 confirms a
   reachable ComfyUI. (Core ComfyUI endpoints.)
2. **Companion node (Option B) present?** `GET /civitdeck/ping` on our own node → 200 with a
   small version JSON. This is the authoritative probe for the receiver we control; define it
   in the node.
3. **ComfyUI-Manager present?** `GET /manager/version` → 200 returns Manager's version string
   (`core.version_str`, e.g. `"3.41"`); 404 = not installed.
   Source: https://github.com/Comfy-Org/ComfyUI-Manager/blob/main/glob/manager_server.py (`get_version`, ~line 1944).
   Useful only to detect Manager for *messaging* ("install our node"), NOT as an install path.

Client logic: (1) fails → not a ComfyUI box (guide Civitai Link / manual). (1) ok + (2) ok →
send via companion node. (1) ok + (2) missing → guided install of the companion node (link +
copy-paste git URL); (3) is informational only.

---

## ModelType → folder mapping

CivitDeck `ModelType` (`core/core-domain/.../domain/model/ModelType.kt`) vs ComfyUI default
folder names (verified in `folder_paths.py`). "Confirmed" = a ComfyUI default folder exists.

| CivitDeck ModelType | Target folder            | Status | Notes |
|---|---|---|---|
| Checkpoint          | `checkpoints`            | ✅ confirmed | ComfyUI default |
| TextualInversion    | `embeddings`             | ✅ confirmed | CivitAI "TextualInversion" = embedding |
| Hypernetwork        | `hypernetworks`          | ✅ confirmed | ComfyUI default (absent from Manager map) |
| LORA                | `loras`                  | ✅ confirmed | ComfyUI default |
| LoCon               | `loras`                  | ✅ confirmed | LoCon/LyCORIS load from `loras` |
| Controlnet          | `controlnet`             | ✅ confirmed | also `t2i_adapter` is part of this folder set |
| Upscaler            | `upscale_models`         | ✅ confirmed | ComfyUI default |
| VAE                 | `vae`                    | ✅ confirmed | ComfyUI default |
| MotionModule        | `motion_models`          | ⚠️ needs node | NOT a ComfyUI default — AnimateDiff-Evolved custom node dir. Only valid if that node is installed |
| AestheticGradient   | —                        | ⛔ no target | No ComfyUI concept; do not send |
| Poses               | —                        | ⛔ no target | Not a model; some workflows drop under `controlnet/poses` or a custom dir — treat as unsupported |
| Wildcards           | —                        | ⛔ no target | Text lists consumed by wildcard custom nodes, not a model folder |
| Workflows           | `user/default/workflows` | ⚠️ not a model | Workflow JSON, not a `models/` file — separate flow, out of scope for the receiver |
| Other               | —                        | ⛔ ambiguous | Require explicit user folder choice |

Resolution should be done **server-side via `folder_paths.get_folder_paths()`** (Civicomfy
pattern) with the mapping above as the type→key hint, so user-customized/extra roots are
honored. The client sends the ComfyUI folder key (e.g. `loras`), not an absolute path.

---

## Open risks / what a Dev-Ready issue must specify

1. **SHA256 verification is new code** (Civicomfy doesn't do it). Spec: compute over stream,
   compare to CivitAI `files[].hashes.SHA256`, delete + fail on mismatch, surface error to phone.
2. **Auth / abuse surface** — the node adds a write-capable HTTP endpoint on the GPU box. Spec a
   shared token (header) and bind guidance (LAN only / behind the same reverse proxy as ComfyUI).
   Consider refusing non-`.safetensors` by default.
3. **Distribution of the node** — not in ComfyUI-Manager's registry initially; spec the guided
   install UX (git URL + restart) and the `/civitdeck/ping` version handshake for compatibility.
4. **Unsupported types** — client must gate `AestheticGradient/Poses/Wildcards/Other` (and warn
   for `MotionModule`) before offering "send"; do not send types with no valid folder.
5. **Progress reporting** — decide poll (`GET /civitdeck/status`) vs ComfyUI websocket custom
   event. Civicomfy uses a background queue + status route; mirror that.
6. **CivitAI early-access / auth'd downloads** — some CDN URLs need a CivitAI token; decide
   whether the phone appends it to the URL or the node holds a key (prefer phone-side to keep
   the box keyless).
7. **Filename collisions / partial downloads** — spec `.part` temp + atomic rename, and
   overwrite policy.
8. **Manager fallback is out** — do not spec an A path; only `/manager/version` for messaging.

---

## Go / No-go

**GO** for a follow-up implementation issue — scoped to **Option B (companion custom node)
only**. The receiver design is de-risked: route registration, folder resolution, download,
and auto-refresh are all proven by Civicomfy + ComfyUI source; the only genuinely new work is
SHA256 verification and the auth token, both low-risk. **NO-GO** on any Option-A / ComfyUI-Manager
install path — verified unusable for arbitrary CivitAI models due to the curated whitelist gate.

---

## Codex cross-review (2026-07-18) — decision CONFIRMED, scope tightened

Codex validated the core architecture (Option B correct; no credible Manager-only path for
arbitrary models was missed) but flagged that this note **understates the companion node's
security and protocol work**. The follow-up implementation issue is a **conditional GO**: the
items below must be specified before it is Dev-Ready.

- **A is confirmed dead**, and the workarounds don't rescue it: generating a `model-list.json`
  turns CivitDeck into a live Manager catalogue (breaks for private/early-access/renamed files);
  patching Manager's cache is unsupported and update-fragile; weakening the security level is not
  an acceptable prerequisite. Manager's only role is **bootstrap** — publish our node in its
  registry to cut install friction. Pin the exact upstream commit (not "main"/approx lines).
- **SSRF is the headline risk** (not called out before). The node becomes a remote downloader +
  filesystem writer. Required: HTTPS-only + approved CivitAI/CDN host allowlist; **re-validate
  every redirect** against loopback/link-local/RFC1918/cloud-metadata (defend DNS rebinding);
  caps on size/concurrency/redirects/duration/disk.
- **Path safety**: allowlist folder keys + extensions; reject absolute paths / unknown-folder
  fallback; sanitize `filename` to a basename (reject traversal/separators, ignore remote
  `Content-Disposition`, defend symlinks); temp file in destination → hash → atomic rename;
  define overwrite/collision policy; never log signed URLs.
- **Auth is under-specified**: "shared token" is not a design. Spec token generation, pairing,
  storage, rotation, revocation; a bearer token over plaintext LAN HTTP is sniffable → require
  HTTPS on untrusted networks and document the plaintext-LAN risk.
- **Multi-root destination policy**: `folder_paths.get_folder_paths(key)` can return multiple
  roots (`extra_model_paths.yaml`). Blindly picking the first is surprising — the node must
  report a **deterministic writable default** (optionally user-configurable), not guess.
- **Refresh is best-effort, not infallible**: mtime cache invalidation can miss on network /
  coarse-timestamp filesystems. Make explicit `get_filename_list` warm-up + restart/reload the
  recovery path; don't promise auto-refresh.
- **Capability probe fixes**: reuse the endpoint the app already treats as authoritative —
  **`/queue`**, not `/system_stats` (`ComfyUIConnectionTesterImpl.kt:31`) — to avoid
  "connection works" vs "probe says not ComfyUI" disagreement. Don't equate every non-200 ping
  with "node absent": 404-after-core-ok = absent/proxy-routed; 401/403 = auth/proxy policy;
  TLS-fail = trust; timeout/5xx = transient; 200+HTML = captive/proxy page. `/civitdeck/ping`
  must return protocol version + node version + supported folder keys + hash algos + auth
  requirement (a capabilities handshake, not just a version).
- **Reverse-proxy gap in the current model**: `ComfyUIConnection` stores only scheme/host/port —
  no base-path prefix, no HTTP auth (`ComfyUIConnection.kt`). A proxy mounted at `/comfy/` or
  requiring Basic/Bearer will fail broadly (not just `/ping`). Either declare unsupported or add
  base-path + proxy-auth to scope. A proxy exposing core routes but omitting custom-node routes
  → report "receiver route unavailable", not "node missing".
- **Phone-resolves split is correct** (minimal data-plane node), but: signed CDN URLs may expire
  while queued → start promptly, refresh + retry on expiry-specific 401/403; **must resolve
  redirects phone-side to a time-limited CDN URL** — appending the raw CivitAI API key to the URL
  sent to the node would falsify the "keyless box" claim; ensure the resolved URL is usable from
  the GPU box (not bound to the phone's session/IP/headers); SHA256 proves integrity, NOT that
  pickle-format models are safe; the node must not trust phone-provided type/filename beyond its
  own allowlists.
- **"Only path" ≠ remove fallbacks**: keep manual phone download as the fallback; "only" means
  only automated server-side receiver.

Net: the "SHA + auth are the only new work, both low-risk" line above is too optimistic —
download security (SSRF/path/redirect), destination selection, queueing/cancellation, disk
handling, and credential pairing are all material work the Dev-Ready issue must scope.

### Sources
- ComfyUI-Manager `manager_server.py` (v3.41): https://github.com/Comfy-Org/ComfyUI-Manager/blob/main/glob/manager_server.py
- ComfyUI-Manager repo: https://github.com/Comfy-Org/ComfyUI-Manager
- Civicomfy repo: https://github.com/MoonGoblinDev/Civicomfy
  - `server/routes/DownloadModel.py`, `server/routes/__init__.py`, `server/routes/GetModelTypes.py`, `utils/helpers.py`, `downloader/manager.py`, `downloader/chunk_downloader.py`
- ComfyUI `folder_paths.py`: https://github.com/comfyanonymous/ComfyUI/blob/master/folder_paths.py
