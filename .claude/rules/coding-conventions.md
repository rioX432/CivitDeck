# Coding Conventions

## General
- Code comments in English
- Commit messages in English, concise single line
- Name repeated domain thresholds and configuration values; keep obvious local literals close to their use
- Error messages: user-facing in target language, logs in English

## Testing

| Layer | Tool | Coverage Target |
|---|---|---|
| <!-- fill per project --> | | |

- Test externally observable behavior in proportion to regression risk; do not require one unit test per public
  function when an integration, property, contract, or compile-time check is the better proof
- Use Arrange/Act/Assert when it clarifies the test, not as mandatory ceremony
- Test names describe behavior, not implementation
