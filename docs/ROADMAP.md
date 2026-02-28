# Roadmap

CivitDeck's development roadmap. Phases are sequential but individual items may ship out of order based on community demand.

Want to influence priorities? [Open an issue](https://github.com/rioX432/CivitDeck/issues/new/choose) or upvote existing ones.

---

## Positioning

CivitDeck is the only native iOS/Android client for CivitAI.
Focus: **Browse → Curate → Prepare** (not Generate).
Local-first design hedges against CivitAI platform risk.

---

## Phase 1 -- Core (Complete)

The foundation: browse, search, and save CivitAI models from your phone.

- [x] KMP project setup (Android + iOS + Shared)
- [x] CivitAI API client (Ktor)
- [x] Model search with filters (type, sort, period, tags)
- [x] Model detail screen with version info
- [x] Image gallery with staggered grid and full-screen viewer
- [x] Prompt metadata viewer with one-tap copy
- [x] Local favorites and offline cache (Room KMP)
- [x] iOS feature parity (SwiftUI)
- [x] Fresh Find -- discover recently published models
- [x] CI/CD pipeline (GitHub Actions)

## Phase 1.5 -- Quick Wins (Complete)

Quality-of-life improvements and account features.

- [x] CivitAI API key authentication ([#119](https://github.com/rioX432/CivitDeck/issues/119))
- [x] Tag-based exploration
- [x] Enhanced collection management ([#121](https://github.com/rioX432/CivitDeck/issues/121))
- [x] Creator profiles
- [ ] Offline browsing improvements ([#122](https://github.com/rioX432/CivitDeck/issues/122))

## Phase 2 -- Power User Expansion (Complete)

Advanced features for users who generate, not just browse.

- [x] Model version comparison -- side-by-side view ([#118](https://github.com/rioX432/CivitDeck/issues/118))
- [x] Power user mode -- toggleable advanced metadata panel
- [x] Prompt history and template system
- [x] Large screen and tablet support ([#125](https://github.com/rioX432/CivitDeck/issues/125))
- [ ] Push notifications for model updates ([#120](https://github.com/rioX432/CivitDeck/issues/120)) -- moved to Phase 5 backlog

## Phase 3 -- ComfyUI & Generation Integration (Complete)

The strategic core: bridge CivitAI browsing with your local generation setup.

- [x] One-tap workflow export to ComfyUI and A1111
- [x] Home screen widget -- trending model/image of the day ([#124](https://github.com/rioX432/CivitDeck/issues/124))
- [x] Model file browser with hash verification
- [x] SD WebUI (Automatic1111/Forge) API support ([#178](https://github.com/rioX432/CivitDeck/issues/178))
- [ ] Live generation preview
- [ ] Workflow template library

---

## Phase 4 -- ComfyUI Remote & Local Generation Management

Manage your local ComfyUI output from mobile. View generated images, inspect full metadata, and save results to collections.

- [ ] ComfyUI `/history` API integration -- retrieve generated image list and output files
- [ ] Remote output gallery browsing -- browse by timeline or workflow
- [ ] Generation metadata viewer -- prompt, seed, sampler, LoRA, CFG scale
- [ ] Secure remote connection UI -- Tailscale / local endpoint configuration (ports 8188 / 8000)
- [ ] Live generation status via WebSocket (`/ws`) -- real-time progress (optional)

**DoD**: `/history` image list retrieval → detail metadata display → save to favorites / collections

## Phase 5 -- Dataset Collection & Curation (CRITICAL)

Enable LoRA training dataset collection, organization, and management directly from mobile.

- [ ] `DatasetCollection` entity -- independent domain concept separate from Favorites ([new issue](https://github.com/rioX432/CivitDeck/issues))
- [ ] Image tagging system -- custom tags with batch editing
- [ ] Caption editor -- per-image and bulk caption editing
- [ ] License & source tracking -- CivitAI / Local / Generated provenance management
- [ ] Quality filtering -- resolution filter, duplicate detection, broken image exclusion UI
- [ ] Dataset manifest generation -- JSONL / CSV format export
- [ ] Generated image → dataset pipeline -- one-tap add from ComfyUI / SDWebUI output
- [ ] Creator follow and activity feed ([#120](https://github.com/rioX432/CivitDeck/issues/120)) -- nice to have

**DoD**: Group images → edit captions → export in training-ready format

## Phase 6 -- Training Pipeline Integration (Preparation Layer)

No on-device training -- but export datasets in formats compatible with kohya-ss / sd-scripts for seamless handoff to your training machine.

- [ ] Dataset export -- zip + per-image `caption.txt` files
- [ ] kohya-ss / sd-scripts compatible format -- `caption.txt` + `tags.txt`, correct archive path structure
- [ ] Auto caption placeholder -- empty field scaffolding for WD14 / BLIP annotation workflows
- [ ] ComfyUI output → dataset pipeline -- generated images added to dataset with one tap

**DoD**: Generated images directly added to dataset → one-click training zip generation

## Phase 7 -- AI-Assisted Curation (Claude/MCP Synergy)

Advanced curation using Claude API integration. Future phase.

- [ ] Claude-powered automatic tag generation -- image content analysis + tag suggestions
- [ ] Prompt clustering -- group similar prompts automatically
- [ ] Style grouping -- auto-classify by visual style
- [ ] Duplicate semantic detection -- hash + embedding-based deduplication
- [ ] Auto dataset suggestion -- derive training sets from collections automatically

---

## Monetization Philosophy

CivitDeck is and will remain **free and open source**.

- Core browsing and discovery features are **never paywalled**
- Future premium features (if any) would be limited to optional extras like cloud sync or advanced workflow management
- Development is sustained through GitHub Sponsors and community contributions
