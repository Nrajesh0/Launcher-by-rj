**Current update for Dot Home 3.0.0**

The audit below records v1.1.0 and remains the reference for the requested rebuild. Version 2 now implements explicit cell coordinates/migration, preserved empty space, dragging/drop previews/collision checks/edge paging, dynamic pages, a stable launcher root, integrated drawer and sheet motion, folder origin transitions and drag-in/out, cover choices, a unified searchable widget gallery, shared preview/live artwork, clock faces, breathing and individual controls, media artwork/callbacks, photo crop positioning, responsive enlarged icons, visual grid/theme settings and optional notification dots.

Version 3 adds an actually verified Home-role flow, a recycled alphabetic drawer with local categories and hidden-app filtering, transactional batch movement, actual Home previews with reversible appearance/wallpaper controls, licensed offline fonts, local photo crop editing and screen-off break history. Android providers use their minimum/default cell footprint and declared resize axes/bounds.

Accessibility navigation and global gesture overlays were removed. Android owns Home/Back/Recents and the navigation bar. This app uses standard launcher integration without Accessibility, ADB or root; manufacturer restrictions on native third-party gesture navigation remain outside the app. Provider-specific behaviour and HyperOS integration require physical-device testing.

The implementation remains an independent custom launcher. Exact original geometry/motion, proprietary artwork, Smart Drawer algorithms, swipeable widget stacks and physical-device integration have not been established. The original audit's reference-capture/device acceptance gate is still open; do not interpret this rebuild as an exact Nothing OS reproduction. Automated Android 16 UI evidence and final source-run provenance accompany the release.

---

**Nothing Launcher comparison — Dot Home 1.1.0**

Audit date: 25 October 2026. Target phone: POCO X6 Pro / HyperOS 3 / Android 16. The app must remain offline, with no Internet permission or online option.

The current release is a functional launcher prototype, not a faithful Nothing Launcher implementation. The implementation departed from the proposed Launcher3/Lawnchair foundation and replaced core launcher behaviour with a small custom Activity. That choice explains much of the gap. Successful compilation, model tests and release publishing establish packaging and selected invariants; they do not establish visual or interaction fidelity.

Evidence boundary: the current-build findings below come from inspecting its source. Original behaviour is grounded in Nothing's official support pages, app descriptions and release notes. Exact screen geometry, picker presentation, motion curves and model-specific details have not been measured on a physical reference phone. Proposed replacement designs below are requirements, not claims that those measurements have already been made.

1. **The most consequential mismatches**

