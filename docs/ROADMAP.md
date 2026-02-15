# Roadmap

CivitDeck's development roadmap. Phases are sequential but individual items may ship out of order based on community demand.

Want to influence priorities? [Open an issue](https://github.com/rioX432/CivitDeck/issues/new/choose) or upvote existing ones.

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

## Phase 1.5 -- Quick Wins

Quality-of-life improvements and account features.

- [ ] CivitAI API key authentication ([#119](https://github.com/rioX432/CivitDeck/issues/119))
- [ ] Tag-based exploration
- [ ] Enhanced collection management ([#121](https://github.com/rioX432/CivitDeck/issues/121))
- [ ] Creator profiles
- [ ] Offline browsing improvements ([#122](https://github.com/rioX432/CivitDeck/issues/122))

## Phase 2 -- Power User Expansion

Advanced features for users who generate, not just browse.

- [ ] Model version comparison -- side-by-side view ([#118](https://github.com/rioX432/CivitDeck/issues/118))
- [ ] Power user mode -- toggleable advanced metadata panel
- [ ] Prompt history and template system
- [ ] Push notifications for model updates ([#120](https://github.com/rioX432/CivitDeck/issues/120))
- [ ] Large screen and tablet support ([#125](https://github.com/rioX432/CivitDeck/issues/125))

## Phase 3 -- ComfyUI & Generation Integration

The strategic core: bridge CivitAI browsing with your local generation setup.

- [ ] ComfyUI remote API integration -- trigger workflows from mobile
- [ ] One-tap workflow export to ComfyUI and A1111
- [ ] Live generation preview
- [ ] Workflow template library
- [ ] SD WebUI API support

## Phase 4 -- Future

Ideas on the horizon. Subject to community feedback.

- [ ] Home screen widget -- trending model/image of the day ([#124](https://github.com/rioX432/CivitDeck/issues/124))
- [ ] Model file browser with hash verification
- [ ] Creator follow and activity feed ([#123](https://github.com/rioX432/CivitDeck/issues/123))
- [ ] Model download manager
- [ ] Community features (reviews, ratings)
- [ ] Plugin system

---

## Monetization Philosophy

CivitDeck is and will remain **free and open source**.

- Core browsing and discovery features are **never paywalled**
- Future premium features (if any) would be limited to optional extras like cloud sync or advanced workflow management
- Development is sustained through GitHub Sponsors and community contributions
