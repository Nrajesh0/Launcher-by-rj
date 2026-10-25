Dot Home 2.0.0 is an early preview of the rebuilt offline launcher for the POCO X6 Pro.

- Requests Android's default Home selection on first launch. If dismissed, use the setup page to select Dot Home.
- Light and dark themes in Customisation → Appearance, with a revised home layout, circular dock icons and local wallpaper presets.
- Reworked widget gallery, clock faces, battery styles, focus animation, timer, local photos and home editing.
- Optional accessibility gestures for Back, Home and Recents. Enabling the service is a separate action in Android Settings.
- No Internet permission or online mode. Signed with the same certificate as version 1.1.0.

Validation: GitHub Actions compilation, model/migration tests, offline checks, APK checksum and signing verification passed. The full automated UI suite has NOT passed: it stopped while selecting the launcher in Android's default Home dialog. Light/dark switching and gesture navigation still need testing on the physical POCO. Treat this as a preview with possible UI and interaction bugs, not a finished Nothing OS replacement.

HyperOS controls its system navigation bar, lock screen, notification shade, Quick Settings and native Recents transitions. Installing this APK cannot replace those system components. ADB instructions for the optional HyperOS navigation setting are in docs/adb-navigation.md; the phone-specific result is unverified.

Download dot-home-2.0.0.apk below. Install over the previous release to preserve your layout. Optional permissions are requested when their feature is used; Internet access is never requested.

Source: 48e024e92e67d695c7c530f63ccc80097f4f2dbd
Build: https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40163296843
