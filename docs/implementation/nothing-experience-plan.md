# Nothing experience implementation plan

Created: 26 October 2026. Working baseline: released Dot Home 4.0.0 / code 6, tested source `b94b5e89a3a8830103a1a3a82cac7bc355d08c37`; publication commit `447f143365c5489a133917de2491a2e7899c1b1f`.

This is the execution checklist for the user's request to implement the [74-item research audit](../reference/v4-nothing-comparison-2026-10-26.md). The adjacent [audit tracker](nothing-audit-tracker.csv) records every original finding, its milestone, implementation state, evidence and remaining limits. Update both as work proceeds. App implementation has not started at creation of this plan.

## Product outcome

Deliver a coherent Nothing-inspired launcher experience on the existing native launcher foundation: Home, icons, colours, wallpapers, menus, customisation, drawer, organisation, widgets and launcher-owned motion. Preserve the user's working layout and familiar Android navigation.

Support complete **Classic** and **translucent** appearances, each light/dark, with a distinct Dynamic colour option. Reference Nothing OS 5 on Phone (3) for the newer direction and the supplied Classic references for the older direction. Label beta/marketing evidence accurately. A working related feature is not automatically a faithful implementation.

## Non-negotiable invariants

- Keep `dev.dothome`, `dev.dothome.HomeActivity`, the existing signing identity and an update-compatible version code.
- Preserve existing v3/v4 layouts, folder contents, widget IDs/state, user choices and the legacy recovery copy. Optional presets never silently replace an existing Home.
- No Internet permission, HTTP library, online option, updater, online feed, runtime font download or delegated cloud feature.
- No Accessibility service, global navigation overlay, root dependency or secure-settings gesture workaround. Use the system Home role and native OS navigation.
- Keep Launcher3/Lawnchair's model, native placement/reorder, folders and external-widget hosting. Modify the presentation and missing behaviours around these foundations.
- Use real local data. Preview fixtures stay in previews. Never invent weather, step counts or headphone battery values.
- Export only public build artifacts and synthetic test evidence to GitHub. The private release key remains local.

## Evidence and completion rules

Each task progresses through **planned → implementing → implemented → verified**. Verification records its type: model/API test, Android instrumentation, screenshot review or physical-device/user check. `implemented` alone is not completion. A firmware boundary is recorded as a constraint, not as an implemented feature.

Functional and visual gates are separate. Compare actual app captures against a named reference state. Do not use generated mockups as acceptance evidence, count providers as feature completeness, or invent a parity percentage. Physical POCO frame timing, unsupported OEM profiles and subjective user approval remain explicitly unverified until observed.

## Milestone 0 — protect the baseline and establish reference states

Audit IDs: A09, A10, A11, A15, A17, A37, A43, A71, A72, A74.

- [ ] Save the audit, tracker and reference provenance in the repository.
- [ ] Preserve the v4 release-source screenshots and known-good CI checks as regression evidence.
- [ ] Record visual target states: Classic light/dark and translucent light/dark Home; theme/palette screen; alphabetical/category/hidden drawers; compact Home/app/widget menus; normal/expanded/covered folders; widget family tray/editors; selected/group-drag states.
- [ ] Define a consistent synthetic layout and app set for repeated screenshots. Exclude stock status/navigation bars from launcher styling comparisons.
- [ ] Keep tests for Home role, swipe-up drawer, native reorder, widgets, upgrade/migration and restore.

Gate: baseline provenance and all 74 findings are traceable. No user layout is reset to improve a screenshot.

## Milestone 1 — one design system and appearance model

Audit IDs: A02–A08, A14, A23, A24, A63.

Implementation targets: a shared resolved appearance model and reusable UI components; update `DotSurface`, widget/theme adapters, folders, Search, settings and engine icon colours to consume it.

