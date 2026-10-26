# Replacement launcher engine

This source adaptation replaces the previous Activity-based workspace with a pinned Launcher3/Lawnchair core. The default build and release path uses this engine.

## Reproduce

Use JDK 21, Android SDK platforms 36 and 36.1, and build tools 36.1.0. Accept the Android SDK licences, set `ANDROID_HOME`, then run:

```sh
DOT_BUILD_TYPE=Debug bash scripts/build_foundation.sh
```

The script creates an owned, ignored `build/foundation` checkout, verifies the upstream commit and System UI library submodule, applies checked patches/overlays, and converts pinned Lawnicons SVGs to local vector resources. Gradle and the converter download build dependencies; the installed app has no Internet permission or networking implementation. The packaged manifest and DEX privacy check runs after the build. `Release` builds unsigned output for local signing with the existing release identity. Never upload a private key.

`foundation/upstream.properties` records all upstream revisions. The resulting sources are in `build/foundation`; the exact adaptation is the committed patches and overlays. A patch aborts when its expected upstream source changes. The preparation script resets only the checkout it owns.

## Verification

`.github/workflows/android.yml` builds the release variant independently and exercises the actual Android launcher, role chooser, swipe drawer, hidden-icons page, settings/theme screen, widget editing/tray, batch movement and a synthetic v3 installation upgrade. Emulator screenshots and failure logs are saved as `foundation-evidence`. Functional checks are separate from the [visual/upgrade acceptance checklist](../docs/reference/v4-audit.md). Debug test certificates are disposable and must not be used to publish an update.

## Attribution

Launcher3/Lawnchair and the pinned System UI support library retain their upstream copyright and licence notices. Lawnchair's root licence is Apache 2.0. Lawnicons vectors and component mappings are Apache 2.0. Geist is under the SIL Open Font License. Copies and resource provenance are included in APK assets. The procedural wallpapers and launcher-specific integration are original adaptations; Nothing binaries, branded wallpaper files, and system firmware are not bundled.
