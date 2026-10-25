Dot Home 1.1.0 · Android version code 2

- Removed the Internet permission and all HTTP, weather, city-search and forecast code.
- Removed the weather option and Internet-settings shortcut. No in-app online option remains.
- Migrates saved layouts, imports, recovery and undo to remove legacy weather tiles.
- Preserves package name and signing identity for updates from the original build.

Built by [GitHub Actions](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40156683094) from commit 1465be27b78ce2c4a9b4b4b7455361d59f4ec649. The exact Actions APK was signed locally; the private signing key was not exported to GitHub. All ZIP entries were verified unchanged by signing.

Passed: 400 random layout tests, 10 malformed backup cases, offline migration/idempotence checks, source networking checks, APK permission/version checks, and signature verification. Physical POCO/HyperOS runtime testing remains outstanding.

Download dot-home-1.1.0.apk to install. The accompanying SHA-256 file verifies its download. Requires Android 15 or later.

The workflow automatically publishes future tagged signed releases once the repository signing secrets are configured. Until then, unsigned CI artifacts can be signed locally with scripts/sign_apk.sh.
