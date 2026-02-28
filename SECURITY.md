# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | ✅        |
| Older   | ❌        |

CivitDeck is in active development. Only the latest release receives security fixes.

## Scope

This policy covers the CivitDeck Android and iOS applications.

**In scope:**
- Local database (Room KMP) — data leakage, unauthorized access to stored models/prompts/collections
- API key storage — insecure storage of CivitAI API credentials
- Network requests — MITM, insecure TLS configuration
- ComfyUI connection — insecure local server communication
- Input handling — injection vulnerabilities in search/filter inputs

**Out of scope:**
- CivitAI's own servers or API (report those to Civitai Inc. directly)
- ComfyUI's own server software (report upstream)
- Issues in third-party libraries (report upstream)

## Reporting a Vulnerability

Please **do not** open a public GitHub Issue for security vulnerabilities.

Report via [GitHub Security Advisories](https://github.com/rioX432/CivitDeck/security/advisories/new):

1. Go to the **Security** tab → **Advisories** → **Report a vulnerability**
2. Describe the vulnerability, steps to reproduce, and potential impact
3. We aim to acknowledge reports within 7 days

## What to Expect

- Acknowledgement within 7 days
- Regular updates on the investigation and fix timeline
- Credit in [CHANGELOG.md](CHANGELOG.md) when the fix is released (unless you prefer to stay anonymous)
