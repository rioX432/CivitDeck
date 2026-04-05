## Description
<!-- What changed and why -->

## Related Issue
<!-- #XX or N/A -->

## Screenshots / Video
<!-- Required for UI changes. Before/After on both platforms if applicable -->
| Before | After |
|--------|-------|
|        |       |

## Manual Test Checklist
- [ ] Android emulator — happy path
- [ ] Android emulator — edge cases (empty, error, offline)
- [ ] iOS simulator — happy path
- [ ] iOS simulator — edge cases (empty, error, offline)
- [ ] Dark mode verified (both platforms)
- [ ] Small screen (iPhone SE / 5") and large screen (Max / tablet)
- [ ] TalkBack navigation (Android)
- [ ] VoiceOver navigation (iOS)

## Automated Tests
- [ ] Unit tests added/updated
- [ ] Roborazzi screenshot tests pass (`./gradlew verifyRoborazziDebug`)
- [ ] Maestro smoke tests pass (if applicable)

## Checklist
- [ ] CLAUDE.md rules followed
- [ ] No hardcoded strings (all externalized)
- [ ] No `!!` or force unwrap
- [ ] No `GlobalScope`
- [ ] Error handling complete
- [ ] New strings added to shared resources
