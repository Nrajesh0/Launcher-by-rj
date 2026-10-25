# Phone acceptance for 3.0.0

Automated emulator checks establish the flows recorded in VERIFICATION.md. Complete these on the POCO X6 Pro / HyperOS 3 / Android 16 before relying on the preview as a daily launcher. These are pending tests, not reported results.

1. Install over the earlier signed release without uninstalling. Confirm existing layout, local photos, dock and hidden apps remain. Record the full HyperOS build and POCO Launcher version.
2. Select Dot Home in Android's default Home chooser. Press the system Home button/gesture from Settings and another app. Confirm Dot Home returns with no repeated chooser. If Android changes the assignment, confirm Home displays the recovery action.
3. Choose native gesture navigation in the phone's settings if available. Check Back, Home and Recents. Keep the stock launcher installed for the OS Recents component. No Accessibility or protected-settings grant is required by Dot Home. Report manufacturer restrictions separately.
4. Open Apps by swiping from blank Home, a tile and the dock. Reverse/cancel short pulls. Page horizontally. Search with the keyboard, launch with Enter, return Home and use Back to close the keyboard/drawer. Confirm hidden apps stay out of Home, dock, folders and ordinary search.
5. Apply and cancel Light/Dark and Classic/Translucent independently of wallpaper. Try each offline preset and a local wallpaper. Background the app during an uncommitted preview; it must roll back. Check large system text, landscape and RTL.
6. Drag, resize and enlarge items beside occupied cells; verify invalid drops leave the layout intact. Move a selected group between pages, cancel, undo, freeze/unfreeze layout, then force-stop/relaunch.
7. Bind a real third-party widget, accept configuration, scroll inside it and resize within its declared constraints. Repeat but cancel configuration and consent. Remove it and confirm the provider updates stop. Some providers keep their own theme.
8. Choose a local photo, drag/pinch the crop, Apply/Cancel, reboot and reopen. Remove/revoke the image and confirm a recoverable unavailable state. Transparent-source sticker mode does not extract a background automatically.
9. Deny then grant Calendar, Camera, Notification access and Usage access only when needed. Revoke each while a related widget exists. No data should be invented. Away Time is screen-off history supplied by Android, not physical phone-placement detection.
10. Play/pause media, change track, disconnect the playback source, lock/unlock and return Home. Check date/time/timezone changes and reduced motion. Export/import a mixed layout with the system document picker; third-party widgets need rebinding and external image grants are not portable.

A seven-day phone trial and matched stock-launcher battery/frame measurements remain pending. Emulator frame statistics do not establish 120 Hz smoothness, battery life, OEM gesture compatibility or exact Nothing OS parity.