- [ ] Resolve material, light/dark mode, wallpaper/manual palette source and per-item override in one place.
- [ ] Define surface/background/foreground/secondary/outline/accent/active-control roles. Coordinate icon, folder, search and widget colours.
- [ ] Implement visibly distinct Classic, translucent and Dynamic material recipes with contrast-safe text.
- [ ] Derive wallpaper palettes locally and invalidate on wallpaper changes. Use a safe fallback for inaccessible/live wallpaper imagery.
- [ ] Give sampled blur a correct local source/cache/coordinate lifecycle; do not capture other applications.
- [ ] Separate brightness mode from wallpaper selection; respect existing wallpaper choices.
- [ ] Define typography roles, circle/pill/card geometry, optical sizing, padding, press feedback and reduced-motion behaviour.
- [ ] Use controlled local display fonts with provenance and licences; calibrate custom digit glyphs and baselines.

Gate: changing wallpaper/material/palette produces one coordinated Home, folders, Search and settings without stale colours; light/dark and busy-wallpaper text stays readable. Record actual screenshots.

## Milestone 2 — primary icons, Home and wallpapers

Audit IDs: A01, A12, A13, A16, A18–A20.

Implementation targets: build-time primary vector artwork, retained component mapping, workspace defaults/presets and local wallpaper/crop/effect code.

- [ ] Draw a coherent primary filled/geometric icon family for the dock and common apps; retain extensive existing mappings for coverage.
- [ ] Specify normal/Max glyph size, work badges, dots, optical centre and fallback behaviour. Avoid blank glyphs and doubled circular framing.
- [ ] Preserve installed icon packs and Home-only/themed-drawer choices.
- [ ] Calibrate dock/Search/widget/control proportions and multi-page indicator placement.
- [ ] Offer optional curated Classic/translucent layouts on a new page or during fresh setup. Preserve the installed user's current layout.
- [ ] Replace weak default artwork with deliberate local optical/textured compositions in both palettes.
- [ ] Provide local-image crop and actual Glass/Atmosphere-style Home effects with preview/apply/cancel. A named static preset is not an effect.
- [ ] Handle an external wallpaper change, a live wallpaper, revoked photo links and image size limits without stale or mismatched backdrops.

Gate: the primary Home looks coherent before adding more widget families; normal/Max icons remain recognizable; effects and preset cancellation preserve existing choices.

## Milestone 3 — menus and customisation as one product

Audit IDs: A21, A22, A25–A27, A45, A66.

Implementation targets: shared menu/sheet controls, `DotPreferences`, folder chooser and widget-specific editors.

- [ ] Apply one visual language to Home popup, app/widget actions, settings rows, switches, sheets and visual choice cards.
- [ ] Implement a page-aware current Home preview; preview palette/layout choices before committing where needed.
- [ ] Use coordinated palette choices and selected-state feedback, with a clear wallpaper/manual source.
- [ ] Make grid/label/dock/Search settings discoverable and show useful previews.
- [ ] Replace the stock folder-style text dialog with cover/layout previews.
- [ ] Use compact family-specific editors rather than a fixed generic 78%-height sheet.
- [ ] Preserve back-stack/scroll/focus; provide consistent dismiss, commit and cancellation. Support predictive-back progress/cancellation on owned screens where available.
- [ ] Remove implementation jargon from normal product choices.

Gate: inspect every route, its selected/pressed/disabled states and back/cancel behaviour. A theme action must not unexpectedly change wallpaper or layout.

## Milestone 4 — drawer, search, folders and arrangement

Audit IDs: A28–A34, A36, A38–A42.

Implementation targets: native drawer adapter/presentation integration; local categories/search configuration; integrated hidden surface; folder previews; arrangement and undo.

