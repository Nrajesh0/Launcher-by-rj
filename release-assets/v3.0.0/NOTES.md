# Dot Home 3.0.0 Preview

Install `dot-home-3.0.0.apk` over your existing Dot Home installation. The package, Home component and signing certificate are preserved; version code 5 supersedes earlier releases. Existing layouts are retained. Enable prereleases for this repository in Obtainium if needed.

## Implemented

- Actual default Home ownership checks, automatic first-run Android chooser, declined-selection handling and a recovery action if Android changes the assignment.
- Recycled alphabetic app drawer with fast scrolling, local search, optional local category folders/manual assignment and hidden-app filtering. Real Home pulls open the drawer from the workspace, tiles and dock, with short/cancelled-pull rollback and horizontal paging.
- In-place multi-selection, selection outlines, group drag and atomic moves between pages. Invalid group placements preserve the previous layout.
- Customisation with the actual Home preview and focused settings. Light/Dark and Classic/Translucent are independent of wallpaper. Apply commits; Cancel/Back rolls back. Offline wallpaper presets and local images remain available.
- Bundled licensed Geist typography, cached icon rendering, adaptive monochrome layers and legible fallbacks preserving app identity.
- Shared widget style/size options, five clock faces, distinct battery styles, live media appearance, breathing/quick controls and standard Android widget hosting.
- Local photos decoded off the UI thread, non-destructive drag/pinch crop editing and rectangle/circle/transparent-source sticker presentation.
- Away Time from optional Android screen-interactive history, labelled as screen-off breaks. No continuous sensor/background tracking service.
- One pinned Gradle build for compilation, resources, dex and release lint.

## Privacy and navigation

There is **no Internet permission, networking implementation or online option**. There is **no Accessibility service, global gesture overlay or secure-settings grant**. Normal Home/Back/Recents is provided by Android. Optional Calendar, Camera, Usage and Notification access is requested only for relevant features. Selected photos use document-picker read grants. Installed third-party widgets retain their own app permissions.

## Verification and boundaries

The installable APK is signed locally from the exact GitHub Actions artifact, with its payload and original certificate checked before publication. Compilation, model/migration/offline checks and the complete Android 16 UI workflow must pass. Exact source run: https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40199817088 (build and complete UI suite passed). The suite additionally exercises reversible wallpaper previews, live inverted-media styling and real provider configuration/cancellation/removal. See provenance.json in the repository.

Physical POCO X6 Pro / HyperOS 3 testing remains pending. Native gesture availability, the navigation bar, global app transitions, Recents, lock screen/AOD, Quick Settings and Glyph hardware remain controlled by the phone's OS. This preview does not claim exact Nothing OS parity, 120 Hz performance or measured battery life. Transparent stickers need a source image with transparency; no AI cutout is included. App categories use local metadata/manual choices. Weather and online services are excluded.

Phone acceptance checklist: https://github.com/Nrajesh0/Launcher-by-rj/blob/main/docs/device-acceptance.md