| Area | Original reference / comparison target | Current implementation | Severity and required change |
|---|---|---|---|
| Home composition | A customisable launcher workspace with app/widget placement and deliberate spacing | A permanent DOT / HOME toolbar, plus/ellipsis controls, vertically scrolling board and three fixed pages | Critical. Rebuild the home composition around a real paged workspace; remove the utility-app toolbar from normal Home. Exact margins and dock/search geometry need reference capture. |
| Item placement | Nothing support explicitly describes dragging apps from the drawer and widgets from the widget menu onto Home | `GridPlanner.place()` recalculates first-fit packing from item sizes; storage contains page/span but no cell coordinates | Critical. Persist cellX/cellY/spanX/spanY/pageId. Preserve empty cells and existing placement. Drag/drop must place into a selected location. |
| Moving apps/widgets | Direct manipulation from long press to placement | A Move menu instructs users to tap another item to swap; the drag listener has no complete drag interaction to drive it | Critical. Implement pickup, drag shadow, target preview, collision handling, edge paging, folder hover and cancelled-drop rollback. |
| Home page movement | Continuous direct manipulation is the interaction target to capture | Horizontal release changes `page` and calls `render()`; there is no visible progress-linked page translation | Critical. Pages should track the finger, then settle according to velocity/progress, including cancellation. |
| Launcher motion | Nothing's 4.0 announcement describes refined interaction physics and motion | No custom property-animation or animator implementation. Dialog/platform defaults do not animate workspace transitions | Critical. Define explicit Home/drawer/overview/picker/folder states with reversible transitions. Measure the reference before claiming matching timing. |
| Blank-space menu | Official support describes long press → Widgets; customisation and Home settings are part of the reference flow | Generic AlertDialog with Add app / built-in widget / Android widget / folder / settings | High. Replace the generic action list with a deliberate Home editing surface; validate presentation against the chosen original build. |
| Widget selection | Original workflow exposes widgets and supports dragging a selected widget onto Home | Built-ins and provider widgets are separate text-only lists, without previews or a placement stage | Critical. One visual picker, provider grouping, preview cards, size/style selection and drag-to-place. Search, tabs and bottom-sheet geometry are proposed UI choices until confirmed against the reference. |
| Widget design | Official Nothing Widgets lists multiple families and swipeable content in supported widgets | Most built-ins are the same title/value rounded card. Only the clock, photo and media controls have substantially different rendering | Critical. Give each supported widget its own renderer, responsive sizes, styles, states and interactions. A list of widget names is not a widget-design library. |
| Clock family | Original offers clocks; specific faces depend on the chosen version | One hand-drawn dot clock, with no face picker | High. Add measured reference faces and previews, with locale/time-format handling. Do not substitute a lock-screen clock for a Home widget. |
| Media tile | A reference widget must be captured as a complete visual/control interaction | Current code displays a title and text-based transport controls, without artwork; visible refresh is primarily periodic | High. Artwork, artist text, clear controls, playback callbacks and supported size variants. Match original motion only after reference capture. |
| Quick controls | Nothing describes individual Quick Settings widgets | One card opens a text menu for multiple settings/actions | High. Separate direct-use control tiles, with clear state and system-settings fallbacks where required. |
| Photo tile | A photo widget is part of the original family | Image picker and one center-cropped rectangular display | Medium/high. Crop/position editing, shape variants and proper previews; confirm which variants exist on the reference. |
| Icons | Nothing publishes a purpose-designed monochrome pack | ColorMatrix saturation=0 removes colour from fallback icons; that does not recreate monochrome glyph artwork | High. Use legitimate icon-pack resources or adaptive monochrome layers, with consistent masks, optical sizing and legible fallback. |
| Max Icons | Official launcher description explicitly advertises Max Icons | Span changes plus a hard-coded 78dp image for enlarged items | High. Match occupied cells, optical scale, label treatment, resize interaction and movement around neighbouring items. |
| Folders | Nothing documents Max Folders; its release material also describes layouts and illustrated covers | One four-app preview arrangement; opening the folder uses an AlertDialog; no cover library | High. Implement small/enlarged variants, covers, target regions, overflow, drag-in/out and opening/closing tied to the folder's position. |
| Drawer/search | Nothing's Smart Drawer and search filtering are documented features | A full-screen Dialog containing a ScrollView/GridLayout, substring search and category selector | High. Integrated drawer motion, proper scrolling, keyboard/search state and an independently specified local organisation system. Android metadata categories are not Nothing's Smart Drawer. |
| Customisation | Nothing documents grid/label/widget/theme choices and wallpaper/style entry points | One long text settings list; no live layout preview or dedicated customisation pages | High. Dedicated settings surfaces with preview and per-feature controls. |
| Notification dots | Must be confirmed on the frozen reference; also a launcher compatibility requirement | Notification listener is present for media, but Home icon rendering has no notification-dot implementation | Medium. If included, derive dots from allowed active notifications, support revocation and avoid inventing unread counts. |

