# Localization Conventions

## String Management
- All user-visible strings in resource files, never hardcoded
  - Android: `strings.xml` (per locale: `values-ja/`, `values-en/`, etc.)
  - iOS: `Localizable.strings` or String Catalogs
  - KMP/CMP: shared resources via `composeResources` or `moko-resources`
- String keys: `snake_case`, descriptive (`login_button_submit`, not `btn1`)
- Parameterized strings: use positional format (`%1$s`) not concatenation
- Plurals: use `plurals.xml` (Android) / `stringsdict` (iOS), never `if count == 1`

## Content Rules
- Default language: English (base strings)
- No raw text in Compose/SwiftUI — always reference resource keys
- Date/time: use platform formatters (`DateTimeFormatter` / `DateFormatter`), never manual format strings
- Numbers/currency: use `NumberFormat` with locale, never manual formatting
- Do not assume text direction — support RTL where applicable

## Quality Checks
- Missing translations: all keys must exist in every supported locale
- String length: allow 40% expansion for translated text (German/French expand significantly)
- Truncation: all text must handle overflow with `maxLines` + `ellipsis` or auto-sizing
- Screenshots: verify UI doesn't break with longest translation

## CMP-Specific
- Shared string resources in `commonMain/composeResources/`
- Platform-specific strings (e.g., iOS permission dialogs) in platform source sets
- Use `stringResource()` in Compose, not direct string access
