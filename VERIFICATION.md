# Verification record

Build date: 24 October 2026.

## Passed

- All Android sources compile against the Android 36 platform with Java 17 bytecode using JDK 21.
- Android resources compile and link with build tools 36.0.0.
- D8 produces dex bytecode; APK alignment completes.
- `apksigner verify --verbose` verifies the signed APK using v3 signing.
- 400 seeded random workspace layouts, comprising 24,000 placements, pass bounds, collision and deterministic-order checks.
- Backup validator accepts a valid layout and rejects 10 malformed cases: unknown schema, invalid columns, invalid icon size, negative page, invalid width/height, unknown item type, duplicate identifiers, oversized dock and excessive item count.

Only compiler deprecation and Java boot-classpath warnings were emitted; no compile failures remain.

## Runtime status

An Android 16 x86_64 emulator was downloaded and started without hardware acceleration because `/dev/kvm` is unavailable. ADB transport connected. Android system services remained in startup and reported timeouts. The initial attempted package installation was refused with `device is still booting`; Dot Home had not been installed or launched by that attempt. That attempt is not a passing smoke test. Runtime verification remains incomplete unless a later successful result is appended below.

## Still required

- Real POCO X6 Pro installation, Home-role selection, navigation and lock/unlock behaviour.
- Android widget binding/configuration and provider-specific resize interaction.
- Calendar, media, camera, usage access and photo persistence on physical hardware.
- Backup/restore through the system document picker and signed APK upgrades.
- App launch/return, keyboard, landscape and clone/work-profile behaviour.
- Visual comparison against a frozen Nothing reference; performance, battery and accessibility review.

The deliverable is a compiled independent launcher build. Exact Nothing OS fidelity and daily-use reliability have not been established by these checks.

## Offline release 1.1.0 — 25 October 2026

Removed Internet permission, HTTP/geocoding/forecast code, weather choices, city-search UI and the Internet-settings shortcut. Migration removes legacy weather tiles from layouts, imported backups, recovery and undo. Cached weather preferences are deleted on startup.

Passed locally: Java/resource compilation, dex generation, alignment, signature verification, 400 random layouts, 10 malformed backups, migration/idempotence test, source networking check and compiled APK permission/version check. The release uses the prior signing identity. Physical-device runtime verification remains outstanding.

## UI rebuild 2.0.0 — 25 October 2026

Local checks pass for Java/resources/dex compilation, 400 legacy random layouts, 100 mixed explicit-cell placements, empty-space preservation, rejected-drop rollback, schema validation, offline migration and the no-network source/APK contract. The instrumentation APK also compiles.

The initial v2 source at `09a681e` passed GitHub Actions run [40159215833](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40159215833): build plus a hardware-accelerated Android 16 emulator UI run. Home, drawer, widget gallery, styles, customisation, overview, folder and navigation setup rendered, text assertions passed, screenshots were retained, and no AndroidRuntime fatal exception was found. Home/gallery/customisation screenshots were visually inspected. This supersedes the earlier unsuccessful local-emulator attempt for those flows.

The subsequent navigation update adds an emulator check that enables the Accessibility service only inside the disposable emulator, verifies three actual overlay windows and no window-content retrieval capability, injects an inward edge swipe and checks that the global Back action dismisses the setup. Final source CI must pass before publication; the release provenance identifies the exact passing source run.

Physical POCO X6 Pro / HyperOS 3 testing is still required for vendor navigation settings, hiding the three-button bar, service survival, Home/Recents gestures, provider-specific widgets, permission flows and battery/performance. Android emulator evidence does not establish those vendor behaviours or exact Nothing OS parity.

The navigation source at `6abed23` passed [40159737050](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40159737050), including live Accessibility windows, a real edge Back gesture, and the no-screen-content capability assertion. The final visual-reference revision also checks bottom Home and hold-for-Recents gestures in the disposable emulator. User-supplied reference images guide the visual design; thumbnail captions are not treated as verified OS release claims.

User review rejected the early v2 visual screenshots. Publication was paused; a subsequent revision reworks the seven-row composition, light/dark palette, dock symbols, clock treatment, translucent surfaces and wallpaper against the supplied references. Earlier screenshots are not visual acceptance evidence for this revision. The extended Home/Recents smoke check also exposed an unresolved bottom-gesture issue; no passing-navigation claim or release should be made until the final source run passes.


## 2.0.1 navigation and drawer correction — 25 October 2026

Removes the Accessibility navigation service, global edge windows and recovery tile. Normal Android Home-role, Back-callback and system Recents integration remain. The Home root recognizes vertical pulls before the workspace pager or clickable tile/dock children consume them, while preserving system navigation insets, horizontal paging and hosted-widget vertical interaction. Real touchscreen regression checks replace reflection-only app-drawer opening; they verify a visible settled panel, short/cancelled pulls, workspace/tile/dock origin and normal Android key-event navigation without granting an app Accessibility service. Local Java/resource/dex compilation, instrumentation compilation, model/migration/offline checks pass. Final GitHub Actions and physical-phone results are reported separately; this entry does not claim unrun device checks passed.
