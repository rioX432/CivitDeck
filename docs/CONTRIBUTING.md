# Contributing to CivitDeck

Thank you for your interest in contributing to CivitDeck! This guide will help you get started.

## Getting Started

Follow the [Development Guide](DEVELOPMENT.md) for the current toolchain, setup, Android flavors, Desktop and iOS commands, and CI-equivalent checks.

Choose a small change and run the checks for the modules and platforms it affects. The guide also explains which source set to use for shared logic and tests.

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
4. Run the applicable [tests and app builds](DEVELOPMENT.md#checks-before-a-pull-request), and report any checks you could not run
5. Push your branch and open a PR against `master`
6. Fill in the PR template

PRs should:
- Have a clear title describing the change
- Reference related issues (e.g., `Closes #42`)
- Include screenshots for UI changes
- Pass CI checks

## Project Guidelines

### Architecture

- Keep domain contracts, network DTOs, and repository implementations in their respective `core/` modules
- Put shared feature ViewModels and use cases in `feature/feature-*/src/commonMain/`
- Use `shared/src/commonMain/` for cross-module wiring and shared integration code
- Use platform source sets only for platform-specific APIs and implementations
- Keep use cases single-responsibility and follow the surrounding module's patterns

See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

### Language

All written content must be in **English**, including:
- Code comments
- Commit messages
- PR titles and descriptions
- Documentation

## Finding Issues to Work On

- Look for issues labeled [`good first issue`](https://github.com/rioX432/CivitDeck/labels/good%20first%20issue) — these are beginner-friendly
- Check the [Roadmap](ROADMAP.md) for upcoming features
- Feel free to open a new issue if you have an idea

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Questions?

Open an issue or start a discussion — we're happy to help!
