# Changelog

## 3.0.0 — 2026-10-25

- Verify actual default Home ownership on selection/resume, handle declined selection, and show recovery when a previously confirmed assignment is lost.
- Use a dedicated recycled drawer with alphabetical fast scrolling, category folders/manual assignment, private hidden-app search handling and reversible pull motion.
- Add in-place multi-selection, transactional group moves across pages and collision/bounds rollback.
- Rework Customisation into Home previews and focused controls; apply/cancel appearance independently of wallpaper. Bundle licensed Geist typography.
- Add async local photo display and non-destructive drag/pinch crop editing, plus screen-off break history with explicit optional usage access.
- Share widget style/size capabilities between configuration and rendering; remove redundant/non-functional style choices.
- Use one checksum-pinned Gradle build for local/CI release compilation, resources, dex and release lint. Keep the original signing identity local.
- No Internet permission, Accessibility service, global gesture overlays or protected-settings grant.

## 2.0.1 — 2026-10-25

- Fix real swipe-up drawer opening across Home tiles, workspace and dock. Cancel child clicks before completing the gesture and animate the full drawer into view. Preserve horizontal paging, short/cancelled swipe rollback and hosted-widget scrolling.
- Remove the Accessibility edge navigation service, global touch windows, recovery tile and associated setup prompts. Use normal Android Home/Back/Recents integration and system navigation settings.
- Add physical swipe regression checks and normal system navigation checks with no Accessibility grant.
- Retain offline operation, layout data, package name and signing certificate. Increment version code to 4 for updater detection.

## 2.0.0 — 2026-10-25

- Rebuild Home with explicit cell placement, preserved empty space, direct dragging, collision checks, edge paging and resize handles.
- Add dynamic pages, drag-to-folder creation/extraction, enlarged folders, cover choices and folder open/close transitions.
- Replace the old widget list with a searchable preview gallery, style/size selection and Android provider previews.
- Add original dot-matrix, circular analogue/tick dial, serif and rounded digital clock artwork; Focus breathing orbits and circular quick-control widgets.
- Refine the supplied visual references into sparse optional styled pages, offline wallpaper presets, widget tints/transparency, smaller dock slots and circular monochrome icon containers.
- Add transparent-photo sticker styling, actual crop positioning, responsive enlarged icons and provider-declared widget sizing constraints.
- Add an integrated swipe-up drawer, sheet transitions, reduced motion and visual grid/theme customisation.
- Use adaptive monochrome icon layers when available; add optional local notification dots and live media callbacks.
- Add optional Accessibility-based Back/Home/Recents edge gestures and a recovery Quick Settings tile. No ADB, computer or root is required for these actions; hiding HyperOS buttons remains firmware-dependent. The service cannot read window contents.
- Ask for the Android Home role automatically once on first launch, with a reusable chooser button.
- Add first-run Home-role/permission setup and a reusable optional-access page. Include reversible ADB instructions for firmware button hiding.
- Add Android 16 emulator UI checks and screenshot artifacts in GitHub Actions.
- Preserve offline-only operation, upgrade migration, package and signing identity.

## 1.1.0 — 2026-10-25

- Remove Internet permission and all HTTP, weather, forecast and city-search code.
- Remove the built-in weather option and Internet-settings shortcut.
- Migrate existing layouts, imports, recovery and undo to remove legacy weather tiles.
- Delete cached weather preferences during upgrade.
- Centralise APK/About versioning in version.properties (version code 2).
- Add GitHub Actions compilation, model tests, offline-contract checks and versioned artifacts.
- Add tagged release publication when signing secrets are configured, with an optional local-signing path that keeps the private key off GitHub.
- Preserve the existing package and signing identity for upgrade compatibility.

Physical POCO/HyperOS runtime testing remains outstanding.
