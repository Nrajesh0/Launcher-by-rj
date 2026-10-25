# Changelog

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
