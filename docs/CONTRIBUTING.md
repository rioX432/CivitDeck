# Contributing to CivitDeck

Thank you for your interest in contributing to CivitDeck! This guide will help you get started.

## Getting Started

### Prerequisites

- Android Studio Ladybug or later
- Xcode 15+ (for iOS development)
- JDK 17+
- Git

### Setup

```bash
git clone https://github.com/rioX432/CivitDeck.git
cd CivitDeck
```

### Build

```bash
# Android
./gradlew :androidApp:installDebug

# iOS
open iosApp/iosApp.xcodeproj
# Build & run from Xcode (select iOS Simulator target)
```

### Run Tests

```bash
# Shared module unit tests
./gradlew :shared:testDebugUnitTest
```

## Development Workflow

### Branch Naming

Create branches from `master` using these prefixes:

- `feature/` — New features (e.g., `feature/creator-profiles`)
- `fix/` — Bug fixes (e.g., `fix/image-loading-crash`)
- `docs/` — Documentation changes (e.g., `docs/update-readme`)

### Commit Messages

- Write in English
- Keep to one line
- Be concise and descriptive

```
# Good
Add model type filter chips to search screen
Fix image gallery crash on rotation
Update Ktor to 3.1.3

# Bad
WIP
fix stuff
Updated files
```

### Code Style

This project uses [detekt](https://detekt.dev/) for static analysis and formatting. Always run detekt before committing:

```bash
./gradlew detekt
```

Auto-correct is enabled, so most formatting issues will be fixed automatically.

### Pull Requests

1. Create a branch from `master`
2. Make your changes
3. Run `./gradlew detekt` and fix any issues
4. Run `./gradlew :shared:testDebugUnitTest` to ensure tests pass
5. Push your branch and open a PR against `master`
6. Fill in the PR template

PRs should:
- Have a clear title describing the change
- Reference related issues (e.g., `Closes #42`)
- Include screenshots for UI changes
- Pass CI checks

## Project Guidelines

### Architecture

- All shared logic goes in `shared/commonMain/` — platform-specific code only when necessary
- DTOs (`data/api/`) are separate from domain entities (`domain/model/`)
- Use cases are single-responsibility (one public function, returns `Flow`)
- ViewModels live in platform modules, not in shared

See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

### Language

All written content must be in **English**, including:
- Code comments
- Commit messages
- PR titles and descriptions
- Documentation

## Finding Issues to Work On

- Look for issues labeled [`good first issue`](https://github.com/rioX432/CivitDeck/labels/good%20first%20issue) — these are beginner-friendly
- Check the [Roadmap](README.md#roadmap) for upcoming features
- Feel free to open a new issue if you have an idea

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Questions?

Open an issue or start a discussion — we're happy to help!
