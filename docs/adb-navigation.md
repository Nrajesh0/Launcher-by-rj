# Restore a previous ADB navigation change

Dot Home 2.0.1 uses Android's native navigation and has no Accessibility navigation service. ADB is not required to use its Home screen or app drawer. Select gestures or buttons in your phone's System navigation settings. Keep the stock system launcher installed.

If you followed an earlier Dot Home preview's HyperOS button-hiding instructions, hiding the bar alone does not provide native gestures. Restore working system navigation before updating or disabling the old service. Open System navigation in the phone's Settings and select Buttons if gestures are unavailable with third-party launchers.

If necessary, the previous change can be reversed using your already authorized computer/USB debugging connection:

```sh
adb devices
adb shell settings put global force_fsg_nav_bar 0
adb shell am start -a android.settings.SETTINGS
```

Then select Buttons in your phone's navigation settings. The vendor key is specific to some Xiaomi/POCO firmware; it is not a general Android navigation API. If you saved `dot-home-navigation-before.txt`, restore its exact prior value instead: use `settings put global force_fsg_nav_bar 0` or `1` for the saved numeric value; if it contained `null`, use:

```sh
adb shell settings delete global force_fsg_nav_bar
```

No ADB commands are run by the app. It requests no WRITE_SECURE_SETTINGS or Accessibility access, and offers no hidden gesture-enabling switch. Normal navigation is owned by the OS. See [the launcher comparison](navigation-research.md) for supporting primary sources.
