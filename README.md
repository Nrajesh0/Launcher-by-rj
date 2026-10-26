# Dot Home

An independent, offline Android launcher inspired by Nothing OS. Version 4 replaces the previous workspace with a pinned Launcher3/Lawnchair engine, locally bundled monochrome icons, original widget artwork and focused customisation screens. It does not bundle Nothing binaries or firmware.

## Install

Download the versioned APK from [Releases](https://github.com/Nrajesh0/Launcher-by-rj/releases). Open Dot Home and choose it in the default Home prompt. Reopen the chooser under **Home settings → Default Home app**. Updates retain `dev.dothome` and the original signing certificate; keep your installed app when updating.

- Swipe up for Apps; use the local Search bar to search apps. Swipe right in Apps for Hidden icons.
- Long press blank Home space for Home settings, Widgets, Customisation and Arrange.
- Drag apps and widgets using native workspace placement, reorder, folders and resize. Long press app shortcuts for Enlarge/Shrink.
- Arrange selects apps, folders and widgets. Move sends a selection to a page; holding and moving a selected item drags the group. Folder groups selected apps; Remove removes Home items. Done exits.
- Select one widget in Arrange and tap Style for faces, colours and configuration. Select a folder and tap Style for its cover.
- Customisation offers the real Home preview, Theme & Style, local wallpapers, icon packs, grid and labels. Light/dark is independent of Classic, Translucent and Dynamic. Picked images have a drag/pinch crop preview.
- Home settings provides freeze layout, drawer options, hidden icons and local layout backup/restore.

## Widgets and permissions

Local clock faces, date, battery, Focus, Away time, photos/stickers, notes, calendar, countdown, elapsed timer, usage, compass, media and quick controls share native drag/resize/persistence. The provider-organised tray also hosts installed Android widgets.

**No Internet permission, network implementation or online option. No Accessibility service, global gesture overlay or secure-settings permission.** Calendar and flashlight ask for their optional permissions when used; photos use Android's document picker. Media/notification dots and usage history have optional system access settings. Bluetooth, sound and DND open supported Android settings. A ringing timer opens Clock. Other apps and widget providers have their own permissions.

Android owns Home, Back, Recents and global navigation gestures. Dot Home uses your selected system navigation mode. Manufacturer restrictions on third-party launcher gestures remain a firmware limitation; ADB and Accessibility are not required by this app. Keep your stock launcher installed for the system Recents implementation.

## Boundaries and data

The launcher is independently implemented and does not claim exact Nothing OS parity. Lock screen, notification shade, Quick Settings, Glyph hardware, Essential/cloud services and privileged app-to-Home animations are outside a normal launcher. Earbud battery APIs depend on the connected device/provider. Original optical wallpapers are included; official Nothing wallpaper assets are not copied.

Upgrades retain the old JSON layout and a recovery copy, then import native items in one database transaction. Local backups contain placements, widget settings and document references, not third-party widget internals or portable photo grants. Restore confirms replacement and checks the grid, bounds, item identities and collisions first. External widgets may need reconnection.

## Build and verification

Use JDK 21, Android SDK platforms 36 and 36.1, build tools 36.1.0; accept SDK licences and set `ANDROID_HOME`. Minimum Android 15; target Android 16.

```sh
bash scripts/test.sh
bash build.sh --unsigned
```

The build prepares pinned upstream source, applies checked patches/overlays, converts pinned icons and checks the packaged privacy contract. Build-time downloads do not add network access to the APK. Output: `build/dot-home-4.0.0-unsigned.apk`. See [foundation instructions](foundation/README.md), [source pins](foundation/upstream.properties) and [research/acceptance record](docs/reference/v4-audit.md).

GitHub Actions builds the release APK, exercises actual UI interactions and performs a v3 installation-upgrade fixture. Screenshots and filtered runtime diagnostics accompany the run. The release APK is signed locally with the existing private key; publication verifies its signature, checksum and byte-for-byte payload against the successful CI artifact. Private signing keys are never committed.

Launcher3/Lawnchair, System UI support libraries and Lawnicons retain their licences and notices. Geist is bundled under OFL. Attribution is available in Home settings and APK assets.
