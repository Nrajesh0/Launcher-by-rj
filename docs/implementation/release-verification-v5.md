# Dot Home 5.0.0 verification

Implementation source: `d3b5f27e77bdae86b3e435ee506004d45ab412e3`. Full Android 16 build/UI/upgrade workflow: [run 40563188991](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40563188991). Version 5.0.0, code 7; package `dev.dothome`; Home component `dev.dothome.HomeActivity`. APK SHA-256: `79efbaf9dd9471558676414da25ac4a2aa079e030fb9da558099a46d36f67e85`.

The 74-item tracker preserves the original v4 findings. Its implementation and verification columns describe v5 separately. Coverage is sampled: passing instrumentation does not establish every widget/span/profile, exact Nothing firmware parity or physical POCO performance.

| Check | Observed evidence |
| --- | --- |
| Build/model/privacy | Complete Release build, JVM placement/rollback/state tests, packaged manifest/DEX gates |
| Home role and navigation | Actual Android Home chooser, native swipe-up drawer and system Home/Recents; no Accessibility service or navigation overlay |
| Appearance | Classic/Lucent light/dark, unchanged native placements, populated multi-page previews, settings route reuse; actual owned-wallpaper palette selection, manual accent preservation and Noir system-icon contrast |
| Organisation | Native arrangement/group movement/folders, category cards and correction; hidden-page entry/cancel/Back; new-page preset and undo |
| Widget state | Note Done and weather draft persistence; owned image import/cutout/cycle; local wallpaper effects and nonempty crop preview |
| External widget host | Standard installed-app picker, actual bind/configure, cancel/remove and binding cleanup; cancellation removes the provisional native item |
| Search | Drawer re-entry after search/settings, calculator on/off; optional contacts denial leaves its scope disabled; selected document name appears in actual drawer search, unselected synthetic file stays absent and index removal removes its result |
| Large text/access states | 1.3x font capture, denied usage-history screen and owned RTL layout; not a full locale/TalkBack matrix |
| Breathing/steps | JVM phase/pause/clock-change and first-use/day/reboot/reset transitions; hardware cadence/accuracy not inferred |
| Migration/update | Synthetic v3 import; actual released v4 APK updated to v5 with native item IDs/positions, folder/widget state, appearance and Home role retained |
| Release signing | Original public certificate fingerprint verified locally; signed ZIP payload compared byte-for-byte with the unsigned successful CI artifact |

The upgrade fixture uses a disposable CI signing identity for both old/new APKs. The separately signed public APK retains the original release certificate. The test device is an Android 16 emulator with synthetic data; no private emulator state or user files are published. Full captures and filtered diagnostics are attached to the linked run. The [review sheet](../reference/v5-actual-review.jpg) contains actual launcher captures, not generated mockups.

Current constraints: sampled blur needs readable app-owned wallpaper pixels; inaccessible external/live wallpaper uses a tint fallback. Weather is manually entered and timestamped. Document search indexes selected names only, not file contents or storage. External widget/document grants may need reconnecting after restore. Steps exclude unmeasured first-use/reboot gaps. Audio widgets do not infer per-ear/case battery. Cutout removes an edge-connected plain background, not semantic subjects. Wallpaper motion is visible-only, not a firmware unlock effect. Backup uses a native database transaction, settings error rollback and staged media; no power-loss atomicity across independent Android services is claimed.

Physical POCO 60/120Hz timing, OEM work/private/cloned profiles, full RTL locales/TalkBack, predictive-Back gesture/IME/rotation matrix and the user's final visual approval remain pending. See [device validation](device-validation.md). Global navigation/Recents, lock screen/AOD/Quick Settings/status bar, Glyph hardware and Nothing cloud services remain OS/hardware/offline boundaries.

Publication checks are recorded separately: original certificate and public checksum, stable/latest GitHub release, versioned APK/checksum assets, and main/tag/publisher Actions must all pass before release completion is reported.

## Completed publication checks — 2026-10-28

- [Stable/latest v5.0.0](https://github.com/Nrajesh0/Launcher-by-rj/releases/tag/v5.0.0): draft false, prerelease false, versioned APK and checksum present. Version code 7.
- [Publisher](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40623345030): success; independently verified source CI, original signature, checksum and original CI payload before publishing.
- [Post-push Android build, UI and v3/v4 upgrades](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40623345004): success for release commit `0d2c0dea4b70d6784025e09fdacd0dfc1516daf0`.
- Public APK downloaded and compared with the separately downloaded checksum: 34,946,740 bytes, SHA-256 `79efbaf9dd9471558676414da25ac4a2aa079e030fb9da558099a46d36f67e85`. Original certificate, package/code, POCO arm64 support, privacy contract and unchanged successful CI payload reverified on that public download.
- Release tag targets implementation source `d3b5f27e77bdae86b3e435ee506004d45ab412e3`; the release commit changes only documentation and public release assets.

This completion record is a documentation-only follow-up. It does not alter the tested or published APK. Device/user limits above remain explicit.
