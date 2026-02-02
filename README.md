<div align="center">

# CivitDeck

**Browse CivitAI models, images, and prompts natively on Android & iOS**

Built with Kotlin Multiplatform (KMP)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-6366F1?style=flat-square)]()
[![CI](https://github.com/rioX432/CivitDeck/actions/workflows/ci.yml/badge.svg)](https://github.com/rioX432/CivitDeck/actions/workflows/ci.yml)

[English](README.md) | [日本語](README.ja.md)

</div>

---

## Why CivitDeck?

[CivitAI](https://civitai.com/) is the largest open-source generative AI community — hosting hundreds of thousands of models, LoRAs, and AI-generated images. But there's **no official mobile app**.

CivitDeck fills that gap. Browse models, explore images, read prompts, and save your favorites — all from your phone.

## Screenshots

<!-- TODO: Add actual screenshots -->

| Android | iOS |
|---------|-----|
| ![Android Search](docs/screenshots/android_search.png) | ![iOS Search](docs/screenshots/ios_search.png) |
| ![Android Detail](docs/screenshots/android_detail.png) | ![iOS Detail](docs/screenshots/ios_detail.png) |
| ![Android Gallery](docs/screenshots/android_gallery.png) | ![iOS Gallery](docs/screenshots/ios_gallery.png) |

## Features

- **Model Search & Browse** — Filter by type (Checkpoint, LoRA, etc.), sort, period, and tags
- **Image Gallery** — Staggered grid with full-screen viewer and pinch-to-zoom
- **Prompt Metadata** — View generation parameters (prompt, model, sampler, seed) and copy with one tap
- **Favorites** — Save models and images locally for offline access
- **Cross-Platform** — Native Android (Jetpack Compose) & iOS (SwiftUI) from a shared KMP codebase

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Shared (KMP)** | Ktor Client, Kotlinx Serialization, Room KMP, Koin |
| **Android** | Jetpack Compose, Material Design 3, Navigation 3, Coil |
| **iOS** | SwiftUI |
| **Architecture** | Clean Architecture + MVVM (UDF) |
| **CI/CD** | GitHub Actions |

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed documentation.

```
┌──────────────────────────────────────────┐
│              Shared (KMP)                │
│                                          │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐  │
│  │   Ktor   │ │Repository│ │ Room KMP │  │
│  │  Client  │ │          │ │ (Cache)  │  │
│  └────┬─────┘ └────┬─────┘ └────┬────┘  │
│       └──────┬─────┘            │        │
│         ┌────▼─────┐     ┌─────▼─────┐  │
│         │ Use Case │     │  Entity   │  │
│         └────┬─────┘     └───────────┘  │
├──────────────┼───────────────────────────┤
│   Android    │          iOS              │
│   Compose    │        SwiftUI            │
│   ViewModel  │       ViewModel           │
└──────────────┴───────────────────────────┘
```

## Getting Started

### Prerequisites

- Android Studio Ladybug or later
- Xcode 15+ (for iOS)
- JDK 17+

### Build & Run

```bash
# Clone
git clone https://github.com/rioX432/CivitDeck.git
cd CivitDeck

# Android
./gradlew :androidApp:installDebug

# iOS
open iosApp/iosApp.xcodeproj
```

## Roadmap

### Phase 1 — MVP
- [x] Project setup (KMP + Android + iOS)
- [x] CivitAI API client (Ktor)
- [x] Model search & browse (Android)
- [x] Model detail screen (Android)
- [x] Local favorites & offline cache (Room KMP)
- [x] Image gallery & metadata viewer (Android)
- [x] Documentation (README, ARCHITECTURE, CONTRIBUTING)

### Phase 2 — iOS & Polish
- [x] iOS model search screen (SwiftUI)
- [x] iOS model detail screen (SwiftUI)
- [x] iOS image gallery screen (SwiftUI)
- [ ] CivitAI account integration (API Key auth)
- [ ] Creator profiles
- [ ] Tag-based exploration
- [ ] Global launch & marketing

### Phase 3 — Advanced
- [ ] Image comparison tool
- [ ] Prompt template management
- [ ] ComfyUI / SD WebUI integration
- [ ] Home screen widget (Popular models)

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

If you find this project useful, please consider giving it a **star** — it helps others discover CivitDeck.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Author

**RIO** ([@rioX432](https://github.com/rioX432))

Mobile App Developer based in Tokyo — Android | iOS | KMP
