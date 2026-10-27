Dot Home 5.0.0 builds on the native launcher foundation with a coordinated Nothing-inspired appearance and fuller local widgets.

- Classic and Lucent in light/dark, Dynamic colour, wallpaper/manual palettes, local display fonts and original filled primary icons.
- Page-aware Home and grid previews, coordinated menus, folder cover/emoji choices and compact family editors.
- Native swipe-up Apps drawer, category cards, hidden icons and offline search scopes. Optional contacts and explicitly selected document names.
- Group dragging with artwork/count feedback, neighbour reflow and guarded undo. Optional Classic/Lucent layouts add a page while retaining existing pages.
- Family-first widget catalogue and standard installed-app widget picker, including tested bind/configure/cancel/remove.
- Focus/Relax/Calm breathing, Away Time detail, manual weather, measured steps, notes, calendar, media and clock/date/battery variants.
- Owned photo collections with reorder/remove/crop, plain-background cutout, outline, bottom-edge swiping and unlock rotation.
- Local Glass/Atmosphere image effects, optional visible-only motion wallpaper and original optical wallpaper choices.
- Versioned local settings/layout/media backup with reconnect handling and staged image restore.
- Reduced motion, predictive Back on owned screens and screen-reader values.

Install over the existing app to retain data. Package `dev.dothome`, Home component and original signing certificate are retained. Version code: 7.

Home and app organisation need no extra permission. Calendar, contacts search, flashlight and pedometer request optional access when used. Media/notification dots, usage history and DND use optional Android special-access settings.

**No Internet permission, network implementation, online option, Accessibility service, root dependency or global navigation overlay.** Android owns Back, Home, Recents and global navigation. Lock screen, AOD, Quick Settings, Glyph and Nothing cloud services remain outside this APK.

Weather is manually entered. Steps cannot reconstruct first-use/reboot gaps. Audio widgets do not invent per-ear/case battery. Sticker cutout handles edge-connected plain backgrounds, not semantic subjects. Third-party widget/document grants may need reconnecting after restore.

Full Android 16 emulator build, UI and v3/v4 upgrade checks passed in [source CI](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40563188991). Physical POCO timing, OEM profile compatibility and final user visual approval remain device checks. This independent implementation does not claim pixel-perfect Nothing firmware parity.

[Implementation and verification](https://github.com/Nrajesh0/Launcher-by-rj/blob/main/docs/implementation/release-verification-v5.md)