- [ ] Refine alphabetical grid density, labels, Search, fast scrolling and shortcut popups.
- [ ] Implement category cards with prominent app targets, overflow, deterministic offline categorisation and local manual correction.
- [ ] Keep work/private/cloned profiles separate; display real platform-provided profiles/badges and respect lock/quiet state.
- [ ] Make Hidden icons an integrated, continuously interactive/reversible drawer surface with consistent editor/search.
- [ ] Expose and explain hidden-in-search policy; keep hiding distinct from system Private Space security.
- [ ] Provide coherent offline search scopes for supported apps/shortcuts/settings/calculator and explicitly selected local data. Add no web/AI scope.
- [ ] Complete normal/expanded/covered folder layouts, direct child taps, labels and 9/10+ item overflow.
- [ ] Replace the generic group drag rectangle with artwork/count feedback and prospective placement outlines.
- [ ] Improve group collision feedback/reflow with a specified policy; preserve atomic writes and relative placement.
- [ ] Add local undo for relevant move/remove operations and distinguish Home removal from uninstall.

Gate: use a large synthetic app set with long/duplicate labels, category corrections and profile fixtures. Verify hidden-policy behaviour, populated folders, cross-page group movement, collisions, cancel and undo.

## Milestone 5 — widget catalogue, state and family layouts

Audit IDs: A44, A46–A61.

Implementation targets: stable native providers, a family-first local tray, real-data adapters, family layouts/actions/editors and supported gesture handling.

| Family | Required implementation and review |
| --- | --- |
| Clock/date | Calibrated analogue/dial/serif/matrix/segment faces, 12/24-hour and responsive supported spans, correct minute/second rollover and tap actions. |
| Quick Look | Real next-event layout with proper multiline text, calendar selection/access states and useful actions. |
| Weather | Explicit manual/local data and freshness/source semantics; optionally host a separately installed provider normally. Never add a weather fetcher. |
| Phone battery | Real charge state, supported compact/large variants and coordinated active/charging artwork. |
| Headphone battery | Display only real public/provider-supported device data, with unsupported/disconnected states; no guessed per-ear/case data. |
| Photos/Collector | Multi-image local collection, crop, circle/rectangle/sticker, local cutout/outline handling, cycle choices and unlock rotation. Preserve imported single-image widgets. |
| Away Time | Correct screen-off-history semantics, compact/large layouts, weekly detail view, totals/longest-break summaries and access/partial-history states. Tapping after permission opens details. |
| Breathing | Focus/Relax/Calm guided sessions with deliberate pacing, start/pause/reset, optional tuned haptics and visibility-aware frame-driven animation. |
| Screen time | Real usage summary and useful per-app/detail view after access, appropriate compact spans and honest unavailable state. |
| Countdown/timer | Separate date countdown from elapsed timer and ringing countdown. Correct time/date changes, start/pause/reset, foreground state and explicit system Clock hand-off where appropriate. |
| Media | Coordinated artwork/transport controls, real supported actions, no-session/denied states and metadata/artwork change handling. |
| Compass | Real heading, unavailable/accuracy/calibration states and supported lifecycle. |
| Quick controls | Real torch state; public supported Bluetooth/sound/DND integrations and clear system hand-off where direct control is restricted. Ask only for relevant optional access. |
| Pedometer | Real local sensor/provider steps, daily goal/progress, reboot/day rollover, optional activity access and an honest availability/lifecycle contract. |
| Widget gestures | Implement content swipes where supported without stealing Home paging or drawer gestures. |

- [ ] Group local widgets by family with accurate size/variant previews; keep installed providers available.
- [ ] Reuse shared rendering/state between tray previews, editing and live tiles while keeping fixture data out of live states.
- [ ] Preserve native external bind/configure/cancel/rebind flows. Do not try to repaint another app's widget content.
- [ ] Give each supported span a deliberate layout instead of stretching one generic composition.
- [ ] Test real actions, permission revocation, unavailable data, process recreation and reboot; persist per-instance state transactionally.

Gate: review each supported family/size in Classic/translucent and light/dark. Verify function separately from artwork. Claims about a family require more than provider registration or a static preview.

## Milestone 6 — motion, accessibility and durability

Audit IDs: A64, A65, A70, A73; validate A63/A66 across the finished surfaces.

