# POCO / HyperOS navigation with ADB

Use this after installing Dot Home 2.0.0 or later. The app stays offline and does not need a WRITE_SECURE_SETTINGS grant. ADB runs on your computer over USB. No root, bootloader unlock, system-file replacement or stock-launcher removal is involved.

1. Install Google's [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools). Enable USB debugging under Developer options, connect your unlocked POCO, and approve the computer's RSA prompt. In PowerShell use `./adb.exe` instead of `adb`; on macOS/Linux use `./adb` if it is not on PATH.
2. Run `adb devices`. Continue only when your phone says `device`, not `unauthorized`/`offline`. If several devices are listed, add `-s YOUR_DEVICE_ID` after every `adb` below.
3. Install the release APK if needed: `adb install -r dot-home-2.0.0.apk`. Open Dot Home, choose it as default Home, then enable **Dot Home edge gestures** in Accessibility settings. Sideloaded apps may require App info → ⋮ → Allow restricted settings first. Add the Dot Home navigation Quick Settings recovery tile. Test Back, Home and Recents while the buttons are still present.
4. Keep the computer connected while changing navigation. Save the original value:

```sh
adb shell settings get global force_fsg_nav_bar > dot-home-navigation-before.txt
```

First try the same vendor screen offered by the app:

```sh
adb shell am start -a com.android.settings.NAVIGATION_MODE_SETTINGS
```

Choose gesture navigation there if HyperOS allows it. If the screen is unavailable or the choice is blocked, and the replacement gestures are working, the following is a **firmware-dependent attempt to hide the buttons**:

```sh
adb shell settings put global force_fsg_nav_bar 1
adb shell settings get global force_fsg_nav_bar
```

A returned `1` proves only that the setting was stored. Check the actual display, app switching, keyboard, Back, Home and Recents. This key does not install Xiaomi's native gesture engine. HyperOS may ignore it or reset it on launcher changes, reboot or updates. Do not repeatedly force the setting if gestures are failing. If the command returns a SecurityException, stop and use the settings screen/restore path; do not grant broad permissions or disable system packages.

## Restore buttons

For immediate recovery on HyperOS:

```sh
adb shell settings put global force_fsg_nav_bar 0
adb shell am start -a com.android.settings.NAVIGATION_MODE_SETTINGS
```

Choose Buttons if necessary. To restore the exact prior setting, read `dot-home-navigation-before.txt`: use `settings put global force_fsg_nav_bar 0` or `1` for the saved numeric value. If the saved value was `null`, remove the override instead:

```sh
adb shell settings delete global force_fsg_nav_bar
```

Restore working system navigation before disabling/uninstalling the edge service. Keep POCO Launcher installed. If HyperOS stops the service, return through the recovery tile or ADB, restore buttons, and review autostart/battery restrictions. USB debugging can be switched off after setup; firmware resets can require another USB session.

## Evidence and limits

Google documents the [USB/RSA ADB setup](https://developer.android.com/tools/adb). The [HyperGesture maintainer's HyperOS integration record](https://github.com/h3nc4/HyperGesture/blob/main/docs/hyperos-navigation.md) distinguishes opening vendor settings/hiding buttons from implementing edge actions. The [Force FSG maintainer's script](https://github.com/hykilpikonna/force-fsg-nav-bar-ksu/blob/main/service.sh) uses the same key and documents firmware resets; its [uninstall script](https://github.com/hykilpikonna/force-fsg-nav-bar-ksu/blob/main/uninstall.sh) restores `0`. We do not install that root module or run its repetition loop.

These are reversible vendor-setting instructions, not a verified fix for every POCO X6 Pro build. Physical testing on your device remains necessary. No commands have been executed on your phone from this workspace.
