# Android launcher navigation research — 25 October 2026

A home-screen app has two separate interactions: its own swipe-up app drawer, and Android system navigation between tasks. Dot Home 2.0.1 implements the first within its Activity and delegates the second to Android, as ordinary launchers do. No Accessibility service or global touch overlay is needed for either normal launcher operation or native system gestures on a supporting phone.

Primary sources checked:

- [Nova Launcher FAQ, Navigation](https://www.novalauncher.com/faq/): a third-party launcher cannot configure system navigation buttons or enable system gestures when the OS blocks them. Native gesture compatibility and transition behavior depend on Android and the manufacturer. Nova's optional Accessibility features are separate from this native integration.
- [Niagara's developer documentation](https://help.niagaralauncher.app/article/7-gesture-navigation): supported native gestures use the system launcher/OS implementation. Xiaomi's restrictions are a manufacturer-specific exception, not a missing permission in a normal Home APK.
- [Microsoft Launcher support](https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android): documents the same third-party launcher full-screen gesture restriction on MI devices.
- [Android gesture navigation guide](https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav): reserve system gesture regions for OS navigation, respect insets, and use the standard Back contract. Dot Home does not install gesture exclusion regions to take over system edges.

Implementation: retain the MAIN/HOME/DEFAULT Activity intent filter, Android Home role request, singleTask onNewIntent Home handling and Android OnBackInvokedDispatcher. Remove global Accessibility actions/windows and their setup. Detect vertical Home swipes at the Activity's root view before clickable tiles, dock controls or the horizontal pager consume them. Cancel the child gesture before opening the drawer; do not claim hosted-widget vertical scroll, multi-touch, active drags or the OS navigation inset. Animate drawer opening through its normal transition rather than leaving a partly translated panel outside the viewport.

Limits: system gestures on a supporting OS need no special app permission. Firmware restrictions and privileged Recents animations cannot be repaired by adding ordinary APK permissions. Root/Xposed gesture activation modules exist for some devices but are outside this app and the user's authorized setup. Hiding a navigation bar with ADB alone does not implement working gestures; it is not offered as a fix.

Verification: real emulator touchscreen injection must open the visible drawer from Home and dock; cancelled/short pulls must leave Home intact, horizontal swipes must still page, and Android system Back/Home/Recents must work without enabling an app Accessibility component. Emulator coverage does not establish compatibility with every OEM firmware.
