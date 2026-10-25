# Dot Home

An independent native Android home-screen replacement inspired by Nothing's monochrome visual language. Targets POCO X6 Pro / HyperOS 3 / Android 16; minimum Android 15. This repository contains original Java code and vector artwork, with no copied Nothing binaries, fonts or branded assets.

## Install and use

Install the versioned APK from [GitHub Releases](https://github.com/Nrajesh0/Launcher-by-rj/releases), open Dot Home and choose it in Android’s default-launcher prompt, which opens once on first launch when Dot Home is not already the default. If dismissed, use **Choose default Home** in setup. You can reopen setup under Customisation → Setup & permissions, or use Home settings → Set as default Home. Android may ask you to permit installation from the app used to download the file. Keep POCO Launcher installed.

- Swipe up or tap Search apps for the integrated local drawer. Search, categories and hidden-app controls work offline.
- Long press empty space for page management, Widgets, Customisation and Home settings.
- Long press and drag apps or widgets to a free cell. Drag to an edge to change pages, onto an app to create a folder, or onto a folder to add it. Empty space is preserved. Release without moving to open item actions.
- Use item actions to enlarge icons/folders, choose folder covers or widget styles, and activate a lower-right resize handle. Invalid drops/resizes preserve the existing layout.
- Enlarged folders launch their visible apps directly. Tap the folder label to open all contents; drag an app out to extract it.
- The unified Widgets screen shows built-in previews, style/size choices and installed Android widget providers. Long press a built-in preview to drag it onto Home, or select a size to place it automatically.
- Tap or long press a dock slot to choose its app. Swipe horizontally or tap a page indicator to change pages; add pages from the overview.
- Customisation includes live theme preview, visual grid choices, labels, icons, wallpaper, layout lock, notification dots, reduced motion and navigation setup. Advanced Home settings include backups, import, recovery and icon packs. Add a Styled page from Customisation for a sparse reference-inspired arrangement without replacing your layout. Original Ocean/Soft mist wallpaper presets are bundled offline. Appearance switches the coordinated light/dark palette without moving items; user-selected wallpapers remain unchanged. The built-in sticker preview uses an original decorative car drawing; tap it to choose your own local image. Built-in item actions include colour and background opacity; photos support rectangle, circle and transparent-source sticker styles.

### Navigation on Android

Swipe up within Home—including over a tile or the dock—to open Apps. Short/cancelled swipes stay on Home, horizontal swipes change pages, and hosted Android widgets retain their own vertical interactions. These are in-app gestures and require no permission.

Android handles system Home, Back and Recents with the gestures or buttons selected in your phone's settings. Dot Home registers as a normal default Home app, responds to the Android Back callback and respects system navigation insets. It has no Accessibility service, global gesture overlays, input injection, secure-settings grants or replacement navigation bar. Choose **Customisation → System navigation** to open the phone's navigation settings. No Accessibility grant, ADB or root is needed for normal launcher operation.

Some manufacturers restrict native gesture navigation with third-party launchers. Being a Play Store launcher does not bypass this restriction; see the [primary-source comparison](docs/navigation-research.md). Keep the stock system launcher installed because Android may use it for Recents. If you previously hid the system buttons with ADB, [restore the original navigation setting](docs/adb-navigation.md) before relying on this version.

## Included widgets

Dot-matrix, minimal analogue, tick dial, serif and rounded digital clocks, date, battery, breathing, an elapsed timer, media controls, upcoming calendar event, photo, editable note, date countdown, compass, aggregate app usage and quick controls. Standard Android provider widgets can be bound, configured, resized and removed.

**The app has no Internet permission, networking implementation, or online option.** Version 1.1.0 removes weather, city search and the Internet-settings shortcut. Existing weather tiles are removed from saved layouts, imported backups, recovery and undo. No analytics, advertising, telemetry, remote configuration or in-app update requests are included. Other installed apps and widget providers run under their own permissions.

Calendar access is requested when its tile is used. Media controls require explicitly enabling Dot Home's notification listener in system settings; notification dots retain only package names, not notification text. Usage access is optional. Photos use the system document picker and persisted read grants. Flashlight access requests camera permission. Core launcher operation needs none of these optional grants. Version 2.0.1 removes the earlier Accessibility navigation service and recovery tile entirely; Android revokes the removed component on upgrade.

Bluetooth, DND and sound controls open supported system settings. Ringing timers are delegated to Clock; the date-countdown widget is not a background alarm service. Compass uses an available rotation-vector sensor while the activity is resumed.

## Current boundaries

This is a new independent launcher implementation, not a port of Nothing OS or the originally proposed Launcher3/Lawnchair fork. Native HyperOS gestures, Recents, lock screen/AOD, global transitions, Glyph hardware and proprietary Essential services are outside its control. It has not been measured against a physical Nothing reference device and does not claim exact Nothing OS parity.

Version 2 adds explicit cell placement, dragging, resize handles, dynamic pages, animated sheets/folders, a unified widget gallery and original clock/control artwork. It is not feature-complete Nothing OS: proprietary folder-cover artwork, pedometer tracking, AI services, widget stacks and full Private Space integration are not included. Category defaults come from Android app metadata, with manual overrides. Standard provider widgets retain their own styles and permission requirements.

Icon packs with common launcher discovery intents and XML appfilter mappings are supported. Monochrome mode uses an app's adaptive monochrome layer where available, with a desaturated fallback; it does not generate original artwork for every logo.

Usage totals are aggregate app foreground time, not a certified equivalent of Android Digital Wellbeing screen time. Media controls listen for active-session playback/metadata changes while Home is resumed, with periodic refresh as a fallback. No continuous background sensor service is used.

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

`version.properties` is the single version source: versionName 2.0.1, versionCode 4. Both the manifest and About screen receive these values during build. Increase versionCode on every release; push a matching tag such as `v2.0.1` after updating the version name. CI rejects mismatching tags.

Pushes to main, version tags, and manual workflow runs compile the app, run model/offline tests and Android 16 emulator UI checks, and upload versioned APK/checksum artifacts plus screenshots. Emulator testing uses an ephemeral runner-only signing identity; release signing retains the existing private identity locally. Without signing secrets, CI produces an explicitly unsigned artifact which cannot be installed directly. Tagged builds automatically publish installable APKs to GitHub Releases when signing secrets are configured. Locally signed releases may also be attached after CI passes. Build-time toolchain/test downloads do not give the installed APK Internet access.

Automatic release signing needs these encrypted Actions secrets: ANDROID_SIGNING_KEY_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD, ANDROID_KEY_ALIAS. Exporting the existing signing identity to GitHub requires the owner's explicit approval and has not been done. Reusing that identity lets new releases update the original APK. No private key is committed, published as an artifact, or silently regenerated by CI. Temporary runner keys are deleted after a signed build.

To sign a downloaded CI artifact locally while keeping the key off GitHub, set SIGNING_KEYSTORE and SIGNING_STORE_PASS in your local environment, then run `bash scripts/sign_apk.sh /absolute/path/to/dot-home-<version>-unsigned.apk`. It writes the installable APK and checksum to build/release. The same signing key must be reused for future upgrades.

The manual **Publish locally signed release** workflow publishes the public signed APK under release-assets after verifying its checksum, signing certificate, source tag and passing source-build run. This path requires no signing-key export. The staged APK is already public distribution material; no private key or password is included.
