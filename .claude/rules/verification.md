# Verification Profiles

Choose verification breadth from what the change can break, not from habit. A one-line change to token validation
can break every login; a hundred lines of documentation cannot break a build. The goal is blast-radius alignment:
high-risk changes get more proof than today, low-risk changes stop paying for proof they do not need.

## Precedence

1. **The issue's `Done when` commands** always run, exactly as written, in every profile.
2. **Repository guidance** (`AGENTS.md`, host-specific override, CI config) decides which commands exist, what each
   covers, and any check it requires for every change. It overrides every default below.
3. **The profile defaults** in this file fill in only what the first two leave open.

Never invent a command. If repository guidance names no narrower command for a tier, use the smallest documented
command that covers the changed surface and record that it was a broader fallback.

## 1. Classify the change

List every signal the change carries, judged from its semantics and the boundaries it touches. The profile is the
highest one any signal maps to. Diff size never lowers a profile.

| Signal | Profile |
|---|---|
| `docs-only`, `comments-only`, `test-only`, `refactor-no-behavior-change`, `config-text` | `fast` |
| `behavior-change`, `new-feature`, `crosses-module-boundary`, `dependency-minor` | `standard` |
| `public-contract`, `data-migration`, `security`, `auth`, `concurrency`, `build-or-ci-config`, `dependency-major`, `cross-repository`, `irreversible` | `highRisk` |

`crosses-module-boundary` means the change alters how two modules interact, not merely that two files in different
modules were edited. A change to what users see or can interact with — layout, sizing, styles, visible copy — is
`behavior-change`, not `config-text` or `refactor-no-behavior-change`. A change you cannot classify with confidence takes the higher candidate profile.

## 2. Tiers

| Tier | Covers |
|---|---|
| `focused` | Tests and checks for the changed units, plus the issue's `Done when` commands |
| `affected-module` | Build, test, and lint of each module that contains a changed file |
| `integration` | Contract and cross-module tests; for a changed contract, both a producer and a consumer |
| `full` | The repository-wide build, test, and lint |

A passing broader tier satisfies a narrower one, except that `Done when` commands always run as written.

## 3. Required tiers per profile

<!-- verification-profiles:start -->
| Profile | Required tiers | May be delegated to CI |
|---|---|---|
| `fast` | focused | — |
| `standard` | focused, affected-module (+ integration with `crosses-module-boundary`) | integration |
| `highRisk` | focused, affected-module, integration, full | full |
<!-- verification-profiles:end -->

- **`fast` does not run the full suite locally.** Running it anyway is over-verification unless repository guidance
  or the issue requires it; record that requirement when it applies. CI may still run everything on the PR.
- **`highRisk` never passes on focused checks alone.** Every tier must pass, locally or through CI delegation.

## 4. Delegating a tier to CI

A tier counts as delegated only when all of these hold:

1. Repository CI runs a check that covers that tier on the pull request's head commit.
2. That check is required for merge (`gh pr checks --required` lists it), so nothing merges before it passes.
3. The verification record names the check, marks the tier `source: ci`, and sets `required: true` only after
   seeing the check in `gh pr checks --required`.
4. The tier is satisfied only once that check reports success on the same head SHA as the rest of the evidence.
   Pending, skipped, or absent CI satisfies nothing; do not call missing CI a pass.

When those conditions do not hold, run the tier locally. Only the tiers listed as delegable above may be delegated.

## 5. Re-verification after a fix

A fix invalidates only the surface it touched:

- Re-run the `focused` checks for the files the fix changed and the `affected-module` tier for their modules.
- Re-run `integration` when the fix touched a contract or a cross-module path; re-run `full` only when the profile
  requires it and the fix could have changed its result.
- If the fix adds a signal (for example it now touches a public contract), re-classify and run every tier the new
  profile adds.
- Do not re-run suites whose surface the fix did not touch. A review-comment fix to one file does not re-run the
  whole gate.

The final evidence must still bind every required tier to the final head commit.

## 6. Evidence reuse

Before running a local check, look for passing evidence of the same check on the same state. Reuse it only when its
key matches exactly:

| Key field | Taken from | Invalidated by |
|---|---|---|
| `head_sha` | `git rev-parse HEAD` | a new commit, unless that commit is exactly the tree the evidence ran on |
| `tier`, `command` | the check | any difference in the command text |
| `surface`, `surface_fingerprint` | the paths the command's result depends on, and their content in the working tree | a wider or different surface; any content change under it, committed or not |
| `config`, `config_fingerprint` | lockfiles, build/test configuration, and gitignored inputs (such as `.env`) the command reads, hashed from disk | any change to those files, including creating or deleting one |
| `toolchain` | the versions of the runtimes and tools that run it (for example `node --version`) | any version change |

- A surface lists every path the command reads that this change set can alter, including shared code it imports.
  Use `.` (the whole repository) for `full`, and whenever you cannot bound it; a `.` surface is invalidated by any
  change to tracked or untracked files. The surface fingerprint cannot see gitignored files, so list any gitignored
  input the command reads under `config`.
- Within one HEAD, an edit outside a surface leaves that surface's evidence valid, which is what lets a review fix
  re-run only its own surface (section 5). Committing the verified working tree unchanged keeps the evidence; any
  other new HEAD invalidates it. Evidence is never carried to a commit with different content.
- Only passing local evidence is reusable. CI results are always read from CI status (section 4), never from a local
  store; a failed run is never reused.
- Keep the store per working copy (for example `workspace/{issue}/evidence.json`), never committed.

`python3 scripts/verification-gate.py key --tier <tier> --command "<cmd>" --surface <paths> --config <files>
--toolchain "<versions>"` computes the key from the repository, and `python3 scripts/verification-gate.py reuse
<evidence.json> <key.json>` returns `reuse` or `run` with the reason. Record both in the check.

## 7. Verification record

Report verification as a structured record so a caller can check it without trusting narration:

```json
{
  "profile": "fast | standard | highRisk",
  "signals": ["behavior-change"],
  "head_sha": "{git rev-parse HEAD}",
  "done_when": ["{exact Done when command}"],
  "checks": [
    {
      "tier": "focused | affected-module | integration | full",
      "command": "{exact command, or the CI check name}",
      "surface": "{files, module, or repository}",
      "source": "local | ci",
      "required": "{ci only: true when gh pr checks --required lists this check}",
      "exit_code": 0,
      "success_signal": "{exact observed signal}",
      "output_excerpt": "{bounded excerpt containing the signal}",
      "head_sha": "{commit the check ran against}",
      "required_by": "{optional: repository | issue, when a check exceeds the profile on purpose}",
      "evidence_key": "{local checks: the key from section 6}",
      "reuse": {"decision": "ran | reused", "reason": "{reuse reason, or why stored evidence was invalidated}"}
    }
  ]
}
```

When the provider's `scripts/verification-gate.py` is available, `python3 scripts/verification-gate.py check
<record.json>` applies sections 1–4 deterministically and counts executed and reused commands separately: it
rejects a missing required tier, a failed or stale check, an unjustified local `full` run under `fast`, a CI check
not marked required, and a delegation outside the allowed tiers. Without it, apply the same rules by hand.
