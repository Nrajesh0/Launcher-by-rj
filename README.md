# Dot Home

An independent native Android home-screen replacement inspired by Nothing's monochrome visual language. Targets POCO X6 Pro / HyperOS 3 / Android 16; minimum Android 15. This repository contains original Java code and vector artwork, with no copied Nothing binaries, fonts or branded assets.

## Install and use

Install the versioned APK from [GitHub Releases](https://github.com/Nrajesh0/Launcher-by-rj/releases), open Dot Home, then choose **··· → Set as default Home**. Android may ask you to permit installation from the app used to download the file. Keep POCO Launcher installed. Select a supported navigation mode in HyperOS Settings.

- Tap **＋** to add apps, built-in tiles, Android widgets or folders.
- Tap the bottom search bar to open all apps. Search works locally; Enter launches the first matching app. Use the category selector to filter apps.
- Long press an app in the drawer to add it to Home, view app information/shortcuts, assign a category or hide it. Hidden apps remain searchable; this is not secure app locking.
- Long press a Home item to resize, swap positions, move to another page, edit or remove it. For swapping, choose Move and tap the destination item. Back cancels.
- Enlarged folders launch their visible apps directly; tap the folder title to open its full contents, add apps or rename it. Long press an app in the open folder to remove it.
- The three page indicators change Home pages. Swipe horizontally over the workspace to change pages.
- Long press the dock to configure its four slots.
- **···** contains dark/light mode, monochrome icons, 4/5 columns, labels, icon packs, wallpaper, layout lock, undo, backup, import and recovery.

## Included widgets

Dot clock, date, battery, media controls, upcoming calendar event, photo, editable note, date countdown, compass, aggregate app usage and quick controls. Standard Android provider widgets can be bound, configured, resized and removed.

**The app has no Internet permission, networking implementation, or online option.** Version 1.1.0 removes weather, city search and the Internet-settings shortcut. Existing weather tiles are removed from saved layouts, imported backups, recovery and undo. No analytics, advertising, telemetry, remote configuration or in-app update requests are included. Other installed apps and widget providers run under their own permissions.

Calendar access is requested when its tile is used. Media controls require explicitly enabling Dot Home's notification listener in system settings; unrelated notifications are not stored. Usage access is optional. Photos use the system document picker and persisted read grants. Flashlight access requests camera permission. Core launcher operation needs none of these optional grants.

Bluetooth, DND and sound controls open supported system settings. Ringing timers are delegated to Clock; the date-countdown widget is not a background alarm service. Compass uses an available rotation-vector sensor while the activity is resumed.

## Current boundaries

This is a new independent launcher implementation, not a port of Nothing OS or the originally proposed Launcher3/Lawnchair fork. Native HyperOS gestures, Recents, lock screen/AOD, global transitions, Glyph hardware and proprietary Essential services are outside its control. It has not been measured against a physical Nothing reference device and does not claim exact Nothing OS parity.

The initial version has three pages, deterministic packed placement and tap-to-swap movement. Free-position dragging, Nothing's full folder-cover library, its complete clock collection, pedometer tracking, AI features, widget stacks and full Private Space integration are not implemented. Category defaults come from Android app metadata, with manual overrides; they do not reproduce Nothing's proprietary categorisation. Standard provider widgets retain their own styles and permission requirements.

Icon packs with common launcher discovery intents and XML appfilter mappings are supported. Unsupported mappings fall back to the app's own icon. Monochrome mode desaturates icons rather than claiming all logos can be converted into perfect Nothing artwork.

Usage totals are aggregate app foreground time, not a certified equivalent of Android Digital Wellbeing screen time. Media controls currently refresh on interaction and at a visible 30-second interval. No continuous background sensor service is used.

Backups contain layouts and references, not third-party widget internals or portable grants to external photos. Android widgets need rebinding after import. Import validates structure and preserves a pre-import recovery copy. The recovery copy can be restored from Home settings. A corrupt saved file is preserved as `unreadable-layout.json` in app-private storage before defaults are used.

## Build

The supplied APK is built with `build.sh` using JDK 21, Android platform 36 and build tools 36.0.0. It has no external runtime libraries. Set Java's `bin` directory on PATH and run:

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk BUILD_TOOLS_DIR=/path/to/android-sdk/build-tools/36.0.0 bash build.sh
```

The current environment's extracted tools directory is named `android-16`; `BUILD_TOOLS_DIR` overrides that local default. Output is `build/dot-home-<version>.apk` plus a SHA-256 file. An Android Studio/Gradle project is also supplied; that build path has not been executed in this environment.

`build.sh` generates a local signing key in `keys/` if missing and reuses it for updates. The keystore password in the script is for local development and is not a production secret-management scheme. Preserve the key privately to continue updating the installed build. The source archive excludes keys and generated build files. A separately built APK signed with a different key cannot update the existing installation directly.

Run model, backup-validation, migration and offline-contract tests:

```sh
bash scripts/test.sh
```

The JVM test runner downloads a pinned, checksum-verified JSON library for tests only. It is not packaged in the APK.

## Verification

See `VERIFICATION.md` for build, tests and device-test status. Physical POCO testing is required before daily-use reliability or native-navigation compatibility can be established.

## Licence

This repository retains its existing GPL-3.0 licence in `LICENSE`. Original Dot Home source and vector artwork are available under Apache License 2.0, preserved in `LICENSES/Apache-2.0.txt`. Android platform APIs and build tools are supplied by their respective owners under their own terms. Nothing, POCO and Xiaomi names identify compatibility and inspiration; this app is independent of those companies.

## GitHub Actions and releases

`version.properties` is the single version source: versionName 1.1.0, versionCode 2. Both the manifest and About screen receive these values during build. Increase versionCode on every release; push a matching tag such as `v1.1.0` after updating the version name. CI rejects mismatching tags.

Pushes to main, version tags, and manual workflow runs compile the app, test it and upload versioned APK/checksum artifacts. Without signing secrets, CI produces an explicitly unsigned artifact which cannot be installed directly. Tagged builds automatically publish installable APKs to GitHub Releases when signing secrets are configured. Locally signed releases may also be attached after CI passes. Build-time toolchain/test downloads do not give the installed APK Internet access.

Automatic release signing needs these encrypted Actions secrets: ANDROID_SIGNING_KEY_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD, ANDROID_KEY_ALIAS. Exporting the existing signing identity to GitHub requires the owner's explicit approval and has not been done. Reusing that identity lets new releases update the original APK. No private key is committed, published as an artifact, or silently regenerated by CI. Temporary runner keys are deleted after a signed build.

To sign a downloaded CI artifact locally while keeping the key off GitHub, set SIGNING_KEYSTORE and SIGNING_STORE_PASS in your local environment, then run `bash scripts/sign_apk.sh /absolute/path/to/dot-home-<version>-unsigned.apk`. It writes the installable APK and checksum to build/release. The same signing key must be reused for future upgrades.
