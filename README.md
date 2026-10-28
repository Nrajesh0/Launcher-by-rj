# Dot Home

An independent, offline Android launcher inspired by Nothing OS. Version 5 builds on the native Launcher3/Lawnchair foundation with Classic/Lucent themes, coordinated palettes, original filled primary icons, a widget-family catalogue and richer local widgets. It does not bundle Nothing binaries or firmware.

## Install

Download the versioned APK from [Releases](https://github.com/Nrajesh0/Launcher-by-rj/releases). Open Dot Home and choose it in the default Home prompt. Reopen the chooser under **Home settings → Default Home app**. Updates retain `dev.dothome` and the original signing certificate; keep your installed app when updating.

- Swipe up for Apps; use the local Search bar to search apps. Swipe right in Apps for Hidden icons.
- Long press blank Home space for Home settings, Widgets, Customisation and Arrange.
- Drag apps and widgets using native workspace placement, reorder, folders and resize. Long press app shortcuts for Enlarge/Shrink.
- Arrange selects apps, folders and widgets. Move sends a selection to a page; holding and moving a selected item drags the group. Folder groups selected apps; Remove removes Home items. Done exits.
- Select one widget in Arrange and tap Style for faces, colours and configuration. Select a folder and tap Style for its cover.
- Customisation offers the real Home preview, Theme & Style, local wallpapers, icon packs, grid and labels. Light/dark is independent of Classic, Lucent and Dynamic. Picked images have a drag/pinch crop preview.
- Home settings provides freeze layout, drawer options, hidden icons and local layout backup/restore. Search scopes include apps, shortcuts, calculator, settings, optional contacts and file names explicitly chosen through Android’s document picker.

## Widgets and permissions

Local clock faces, date, battery, Focus, Away time, photos/stickers, notes, calendar, countdown, elapsed timer, usage, compass, media and quick controls share native drag/resize/persistence. The family-first tray also opens the standard installed-app widget picker. Optional Classic/Lucent presets add a new page; existing pages and the dock stay intact.

**No Internet permission, network implementation or online option. No Accessibility service, global gesture overlay or secure-settings permission.** Calendar, contacts search and flashlight ask for their optional permissions when used; photos use Android's document picker. Media/notification dots and usage history have optional system access settings. Activity recognition is requested for the pedometer. Sound uses the public ringer API; DND asks for optional notification-policy access; Bluetooth opens system settings. A ringing timer opens Clock. Other apps and widget providers have their own permissions.

Android owns Home, Back, Recents and global navigation gestures. Dot Home uses your selected system navigation mode. Manufacturer restrictions on third-party launcher gestures remain a firmware limitation; ADB and Accessibility are not required by this app. Keep your stock launcher installed for the system Recents implementation.

## Boundaries and data

The launcher is independently implemented and does not claim exact Nothing OS parity. Lock screen, notification shade, Quick Settings, Glyph hardware, Essential/cloud services and privileged app-to-Home animations are outside a normal launcher. Earbud battery APIs depend on the connected device/provider. Original optical wallpapers are included; official Nothing wallpaper assets are not copied.

Upgrades retain the old JSON layout and a recovery copy, then import native items in one database transaction. Version 2 local backups contain placements, appearance/search/organisation settings and bounded copies of owned collection images. External documents and third-party widgets may need reconnection. The system wallpaper stays selected; a backed-up wallpaper image is available in the crop/apply route. Staging and error rollback protect existing media/layout when restore fails; no cross-service power-loss atomicity is claimed. Restore confirms replacement and checks the grid, bounds, item identities and collisions first. External widgets may need reconnection.

## Build and verification

Use JDK 21, Android SDK platforms 36 and 36.1, build tools 36.1.0; accept SDK licences and set `ANDROID_HOME`. Minimum Android 15; target Android 16.

```sh
bash scripts/test.sh
bash build.sh --unsigned
```

The build prepares pinned upstream source, applies checked patches/overlays, converts pinned icons and checks the packaged privacy contract. Build-time downloads do not add network access to the APK. Output: `build/dot-home-5.0.0-unsigned.apk`. See [foundation instructions](foundation/README.md), [source pins](foundation/upstream.properties) and [implementation plan](docs/implementation/nothing-experience-plan.md) and [74-item audit](docs/reference/v4-nothing-comparison-2026-10-26.md).

GitHub Actions builds the release APK, exercises actual UI interactions and tests v3 migration and an install update from the actual released v4 APK. Screenshots and filtered runtime diagnostics accompany the run. The release APK is signed locally with the existing private key; publication verifies its signature, checksum and byte-for-byte payload against the successful CI artifact. Private signing keys are never committed.

Launcher3/Lawnchair, System UI support libraries and Lawnicons retain their licences and notices. Geist, Libre Caslon Display and Space Mono are bundled under OFL. Attribution is available in Home settings and APK assets.

Photo collections support up to 24 locally imported images, reorder/remove, unlock rotation and swiping along the bottom of the tile. Sticker background removal is intended for plain backgrounds; it is not semantic subject segmentation. Weather is manually entered and timestamped. Step totals start from the first sensor reading that day; no background service reconstructs missing history. Audio-device widgets never guess per-ear/case battery percentages. Physical POCO 60/120Hz timing and OEM profile behaviour remain device checks.

See the [v5 verification record](docs/implementation/release-verification-v5.md) and [actual app review sheet](docs/reference/v5-actual-review.jpg) for tested behaviour and remaining device checks.
