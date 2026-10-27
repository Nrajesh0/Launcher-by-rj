# Dot Home 5.0.0 verification

Implementation source: `d3b5f27e77bdae86b3e435ee506004d45ab412e3`. Full Android 16 build/UI/upgrade workflow: [run 40563188991](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40563188991). Version 5.0.0, code 7; package `dev.dothome`; Home component `dev.dothome.HomeActivity`. Signed APK SHA-256: `79efbaf9dd9471558676414da25ac4a2aa079e030fb9da558099a46d36f67e85`.

The 74-item tracker preserves the original v4 findings. Its implementation and verification columns describe v5 separately. Passing instrumentation does not establish every widget/span/profile, exact Nothing firmware parity or physical POCO performance.

| Check | Observed evidence |
| --- | --- |
| Build/model/privacy | Complete Release build, placement/rollback/state tests and packaged manifest/DEX gates |
| Home and navigation | Real default-Home chooser, native swipe-up drawer, system Home/Recents; no Accessibility service or navigation overlay |
| Appearance | Classic/Lucent light/dark; unchanged native placements; populated page-aware previews; owned-wallpaper palette, manual accent preservation and Noir icon contrast |
| Organisation | Arrangement/group movement/folders, category corrections, hidden-page entry/cancel/Back, new-page preset and undo |
| Widgets | Note Done/weather drafts, owned image import/cutout/cycle, local wallpaper effects and crop preview |
| External widgets | Installed-app picker, actual bind/configure/cancel/remove and provisional-item/binding cleanup |
| Search | Drawer re-entry; calculator on/off; contacts denial leaves scope disabled; selected document name appears in drawer, unselected file stays absent, removal removes result |
| Access states | 1.3x font capture, denied usage screen and owned RTL layout; not a full locale/TalkBack matrix |
| Breathing/steps | Phase/pause/clock-change and first-use/day/reboot/reset model tests |
| Migration | v3 import and actual released v4 APK upgrade preserving native IDs/placements/folders/widgets/appearance/Home role |
| Public signing | Original certificate verified locally; every unsigned ZIP entry compared byte-for-byte with successful CI artifact |

The disposable CI upgrade fixture uses a CI signing identity for both old and new APKs. The public APK is separately signed with the original release key. All captured emulator data are synthetic. No private signing key or emulator state is published.

Constraints: inaccessible external/live wallpaper pixels use tint fallback. Weather is manually entered. Document search indexes explicitly selected names only. External grants/widgets may need reconnecting after restore. Steps exclude unmeasured gaps. Audio widgets do not infer per-ear/case battery. Cutout removes plain edge-connected backgrounds. Wallpaper motion is visible-only. Backup uses a native database transaction, settings rollback and staged media; cross-service power-loss atomicity is not claimed.

Physical POCO 60/120Hz traces, OEM work/private/cloned profiles, full locale/TalkBack and predictive-Back/IME/rotation matrices, and final user visual approval remain pending. Global navigation/Recents, lock screen/AOD/Quick Settings/status bar, Glyph and cloud services remain OS/hardware/offline boundaries.

Publication is complete only after publisher and post-push Actions pass and the stable/latest public APK checksum matches the signed artifact.