Primary reference sources: [Nothing Launcher](https://play.google.com/store/apps/details?id=com.nothing.launcher), [Nothing's app dragging and layout-freeze instructions](https://support.nothing.tech/hc/en-us/articles/32975152760081-An-app-icon-has-disappeared-from-the-home-screen-What-can-I-do), [Nothing's widget entry and drag flow](https://support.nothing.tech/hc/en-us/articles/32978588562833-How-do-I-add-the-Nothing-Ear-widget-to-my-home-screen), [Nothing Widgets](https://play.google.com/store/apps/details?id=com.nothing.hearthstone), [Nothing Icon Pack](https://play.google.com/store/apps/details?id=com.nothing.icon), [Nothing's customisation and swipeable-widget description](https://whatsnew-phone1.nothing.tech/), [Nothing OS 3.0 / Smart Drawer](https://nothing.community/d/22988-nothing-os-30-general-release), [Nothing OS 4.0](https://nothing.community/d/47265-nothing-os-40-general-release).

The OS 2.0 material establishes historical features, not exact OS 4.1 screen layouts. The 4.1 announcement explicitly notes differences between devices, so one model/build and its launcher/widget app versions must be frozen. [Nothing OS 4.1 rollout](https://nothing.community/d/56170-nothing-os-41-rolling-out-now).

2. **Why adding a few animations will not fix this**

`render()` recreates the root view and calls setContentView after many actions. `GridPlanner` stores no deliberate empty-space placement. Dialog windows own the drawer and folder rather than a shared launcher transition system. These choices leave no stable scene graph, gesture-progress state or item-coordinate model on which to build faithful movement.

The required correction is architectural: stable workspace views, a persistent placement model, explicit UI states and direct manipulation. Decorative fades on the current generic menus would leave the biggest mismatches intact.

The original Launcher3/Lawnchair foundation should be reconsidered through a build-and-device trial. It already addresses substantial launcher mechanics. Any candidate must be pinned, audited for the offline requirement, stripped of online integrations and tested on the actual HyperOS build before being selected. Preserve the app's package/signing identity and existing user data regardless of the engine choice.

3. **Replacement widget-picker specification**

Proposed flow: long press Home → Widgets → inspect real preview → choose supported style/size → drag or place on Home → configure when necessary.

- Bring built-in and Android provider widgets into one discovery surface, while preserving their different configuration requirements.
- Display family/provider names, preview imagery, size footprints and availability. Do not load previews by creating persistent live widget bindings.
- Provide a style/size gallery for our widgets, rather than a generic Resize list that lets a complex card become an unusable 1×1 box.
- Show a placement outline and invalid-target feedback. Commit the widget only after an accepted drop and required configuration.
- Cancelled binding/configuration must release allocated IDs and restore the previous workspace state.
- Keep scrollable widget gestures distinct from workspace paging and picker dismissal.
- Keep one shared widget definition for preview and final rendering so the selected preview matches the placed result.
- Preview data must be labelled sample data. Do not display invented live device/calendar/media data as real information.

This specifies the replacement we need; its exact presentation must be compared with a reference capture before being called an exact reproduction.

4. **Motion acceptance requirements**

| Transition | Required behaviour |
|---|---|
| Home ↔ drawer | Follow gesture progress; settle or cancel; coherent icon/background movement; correct Back and keyboard handling |
| Home page swipe | Visible translation while moving, velocity-based settle and interruption support |
| Home ↔ edit overview | Continuous scale/translation, page selection, stable return position and no layout recreation flash |
| Picker open/close | A controlled transition over the workspace, preserving scroll/selection state and correct Back ordering |
| Folder open/close | Animate from/to the actual folder position; preserve input focus and app tap targets |
| Item pickup/drop | Pickup feedback, shadow, target preview, accepted/cancelled settle and one persistent model transaction |
| Resize | Visible handles, supported-size snapping, collision validation and cancellation |
| In-widget interactions | Appropriate motion only while visible; event-driven content updates and reduced-motion support |

No original durations, damping constants or blur strengths have been measured. They must be recorded from reference material; do not invent exact-match values. Home-to-app and app-to-Home system surface animation is a separate HyperOS integration issue.

5. **Keep actual platform limits separate from implementation debt**

We can implement a visual picker, proper Home layout, direct dragging, folder transitions, drawer motion, widget artwork and local customisation inside an ordinary offline launcher. Their absence is not caused by HyperOS restrictions.

Native Recents, keyguard/AOD, global app-return transitions and system gesture integration remain firmware-dependent. Glyph hardware is absent. Those limits must be explained without using them to excuse ordinary launcher UI gaps.

Weather, city search, online AI/search, cloud galleries, remote widgets and telemetry remain intentionally excluded. The redesign must preserve the offline source/APK checks and must not reintroduce an Internet permission. A proprietary cloud-backed feature is not something to silently replace with a network service.

6. **Rebuild order and acceptance gate**

1. Freeze a real reference set: clean Home, edit mode, widget library, widget style chooser, drawer, folder, icon menu and customisation. Record model/build/app versions and logical display scale. Include screen recordings for the transitions above.
2. Select and prove the workspace engine. Migrate schema 1 packed items to explicit cell coordinates once, keeping backups and restoring previous state on failure. Test engine and layout behaviour on the POCO.
3. Implement Home composition, persistent paging, direct dragging and edit mode. The layout must preserve intentional empty cells after restart.
4. Implement the visual widget picker and renderer variants. Start with clock, date, battery, media, photo and individual controls, using useful local data.
5. Implement coherent drawer, folder and menu transitions, then dedicated customisation surfaces. Keep placement, gestures and motion consistent across all these states.
6. Review still frames and recordings against the frozen reference. A release needs visual and interaction evidence, migration tests, offline checks and a real-device pass. Compilation success alone is not the acceptance gate.

The next substantial version should be treated as a workspace/UI rebuild. The current release should remain described as a prototype until those acceptance criteria are met. This audit does not claim that a replacement build has already been implemented.
