---
description: Compose Desktop (JVM) conventions and patterns
globs: desktopApp/**/*.kt
---

# Desktop Rules

## Compose Desktop Conventions
- Entry point: `main()` function with `application { Window(...) }` block
- Use Material Design 3 components (same as Android Compose)
- Use `collectAsState()` instead of `collectAsStateWithLifecycle()` — no Android lifecycle on JVM
- ViewModels are plain classes with `CoroutineScope` — NOT `androidx.lifecycle.ViewModel`
- Desktop ViewModels live in `desktopApp/`, not in feature modules

## Navigation
- No Navigation 3 — JVM target does not support AndroidX Navigation
- Use state-based routing: sealed class for screens + `mutableStateOf` for current screen
- Back navigation via explicit state management

## Image Loading
- Coil 3.x works on JVM without `LocalContext.current`
- Do NOT pass Android `Context` to `ImageRequest.Builder` — it does not exist on Desktop
- `SubcomposeAsyncImage` is available for loading states

## Testing Commands
```bash
./gradlew :desktopApp:run             # Run the desktop app
./gradlew :desktopApp:packageDmg      # Package macOS .dmg
./gradlew :desktopApp:packageMsi      # Package Windows .msi
./gradlew :desktopApp:packageDeb      # Package Linux .deb
```

## File Dialogs
- Use `javax.swing.JFileChooser` for native file open/save dialogs
- Wrap in `SwingUtilities.invokeLater` or use `rememberCoroutineScope` + `Dispatchers.IO`

## Keyboard Shortcuts
- Use `Modifier.onKeyEvent` for component-level key handling
- Use `Window` `onKeyEvent` parameter for global shortcuts
- Common patterns: `Ctrl+F` for search, `Escape` to go back, arrow keys for navigation

## Platform Differences
- No `LocalContext.current` — any code depending on Android Context must use `expect/actual`
- No WorkManager — background tasks use plain coroutines
- No system notifications — use in-app notification UI
- No Glance widgets or Quick Settings tiles
