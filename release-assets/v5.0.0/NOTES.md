Dot Home 5.0.0 builds on the native launcher foundation with a coordinated Nothing-inspired appearance and fuller local widgets.

- Classic and Lucent in light/dark, Dynamic colour, wallpaper/manual palettes, local display fonts and original filled primary icons.
- Wallpaper palettes follow the backdrop actually shown; Noir and empty-photo contrast are corrected.
- Page-aware Home previews, grid choice previews, coordinated menus, folder cover/emoji choices and compact family editors.
- Home and Apps use Launcher3’s native drawer controller across system navigation modes; system Recents/navbar controllers and onboarding stay with the OS.
- Category cards with direct app targets, overflow and local corrections; a reversible hidden-icons drawer and offline search scopes, optional contacts and explicitly selected document names.
- Artwork/count group dragging, prospective placement, neighbour reflow and guarded layout undo. Optional Classic/Lucent layouts add a new page without replacing existing pages.
- Family-first widget catalogue with the standard installed-app widget picker.
- Guided Focus/Relax/Calm breathing, Away Time weekly detail, per-app screen time, manual weather, measured steps/goal, audio-device states, notes, calendar, clock/date/battery variants and custom media controls.
- Owned photo collections with reorder/remove/crop, plain-background cutout, sticker outline, bottom-edge swiping and unlock rotation.
- Local image Glass/Atmosphere effects, optional visible-only motion wallpaper and original optical wallpaper choices.
- Versioned local settings/layout/media backup, staged image restore and reconnect handling. Current system wallpaper stays selected; saved wallpaper images have a separate crop/apply route.
- Reduced motion, predictive Back on owned screens and live widget values for Android screen readers.

Install over the existing app; do not uninstall if you want to retain its data. Package `dev.dothome`, Home component and original signing certificate are retained. Version code: 7. The release includes a checksum.

Home and app organisation need no extra permission. Calendar, contacts search, flashlight and pedometer request their optional access when used. Media/notification dots, usage history and DND use optional Android special-access settings. Sound uses the public ringer API; Bluetooth opens system settings. Other providers have their own permissions.

**No Internet permission, network implementation, online option, Accessibility service, root dependency or global navigation overlay.** Android/HyperOS owns Back, Home, Recents and global navigation. Lock screen, AOD, Quick Settings, Glyph hardware and Nothing cloud services are outside this APK.

Weather is manually entered and timestamped. Steps start at the first sensor reading of a day and cannot reconstruct first-use/reboot gaps. Audio widgets never invent per-ear/case battery percentages. Sticker cutout removes an edge-connected plain background; it is not semantic subject segmentation. Third-party widget/document grants may need reconnecting after restore.

Android 16 emulator checks and actual screenshots are recorded in the release verification document. Physical POCO 60/120Hz timing, OEM profile compatibility and the user's final visual approval remain device checks. This independent implementation does not claim pixel-perfect Nothing firmware parity.

[Implementation and verification](https://github.com/Nrajesh0/Launcher-by-rj/blob/main/docs/implementation/release-verification-v5.md) · [Actual app screenshots](https://github.com/Nrajesh0/Launcher-by-rj/blob/main/docs/reference/v5-actual-review.jpg) · [Passed source CI](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40563188991)
