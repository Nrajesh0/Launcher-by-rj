# POCO and final design review

CI uses an Android 16 emulator. These checks need the actual POCO X6 Pro on HyperOS 3; they have not been recorded as passed.

1. Update over the installed APK without uninstalling. Confirm your pages, apps, folders and selected photos remain.
2. Confirm Dot Home holds the default Home role. Test Home, Back and Recents from several apps, then reboot. Keep the stock launcher installed for the OS Recents implementation.
3. Try Classic light/dark and Lucent light/dark. Select a manual palette in Lucent, an external wallpaper and one local photo. Inspect readable text, icon badges, widget surfaces and the dock/search pill. Changing theme must preserve positions and wallpaper selection.
4. Open the widget family tray and an installed provider. Add, configure, resize and remove a widget. Cancel a provider configuration and confirm no stranded placement remains.
5. Open Apps by swiping up. Cancel the hidden-icons swipe halfway, open it fully and go Back. Check hidden-in-search policy. In categories, launch a direct app, open overflow and change one app's category. Test the actual work/private/cloned profiles provided by your firmware, including their locked/quiet states.
6. Arrange a group across pages, drop beside occupied cells and undo it. Freeze Home and confirm layout changes are blocked. Try a new Classic/Lucent starting page and undo it.
7. Import a small photo collection; reorder/remove/crop, try an image with a plain background, swipe along the bottom and test unlock rotation. Revoking the original document grant should not remove an owned imported copy.
8. Test optional calendar, media, usage, camera, DND and activity access one feature at a time, including refusal and revocation. No extra access is required for Home/app launching. Verify the actual compass, step sensor, flashlight, audio route and ringer behaviour on this device. Step totals exclude unmeasured first-use/reboot gaps; device battery values are never guessed.
9. Export to a local document, cancel a restore, then restore a compatible-grid backup. Verify settings and owned photos. A saved wallpaper is applied separately. External widget/document grants may need reconnection.
10. Enable larger system text and a right-to-left locale. Review all routes, labels and TalkBack values without enabling any launcher Accessibility service.

## 60/120Hz measurements

Select each supported refresh mode through the phone's Display settings. Use the same wallpaper, layout and interaction sequence. With ADB connected:

```sh
adb shell dumpsys gfxinfo dev.dothome reset
# Swipe Home pages and Apps; open/close menus, folders and widgets for 30 seconds.
adb shell dumpsys gfxinfo dev.dothome framestats > dot-home-framestats.txt
adb shell dumpsys SurfaceFlinger --latency > display-latency.txt
```

Record the actual display mode, temperature, battery state and Android/HyperOS build with the capture. Android's attribution and SurfaceFlinger output vary by firmware. Compare repeat captures in each mode; an emulator's timing is not a substitute.

## Visual acceptance

Compare real Home/menu/widget screenshots to the supplied reference state, with the same theme, grid and approximate arrangement. Exclude stock status/navigation bars and firmware-only surfaces from launcher claims. Record the remaining design differences and the user's approval separately from functional CI. No parity percentage or pixel-perfect claim is assigned to v5.
