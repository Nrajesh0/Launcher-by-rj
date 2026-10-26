# Rejected v3 preview: fidelity rebuild

The user rejected v3 visual and interaction quality on 25 October 2026. Functional smoke checks are not visual acceptance. v3 is not a design baseline for the replacement.

Primary reference: Nothing OS 5 general release on Phone (3). Sources rechecked online on 25 October:
- https://nothing.community/d/63548-nothing-os-50-general-release-is-here
- https://nothing.community/d/61730-nothing-os-50-new-features-explained
- https://nothing.tech/nothing-os
- https://support.nothing.tech/hc/en-us/articles/32975168713361-How-do-I-hide-app-icons-on-my-Nothing-Phone
- https://play.google.com/store/apps/details?id=com.nothing.launcher
- https://play.google.com/store/apps/details?id=com.nothing.hearthstone

The supplied photos remain composition references. Reference footage has been downloaded and inspected frame by frame, including batch selection/movement and Away Time variants. The public Theme & Style photograph is an observed beta screen, not proof of unchanged general-release labels.

## Failures to replace

| v3 substitution | Required correction |
| --- | --- |
| Large generic full-screen panels for item actions | Compact popup at the pressed item, direct resize/enlarge and clear hierarchy |
| A grid of giant Customisation buttons | Focused settings with smaller rows, previews, a theme selector, colour choices and independent dark mode |
| Arbitrary grayscale inside another disc | Proper monochrome icon layers/pack resources, consistent optical scale and no disabled-looking fallback |
| Smooth generic pastel/blue gradients | A deliberate bundled wallpaper library, matching optical texture, glass depth and both palettes |
| Rigid collision rejection | Mature workspace reorder, drag/folder/resize and transactional persistence |
| Two-column sample dashboard as widget library | Provider-organised widget tray, actual size/variant previews, direct placement and configuration |
| Hidden-app toggle list only | A dedicated hidden-icons drawer surface and sideways access gesture |
| Functional checks treated as quality proof | Compare actual app screens and recorded motion with references; keep unmatched details explicit |

## Foundation trial

Pinned Lawnchair 15 Beta 3 / Launcher3 source at `505dbc40e6154c05158b5d0271c45f6a885a411b` is under build/compatibility evaluation. Its existing workspace, drag/reorder, folder, shortcut and widget machinery is preferable to inventing these interactions again. This is a trial, not a claim that migration is complete. The raw upstream build contains Internet permission, web suggestions, online update/bug-report facilities and an Accessibility service. These must be removed from the shipped build, including user-facing choices; removing only the Internet permission is insufficient. The private signing identity stays local. Existing layout migration must retain a recoverable copy.

Exact Nothing source/binaries are not being substituted for an independently built APK. System-wide Nothing UI, hardware and firmware-controlled gestures remain outside a normal launcher. Product-owned appearance and interactions are the target of this rebuild.

## Replacement checkpoint

The default build path now prepares the pinned engine. Android 16 emulator run [40229451748](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40229451748) passed the baseline chooser, swipe-up drawer, hidden page, light/dark appearance, native widget binding, compact popup, batch move and Home/Recents interactions. Actual screenshots revealed and helped fix framework RemoteViews updates erasing local widget contents; counting database rows alone had missed that defect.

Subsequent release checks exercise the added per-instance style sheet and installation upgrade. Screenshots and filtered diagnostics are artifacts of each run. Physical POCO touch, high-refresh frame timing and comparison with a physical Nothing device remain unverified.

Implemented launcher-owned work includes native placement/reorder/resize/folders, local icon mappings and installed packs, compact settings and actual Home preview, Classic/Translucent/Dynamic with independent dark mode, original local wallpapers and crop editor, a provider tray, visible local data widgets, batch icon/widget selection with move/folder/remove and group dragging, folder covers, local backup/restore and legacy JSON import. A successful emulator check does not establish pixel-perfect Nothing parity.

Remaining differences include the precise proprietary icon/folder artwork, exact motion curves, platform-specific integrations and some widget families. Translucent widgets sample and blur our own saved wallpaper; they do not capture other apps. Backups store document references and widget placement, not provider internals or transferable grants. Controls use public system settings where direct toggles are restricted.

System lock screen, Quick Settings, status-bar design, Glyph hardware, Essential AI/cloud features and firmware-owned global navigation are not launcher-owned surfaces. Offline weather must use manual/local information or an independently installed provider; fresh weather cannot be fetched by this app.
