# Changelog

## 1.1.0 — 2026-10-25

- Remove Internet permission and all HTTP, weather, forecast and city-search code.
- Remove the built-in weather option and Internet-settings shortcut.
- Migrate existing layouts, imports, recovery and undo to remove legacy weather tiles.
- Delete cached weather preferences during upgrade.
- Centralise APK/About versioning in version.properties (version code 2).
- Add GitHub Actions compilation, model tests, offline-contract checks and versioned artifacts.
- Add tagged release publication when signing secrets are configured, with an optional local-signing path that keeps the private key off GitHub.
- Preserve the existing package and signing identity for upgrade compatibility.

Physical POCO/HyperOS runtime testing remains outstanding.