- [ ] Use frame-driven launcher-owned animations with consistent gesture tracking, velocity completion and cancellation.
- [ ] Pause widget timers/sensors/haptics/animations appropriately when offscreen or Home loses activity; bind static content even when not visible.
- [ ] Verify reduced motion, touch targets, text scaling, contrast, RTL and useful screen-reader labels. No Accessibility service is needed for these semantics.
- [ ] Extend local backup to a documented, versioned appearance/layout/settings contract with optional owned image content and reconnect handling. Never assume transferable external URI grants/widget IDs.
- [ ] Verify v3 and v4 upgrade, legacy recovery, existing widget instances, process restart, backup cancel/restore and invalid backups.
- [ ] Collect actual 60/120Hz POCO frame timing and interaction evidence when the physical device is available. Emulator performance is not a substitute.

Gate: no regressions in launcher-role/navigation/layout persistence; no unnecessary background work; clear restoration contract and honest hardware-validation status.

## Milestone 7 — release and accountable evidence

- [ ] Update the tracker with implementation commits, checks, actual captures and unresolved constraints.
- [ ] Run model/format/privacy checks and Android build; add meaningful instrumentation for new stateful behaviour.
- [ ] Inspect actual Classic/translucent light/dark Home, menus, drawer/category/hidden views, folder/editing and representative widget states.
- [ ] Keep physical-device/user review items pending until actually observed; do not relabel them as passed CI.
- [ ] Increment app version/code; retain original certificate and package/component identity.
- [ ] Push source to a reviewable branch and monitor GitHub Actions; fix build/test failures.
- [ ] Sign only the validated artifact locally. Publish only the signed public APK/checksum/provenance, never signing material or private device files.
- [ ] Promote validated source, publish stable/latest release with versioned APK and checksum, and verify the public download and Obtainium-visible latest release.
- [ ] Monitor publishing and post-push Actions to completion. Final notes describe implemented features and actual remaining limits without asserting pixel-perfect firmware parity.

## Explicit scope boundaries

| Audit IDs | Decision |
| --- | --- |
| A35 | Essential web/AI search remains absent under the absolute offline brief. |
| A62 | Nothing service-backed shared widgets are excluded; standard external-widget hosting remains supported. |
| A67 | Native global navigation/Recents belong to the OS. No overlay/Accessibility replacement. |
| A68 | System lock screen/AOD/Quick Settings/status bar are not replaced by this Home APK. |
| A69 | POCO lacks Glyph hardware. A front-screen timer may reproduce an interaction, never the hardware feature. |
| A36/A50/A65 | Implement documented support/fallbacks; require actual OEM/device evidence for compatibility and performance claims. |

## Working log

| Date | State | Evidence / next dependency |
| --- | --- | --- |
| 2026-10-26 | Plan created before app implementation | All 74 findings assigned to a milestone. |
| 2026-10-26 | First implementation batch in progress | Shared appearance/primary glyphs, settings/palettes/previews, history/breathing/collections, category/hidden drawer, folder chooser, group feedback/undo and local data widgets. Local compiler caught a settings radius type mismatch; corrected. Full build/runtime/visual checks pending. No release changed. |

### Second implementation checkpoint

The first v5 source checkpoint `06b8f88` passed Android 16 build and baseline UI/migration CI (`40365860876`). Actual captures were inspected: primary glyphs and serif/date typography render, and the family tray and theme route are usable. That review exposed a left-aligned single Home preview and touching tray cards; both are corrected in the next batch. These checks do not establish visual parity or physical POCO performance.

The second batch adds draft-safe family editors, collection remove/reorder/swipe, sensor rollover state tests, supported custom media transport, backup media staging/error rollback, grid choice previews, predictive-back progress/cancel, profile-aware category icons and expanded v5/native-v4 upgrade instrumentation. Saved wallpaper sources restore to an explicit crop/apply route, keeping the system wallpaper selected. Original optical wallpaper variants and shared popup tokens complete the appearance surfaces. Full latest-source CI remains required.
