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
