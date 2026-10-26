# Dot Home 4.0.0 compared with Nothing Launcher

Research and source audit: 26 October 2026. Baseline APK: released **4.0.0, version code 6**, source `b94b5e89a3a8830103a1a3a82cac7bc355d08c37`. This document assesses the released foundation, not an imagined next version.

**Finding: v4 is a usable launcher foundation, but its visual identity and several feature behaviours remain substantially different from Nothing Launcher.** The strongest work is the native workspace, drag/reorder, widget hosting, Home role and installation continuity. The weakest work is the consistency of icons, colour/materials, widget families, configuration flows and motion. Keeping the engine while redesigning these surfaces is preferable to discarding the engine again.

## Evidence and limits

I reopened Nothing's current OS pages, release announcements, support instructions, official Play listings and design explanation. I downloaded the official Home, colour and material illustrations and the batch-editing, Away Time and Collector demonstrations. I inspected their frames, the actual v4 CI screenshots, and the implementation behind the relevant screens.

Our screenshots come from the release-source Android 16 emulator evidence, run [40290916516](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40290916516). The subsequent main build [40317409109](https://github.com/Nrajesh0/Launcher-by-rj/actions/runs/40317409109) also passed. These tests establish specific functional behaviours. They do not establish Nothing visual fidelity, physical POCO performance, or the subjective feel of a real Nothing device.

Evidence labels used here:

- **Observed:** directly visible in actual screenshots, supplied references or downloaded demonstrations.
- **Source-confirmed:** follows from the released implementation or primary documentation.
- **Partial:** a working related capability exists, but design, behaviour or scope differs.
- **Unverified:** requires a device, a matching reference state or motion/performance capture.
- **Constraint:** a deliberate offline requirement, an Android API restriction, or a firmware-owned surface.

There is no defensible overall percentage for "Nothing parity." We do not have a physical Nothing device and a controlled, identical set of apps and states to measure one. A date tile passing an existence check is not evidence that the whole widget experience matches.

## 1. Choose the reference accurately

The supplied images mix classic monochrome Home screens, newer translucent Home screens, lock screens, Quick Settings and promotional collages. One supplied collage also contains interface designs that cannot establish Nothing Launcher behaviour. A thumbnail headed "OS 5.0" is not sufficient proof of a shipped feature.

The current primary benchmark is **Nothing OS 5.0 on Phone (3)**. The official [general-release announcement](https://nothing.community/d/63548-nothing-os-50-general-release-is-here) confirms that the redesigned launcher is included. Earlier OS 2–4 references remain useful for the Classic theme and longstanding launcher interactions. The original public Android-wide Launcher beta is not an adequate benchmark for the modern experience; the current [official launcher listing](https://play.google.com/store/apps/details?id=com.nothing.launcher) is for supported Nothing OS devices.

Two complete visual modes are required by the user's existing brief: **Classic and the newer translucent direction**, each in light and dark. Nothing's photographed theme selector uses **Lucent, Classic and Dynamic**; our v4 selector says **Translucent, Classic and Dynamic**. The precise selector photograph is a beta reference, not proof that every general-release label and control remains identical.

Nothing's [design explanation](https://nothing.community/d/61774) describes materials, typography and a colour engine as a system. It distinguishes the newer wallpaper-responsive direction from Classic and Android Dynamic Colours. The important implication for our design is that three labels should produce distinct, coordinated rules across every owned surface.

There is also a release-versus-marketing caveat. The official material describes wallpaper-tinted icons, while the [September hands-on](https://www.91mobiles.com/reviews/nothing-os-5-hands-on/) explicitly covers a beta and [general-release users report incomplete icon treatment](https://nothing.community/d/63548-nothing-os-50-general-release-is-here?page=2). We should not promise universally transparent icons or a translucent drawer as verified stock behaviour. Those could be improvements, clearly identified as such.

## 2. Why the current Home still reads as generic

The actual v4 light/dark screenshots show a deliberate attempt at the same ingredients: circular clock, large date card, small circular controls, battery pill, Focus tile, four dock icons and a bottom Search pill. That is meaningful progress. However, similarity of ingredients is weaker than similarity of the finished composition.

| Detail | V4 evidence | Consequence / correction |
| --- | --- | --- |
| App glyphs | Phone, Messages, Camera and most other mapped icons are Lawnicons outlines | The most frequently seen elements have a different visual weight. Introduce a coherent filled/geometric primary set and preserve readable fallbacks. |
| Relative sizes | Large widgets, smaller dock discs, inset control discs | Their optical sizes and margins are not calibrated together. Measure outer cell, visible surface and glyph separately. |
| Surfaces | Classic is mostly the same opaque off-white or charcoal colour everywhere | There is insufficient differentiation between content cards, controls, search and configuration surfaces. |
| Composition | The seeded screen has a very large empty middle area | Whitespace is intentional in the references, but ours comes from a fixed seed rather than a polished set of complete arrangements. Offer optional curated starting layouts. |
| Wallpaper | Soft gradients, grain and procedural arcs | Similar palette, weaker optical structure and focal detail. Rework actual bundled artwork and crop behaviour. |
| Search | A large magnifier and centered Search text on the inherited dock field | The placement is recognizable, but proportions, baseline, tint and transition need calibration. |
| Type | Geist labels coexist with system serif, generic monospace and custom dot/segment drawing | Different renderers do not yet form a controlled hierarchy. Specify roles and metrics for every family. |
| Page indications | Native engine behaviour exists, but the one-page screenshot does not demonstrate the multi-page reference | Verify multi-page indicators, active state, spacing and adding/removing a page. Do not call this missing from a single-page image. |

These are launcher-owned gaps. The POCO status bar in our screenshots is not evidence of failed app icon design; status-bar artwork belongs to the OS. It must be excluded from direct visual acceptance masks.

The seed layout must never overwrite an existing layout. A closer demonstration Home should be an explicit optional preset on first setup or a new empty page, with installed default apps resolved locally and unsupported items omitted honestly.

## 3. Icons: the biggest immediately visible difference

The [Nothing Icon Pack listing](https://play.google.com/store/apps/details?id=com.nothing.icon) describes a unified monochrome set intended to work with its widgets. Looking at the user's references, the recurring primary icons are compact, comparatively solid silhouettes. Looking at our actual Home and drawer, our mapped icons have rounded outline strokes and more internal empty space.

Our component-to-icon mapping is substantial and useful. Its coverage does not make its artwork a Nothing-style set. Ten thousand icons from a different family can still produce a weaker match than twenty carefully coordinated primary icons.

The next pass should establish:

1. A primary set for Phone, Messages, Camera, Browser, Gallery, Settings, Contacts, Clock, Files and major locally installed apps.
2. One optical size rule, consistent fill/stroke treatment, baseline and surrounding disc.
3. Separate handling for an installed icon pack, an app-supplied monochrome layer and a fallback full-colour icon. Avoid stacking an already circular icon inside another unnecessary disc.
4. A recognizable fallback for unsupported apps. Indiscriminate grayscale is not sufficient, and a blank monochrome silhouette must never be accepted.
5. Predictable light/dark inversion and tint rules, work-profile badges, notification dots and selected/pressed states.
6. Correct normal/Max icon scaling without blurry upscaling or clipping.
7. A Home-only versus Home-and-drawer switch whose effect is visible in the preview.

V4 supports normal/Max app spans, installed packs and themed icons. Their exact rendering and transitions remain partial matches. Nothing's proprietary artwork is not bundled; an independently drawn compatible style or a user-installed supported pack is the implementation route.

## 4. Colours and materials: more than an accent picker

Our current appearance screen offers six single-colour swatches. The inspected theme photograph uses multi-colour palette choices. More fundamentally, v4 has several independent colour paths:

- `DotPreferences` stores a manual `dot-style.accent` and sometimes sets the engine to `WallpaperPrimary`.
- `DotSurface.paper()` mixes a fixed base with that saved accent, using one fixed translucency alpha.
- Themed icons read the engine's colour tokens.
- `TileArtwork` has additional hard-coded tone colours and a hard-coded active flashlight green.
- Folder covers draw another independently specified surface.

Therefore a wallpaper-derived engine palette does not guarantee matching widget and folder colours. This is source-confirmed fragmentation, not merely a subjective dislike of the chosen green.

Nothing's [official colour illustration](https://cdn.nothing.community/2026-08-24/1787594344-523374-new-colour-engine-1.png) is useful evidence of distinct content and surface roles. Our proposed correction is a single resolved appearance state containing material, brightness mode, palette source, surface shades, text roles, control states, icon roles and user overrides. Every owned renderer should consume that state.

For the translucent mode, v4 does perform real local wallpaper sampling and blur. It is not just an alpha-only placeholder. But the sampling uses our own saved/generated wallpaper, a small blurred bitmap and view screen coordinates. It does not provide live wallpaper capture. The source also draws the dock Search as an alpha-colour background without the same sampled backdrop treatment as local tiles. A wallpaper change outside our picker can leave the sampled source inconsistent. These cases require deliberate invalidation and an honest fallback.

Use separate material recipes for a solid card, frosted card, control disc, active control, folder and Search. Preserve contrast on bright, dark and visually busy wallpapers. Avoid making every widget identically transparent. The [official material illustration](https://cdn.nothing.community/2026-08-24/1787594325-318797-deck-12.png) distinguishes transparent, translucent and opaque roles; its diagram is not a ready-made alpha specification for our app.

Our settings paper colour and spacing are not yet coordinated with Home materials. Dark mode currently also changes the fallback wallpaper when a real wallpaper has not been applied. Theme brightness and wallpaper selection should be separate user decisions.

## 5. Typography, shapes and visual hierarchy

Using Geist in v4 was directionally correct for the newer Nothing reference. The [official OS 5 page](https://nothing.tech/nothing-os) identifies Geist among the current design changes. However, choosing that font does not finish typography.

Our widgets use a mixture of Geist Medium, Android generic monospace, Android generic serif, a hand-coded dot alphabet and seven-segment digits. The date and serif clock depend on the device's generic serif rather than a controlled bundled display face. That can vary by firmware. The widget sheet and launcher popup also inherit different sizing and component rules.

Define body, secondary, metadata, display-serif, matrix and segmented roles, including baseline, line height, numeral width and punctuation. Use matrix type where the reference calls for it, rather than turning ordinary menu copy into decorative dots. Do not assume a seven-segment face is the Gooey or Micrographics lock-screen face.

Define circle, pill and rounded rectangle dimensions relative to cell size, with family-specific inner padding. Widget bounds, rendered surface, drag outline, resize handles and tappable area must agree. Visible artwork can be smaller than a target, but invisible padding must not make placement feel inconsistent.

## 6. Customisation and menus

The actual v4 Customisation screenshot is recognizable: a real Home preview followed by Theme & Style, Wallpaper, Home screen layout and Icon pack. These are implemented. The screen is not empty or completely unrelated to the reference.

Its limitations are nevertheless substantial:

- The preview shows only one miniature Home view. It is not a browsable representation of all pages or separate Home/lock views.
- Rebuilding the whole settings view for each choice is not the same as a polished live preview interaction.
- Every route uses the same fairly large card rows, even when a compact choice group or inline treatment would communicate better.
- The back control is a text glyph. Visual consistency, press states and navigation motion need attention.
- Theme choices, widget colours and folder styles each use separate UI conventions.
- Grid options are four text rows: 4×6, 4×7, 5×6, 5×7. They lack a placement-aware interactive grid preview.
- The core can support many search/dock behaviours, but the replacement settings expose only a subset. Search visibility/behaviour and some layout preferences are not presented as a complete product flow.
- Some setting labels explain implementation, such as local loading, rather than helping users choose an appearance.

The Home long-press popup is compact and has four correct functional routes. Its segmented row styling, inherited icons, entry/exit animation and relationship to the pressed location still need a visual pass. App shortcut popups, widget actions and folder configuration need to follow the same design language. Folder style currently opens a stock `AlertDialog` list, not a visual chooser.

A coherent route structure should put appearance, wallpaper, layout and icon selection under Customisation; drawer/search behaviour, layout freeze, default Home and backup under Home settings. Place controls where the user expects them and avoid presenting the same choice differently on several screens.

## 7. Drawer, search and hidden apps

### Alphabetical drawer

Swipe-up opening is functional on the release-source emulator. The drawer shown is a standard five-column alphabetical grid with large labels and an inherited top search field. Icon artwork, search dimensions, scrolling treatment, popup styling and transitions need a Nothing-specific pass. Many installed-app states, including long names and work apps, are not demonstrated by the small emulator app set.

### Smart Drawer

Nothing introduced category-based Smart Drawer in its [OS 3 general release](https://nothing.community/d/22988-nothing-os-30-general-release). Our "Smart categories" setting calls Lawnchair's `drawerList=false` branch, which builds native folders from its local category rules. It has genuine categorisation; it is not an empty toggle. But category folders in a standard adapter are not evidence of the same large category-card presentation, prominent child apps, ordering and interactions as Nothing's Smart Drawer.

The implementation should retain offline rules, add deterministic fallbacks and let the user correct categories locally. Manual correction is a proposed improvement, not a claim about stock Nothing behaviour. Profile separation, search results, installation/uninstallation and category changes need tests beyond just enabling the mode.

### Search

The dock correctly opens local search. The foundation also retains local search-provider machinery, including app matching and some optional non-app providers. It is inaccurate to say no local search engine exists. However, the replacement settings do not expose a finished scope-selection interface, and the manifest's restricted permission set prevents claiming complete contact/file/SMS/media search parity.

Nothing's [OS 4 announcement](https://nothing.community/d/47265-nothing-os-40-general-release) documents scope controls. Its [Essential Search listing](https://play.google.com/store/apps/details?id=com.nothing.essential.search) additionally advertises web/AI answers. For us, a coherent offline search could cover apps, available shortcuts, settings links, calculator and explicitly selected local information. Web answers and cloud search remain excluded by the user's requirement.

### Hidden icons versus Private Space

V4 has a real right-swipe Hidden icons route, matching the general gesture described in [Nothing support](https://support.nothing.tech/hc/en-us/articles/32975168713361-How-do-I-hide-app-icons-on-my-Nothing-Phone). But ours opens a separate Activity after crossing a gesture threshold; it is not a continuously interactive drawer page. Its editor is a generic checkbox list. This is a substantial feel difference.

The retained engine defaults hidden apps in search to `NEVER`; our own settings do not expose that choice. That differs from the OS 4 announcement's description of hidden apps remaining reachable through search. Decide and explain the rule; do not silently change the user's privacy expectations.

Hidden icons are cosmetic organisation, not app isolation. Actual Private Space uses a separate OS profile. The [Android private-space documentation](https://source.android.com/docs/security/features/private-space) describes Home role and hidden-profile access requirements. Our underlying core includes relevant profile machinery and declares the permission, but a complete physical HyperOS private/work/cloned-profile flow remains unverified. It is not correct to declare all such support impossible for third-party launchers.

## 8. Folders and arrangement

V4 supports real native folders, expanded Home folders, direct child taps, covers and transactional group placement. These should be retained.

The current expanded renderer draws a fixed three-by-three preview into an enlarged surface. Its selection dialog offers App previews, Dot grid, Flower, Orbit and Cross plus Enlarge/Shrink. There is no full visual cover library or emoji-cover selection flow. The current folder screenshot has two small icons in a large otherwise empty surface; it proves content survived, not polished populated-folder appearance.

Expanded preview drawing reserves a label area and draws its own title. Native label settings and cover-mode label visibility require verification together. Covered folders, overflow, exactly 9/10+ entries, dragged-in items, work badges and child launch targets need distinct acceptance cases.

The official [batch demonstration](https://cdn.nothing.community/2026-08-24/1787549689-179917-hsbatchq-ezgifcom-video-to-gif-converter.gif) shows a group represented by app artwork and a count, with outlined placement shapes and a remove target. Our source creates a rounded rectangle reading "N items" as the drag ghost. Our screenshot also shows a narrow toolbar containing Move, Style, Folder, Remove and Done. These are different interaction presentations.

Our group drop preserves offsets and rejects occupied/boundary conflicts with a toast. Native single-item reorder is richer. Group movement therefore does not inherit the full single-item collision/reorder feel. It needs useful placement preview, space feedback and a specified policy for rearranging neighbours. Preserve atomic database updates while improving the interaction.

A local undo for layout removal/movement would be a useful improvement. It is not asserted here as a stock Nothing feature. Destructive layout actions must remain distinct from uninstalling an app.

## 9. Widget-by-widget comparison

The [official Nothing Widgets listing](https://play.google.com/store/apps/details?id=com.nothing.hearthstone) identifies Clock, Weather, Quick Look, Photos and Compass, plus in-widget information swiping where supported. Merely counting providers in our tray overstates completeness: v4 registers multiple faces as separate entries, and the tray count can include inherited providers.

| Family | Released v4 | Gap / next implementation |
| --- | --- | --- |
| Minimal analogue clock | Live hands and accent dot, genuine native widget | Calibrate hand silhouette, length, angle interpolation, surface size and dot position. |
| Dial clock | Tick ring and seconds hand | Family-specific tick weights, numeral options where observed and motion calibration. |
| Dot clock | Locally drawn dot digits | Proper glyph metrics, colon/spacing, edge rendering, larger/smaller variants and 12/24-hour behaviour. |
| Serif clock | Generic Android serif | Controlled typeface, baseline and responsive numeral fitting. |
| Segmented clock | Custom seven-segment drawing | A real face, but not equivalent to every Nothing display clock. Keep names honest. |
| Date | Large number, weekday/month | More complete compact/large compositions and reference typography. |
| Quick Look | Next calendar event rendered as a limited text tile | Missing the richer contextual presentation. The source produces a newline-containing event string but the generic artwork uses single-line `drawText`; improve actual multiline layout. |
| Weather | No local built-in family | A meaningful missing signature family. Use manual data or an explicitly selected external widget; no weather fetching by this app. |
| Phone battery | Dot and ring variants, local real battery | More sizes, charging state design and optical calibration. |
| Ear/headphone battery | Not implemented | Separate left/right/case/device imagery needs actual device data or a compatible installed provider; never invent percentages. |
| Photos | A real selected image, crop, pinch/position and circle/rectangle modes | Needs gallery/album flow, clear replacement and revoke/unavailable states, and family-specific editing. |
| Sticker / Collector | Single image with fit-inside/no rectangular surface | No subject extraction, outline generation, collection model or image cycling. A transparent PNG can already appear cut out; that is not background-removal functionality. |
| Away Time | Usage-event-derived screen-off history and a seven-bar summary | No matching compact graph compositions or weekly detail view. Tapping always opens Usage Access settings. |
| Breathing / Focus | Oscillating ellipse artwork | No full guided session or three-mode experience. Tapping opens the generic style sheet. |
| Screen time | Aggregated local usage total with optional access | No rich breakdown/drill-down; tapping opens access settings even after access exists. |
| Countdown | Named date and days remaining | Generic text composition and date dialog; improve readable hierarchy, date handling and variants. |
| Elapsed timer | Start/pause/reset count-up state | Does not implement a self-contained ringing countdown or the radial Glyph Timer interaction. |
| Compass | Real rotation-vector heading | Calibration, missing-sensor/accuracy states and visual family need refinement. |
| Media | Active session title/artwork and previous/play/next | Default Android media icons, simple layout, no complete visual/interaction match or tested player matrix. |
| Flashlight | Real public CameraManager toggle, live observation | Better state transitions, unavailable state, palette-consistent active styling. |
| Bluetooth | Opens OS Bluetooth settings | A shortcut, not direct on/off or an in-launcher device controller. |
| Sound | Opens OS sound settings | Not a self-contained sound-mode control. |
| Do not disturb | Opens OS DND settings | Not the red active/inactive toggle experience in the references. |
| Legacy control panel | Four labelled buttons for restored older layouts | Useful compatibility, visually generic; preserve old data while replacing presentation. |
| Notes | Saved text | Useful local extra, not evidence of stock Notes widget parity; multiline rendering needs work. |
| Pedometer | Not implemented | Missing daily goal/progress family. Requires real sensor/provider data, appropriate optional activity access and a supported lifecycle strategy. |
| Soundscape | Not implemented | Assess separately after verifying current availability; do not promote an announcement of future availability to universal shipped support. |
| Shared widgets | Not implemented | Nothing-specific service/device integration and network expectations differ from this offline app. |

Nothing's [OS 2.5 changelog](https://nothing.community/d/3875-phone-2-nothing-os-25-changelog) documents Pedometer, Media and Screen Time. The [Phone (4a) update post](https://nothing.community/d/54240-phone-4a-updates) documents Focus, Relax and Calm with haptic guidance and distinguishes the then-upcoming Soundscape widget. Device and update applicability matter.

Our Away Time semantics should remain explicit: **screen-off events approximate time away**. They do not prove a phone was physically put down or that a person was away. Events before the query window, limited history, revoked access and OEM differences need correct empty/partial data handling.

The [official Away demonstration](https://cdn.nothing.community/2026-08-24/1787536756-511941-widgets50-ezgifcom-video-to-gif-converter-1.gif) and [Collector demonstration](https://cdn.nothing.community/2026-08-24/1787536737-225185-widgets50-ezgifcom-video-to-gif-converter-2.gif) establish variant artwork and collection-style presentation. The [community review-program report](https://nothing.community/d/62152) is additional hands-on evidence for Away details and unlock cycling; it is a beta report, not an official API contract.

## 10. Widget selection and editing

V4 uses the native provider-organized picker. This fixes basic widget-hosting and placement machinery. Its screenshot, however, is predominantly a list of Android provider rows, with Dot Home buried alphabetically and many variants under one provider. It does not give our own catalogue the prominence or family structure needed for the requested experience.

Build a local first-party section organized by family, with accurate size/variant previews, followed by installed providers. Use the same renderer and state model for preview and live tile, but distinguish preview fixtures from real user data. Keep proper Android bind/configure/cancel flows for external widgets. An external app controls its own widget content; we cannot force it into our material language.

The current per-widget sheet consumes a fixed 78% of screen height. Its title, Done, large preview, face carousel, hard-coded swatches and Cancel are shared broadly. A compact clock picker, photo collection editor, calendar-access explanation and elapsed-timer controls should not all be squeezed through that same generic treatment.

Some local widgets allow broad resizing while their renderers remain largely generic. Specify each supported span, and provide family-specific layouts at that span instead of stretching a single composition. Swiping inside a widget where supported needs explicit touch arbitration with Home's upward drawer and horizontal paging gestures. It is not implemented by merely hosting the widget.

## 11. Animations, touch and haptics

Native Launcher3/Lawnchair supplies much of the workspace, drawer, drag and folder motion. It is a strong starting point. It is not the Nothing motion specification.

| Interaction | Current evidence | Required acceptance |
| --- | --- | --- |
| Drawer pull/release | Opens under actual input in emulator | Finger tracking, velocity-dependent completion, cancellation, Search/keyboard continuity. |
| Hidden page | Threshold opens an Activity | Continuous page movement and reversible gesture, unless matching reference proves another behaviour. |
| Settings route | Whole view is replaced | Consistent navigation animation and preserved scroll/focus. |
| Theme changes | Preferences updated, screen rebuilt | No palette flash or stale widget; smooth, reduced-motion-safe transition. |
| Single-item drag | Native implementation | Reference scale/shadow, folder hover, edge paging and stable drop. |
| Group drag | Custom rectangle ghost | Artwork/count feedback, positions, collision prediction, cancel and cross-page continuity. |
| Widget breathing | `postInvalidateDelayed(40)` | A frame-driven session animation with intentional cadence/haptics, paused offscreen. |
| Analogue seconds | Periodic invalidation | Tick/sweep chosen deliberately per face; correct pause/resume. |
| App launch/return | Standard third-party activity launch | Tune what the launcher owns, measure the OS hand-off, avoid claiming system Quickstep replacement. |
| Predictive Back | Some custom callbacks exist | Interactive progress/cancel for every owned full-screen/sheet route; not just finish-on-back. |

The 40ms breathing invalidation is approximately 25 redraw opportunities per second, not a 120Hz animation. This is a concrete source-level reason that an animated tile can still look unsmooth on the POCO. It does not prove the rest of the launcher runs at 25fps.

Do not invent Nothing's spring stiffness or timing from marketing adjectives. Capture reference motion, record interaction/state transitions and tune against observable behaviour. Hardware display scheduling and global Home/Recents remain OS-dependent.

## 12. POCO and platform boundaries

The [official POCO specification](https://www.po.co/global/product/poco-x6-pro/specs/) gives 1220×2712 resolution, up to 120Hz refresh, Dimensity 8300-Ultra/Mali-G615, compass/gyroscope and an X-axis vibration motor. These support a much more polished launcher than the current visual layer. They do not supply proprietary Nothing firmware services.

At 120Hz a display interval is approximately 8.33ms; at 60Hz it is 16.67ms. These are frame intervals, not guaranteed application CPU budgets. Test the actual selected refresh mode, display scaling, font scaling, cutout and navigation insets. The emulator's software graphics performance is not a POCO benchmark.

The user's HyperOS 3 / Android 16 version is taken from their report. Exact ROM region/build, supported navigation settings and OEM behaviour still need device verification. We should not extrapolate one HyperOS behaviour to all Android devices.

| Requested experience | Feasible in a normal offline Home app? |
| --- | --- |
| Nothing-like Home, drawer, icons, folders, picker, owned widgets and menus | Yes; these are predominantly our design/implementation responsibility. |
| Own wallpaper library, crop, static glass effect, Home-side animated background | Yes, using local assets/rendering; lock-to-Home firmware animation is a separate integration. |
| Home role request, native swipe-up app drawer | Yes; no Accessibility or ADB required. |
| Global Back/Home/Recents gestures and their firmware animations | OS-owned. Do not emulate with overlays or Accessibility. |
| Native lock screen, AOD, status bar, notification shade and Quick Settings replacement | Not a normal launcher surface. Keep the stock system and scope accurately. |
| Bluetooth direct silent toggle | Restricted for modern third-party targets; a user-consent/system route is appropriate. |
| DND or brightness public integrations | Some require separate special access and correct API handling; not all controls are impossible. Do not add grants during this research task. |
| Fresh built-in weather, Essential cloud answers, online widget recommendations | Excluded by the absolute no-network brief. |
| User-installed external widget | The host can display it; that provider remains a separate app with its own permissions/network behaviour. |
| Glyph light/matrix control on POCO | Hardware absent. A front-screen timer may reproduce an interaction, not the Glyph hardware feature. |
| Work/Private Space profile display | Potentially supported through documented launcher APIs where the OS provides profiles; verify actual OEM behaviour. |

The [Android Bluetooth target behaviour](https://developer.android.com/about/versions/13/behavior-changes-13) is a concrete API limitation. It must not become a blanket excuse for poor icons, menus, widgets or layout. Most of the largest present gaps are within our control.

## 13. Quality gaps beyond the reference photos

A polished implementation also needs states the photos do not show:

- Long app titles, duplicate labels, multiple launcher activities, removed apps and profile badges.
- Empty, denied, permanently denied, revoked and unavailable-data widget states.
- Camera in use by another app; unsupported compass; no active media session; media artwork changes without a track change.
- 12/24-hour formats, time zones, daylight-saving changes, midnight rollover and reboot.
- Countdown crossing a daylight-saving transition; completed/past events; all-day and multi-line calendar events.
- Large fonts, display scaling, RTL, contrast, reduced motion, clear touch targets and readable accessibility semantics. Supporting screen-reader use does not require declaring an Accessibility service.
- Workspace lock for all relevant paths, folder overflow, drag cancel, undo, interrupted resize, process death and rotation handling.
- Wallpaper changed outside our picker, a live wallpaper, denied old document links, oversized images and colour-cache invalidation.
- Fresh install and v3/v4 upgrade, process restart after migration, local backup/restore, reinstall and external widget rebind.

Our JSON backup is useful but currently saves selected Dot preference stores and layout records, not a complete engine preference snapshot or embedded image library. Icon-pack selection, drawer preferences, fonts and external document grants therefore need an explicit restoration contract. Same-device synthetic restore success does not prove a fully portable theme backup.

External widget restoration and document grants cannot be solved by pretending every saved URI or provider ID transfers to another installation. Use user-visible reconnect states and a reviewable manifest of what a backup contains.

## 14. Implementation order and review gates

### Pass A: a reference specification, then the shared design layer

Keep the current APK as the functional baseline. Record exact reference version/device, image provenance and target states. Extract normalized proportions rather than copying phone-frame pixel dimensions. Define the icon language, typography, spacing, corner families and resolved appearance state. Distinguish observed dimensions from proposed values. No generic replacement layout should be called a match just because it has circles and a clock.

**Gate:** one light/dark Classic Home and one light/dark translucent Home, with coordinated dock, Search, controls and two representative widgets. Actual app screenshots compared against labelled references; existing users' layouts preserved.

### Pass B: icons, Home and wallpapers

Replace the primary outline set; preserve component mapping and fallback coverage. Calibrate grid/optical sizes and mixed-size placement. Rework local wallpaper artwork, crop and palette extraction. Separate dark mode from wallpaper choice. Introduce optional curated layouts. No online wallpaper catalogue or runtime font download.

**Gate:** icons remain recognizable at normal and Max size; colours agree across widgets, folders and Search; repeated wallpaper/theme switches leave no stale surfaces; three tested display/font scales remain usable.

### Pass C: menus and configuration

Use one component language for Home popup, app/widget actions, visual folder picker, Customisation, Theme & Style, wallpaper editor and widget sheets. Add live, page-aware previews and a clear, coherent settings structure. Preserve back/scroll/focus and transactional cancel/Done semantics.

**Gate:** a reference-based screen checklist covers all routes and selected/disabled/pressed states. A colour selection alone never unexpectedly changes a layout or wallpaper.

### Pass D: drawer and organisation

Build the actual category-card Smart Drawer presentation on top of offline categorisation. Make hidden icons an integrated interactive surface. Expose local search scopes and hidden-in-search policy. Replace the group ghost/toolbar presentation and improve placement feedback without sacrificing atomic updates. Complete visual folder covers and normal/expanded variants.

**Gate:** test with a realistically large synthetic app set, long labels, category mistakes and profile fixtures; cross-page group move, cancellation, collision and undo are visible and recoverable.

### Pass E: signature widgets, in families

Prioritize clocks/date, Photos/Collector, Away Time, breathing, battery/weather presentation and compact controls. Then finish Quick Look, screen time, countdown/timer, media, compass and pedometer. Each family gets real data semantics, supported sizes, its own editing interaction, empty/denied states and frame/lifecycle behaviour. A style-only imitation should not be labelled a completed feature.

**Gate:** compare each size in light/dark and Classic/translucent modes; verify real data/action changes, revocation and reboot. Simulated preview values remain visibly confined to the picker/editor preview.

### Pass F: motion and physical-device validation

Tune launcher-owned motion against actual recordings. Use frame-driven animations and lifecycle pause/resume. Measure POCO 60/120Hz transitions and jank; exercise cancellation and fast successive gestures. Keep native system navigation. No Accessibility, root, global overlay or Internet path is introduced.

**Gate:** collect actual before/after screens and motion with the exact commit; run upgrade/migration/backup/privacy checks. Obtain the user's visual judgement before calling the result a successful Nothing-style implementation. Functional CI stays necessary but insufficient.

The highest-return order is **shared appearance → primary icons/Home → configuration surfaces → signature widget behaviour → drawer/folder/editing polish → motion/device validation**, with feature-specific work arranged around those dependencies. Another large list of uncoordinated widgets would not solve the main problem.

## 15. What to retain, replace and verify

**Retain:** Launcher3/Lawnchair workspace/model and reorder machinery, widget host, component identity, Home role flow, original signing identity, component-to-icon map, transactional movement, migration/recovery and offline enforcement.

**Replace or substantially rework:** primary icon artwork, colour/material resolution, default Home composition, procedural wallpaper library, generic settings/widget/folder configuration, category presentation, separate hidden Activity interaction and custom group drag presentation.

**Verify before claims:** exact general-release theme labels/tinted icon coverage, profile behaviour, real POCO frame timing, predictive gesture cancellation, every external widget/provider integration and portable restore coverage.

**Preserve deliberate differences:** zero network access; no Accessibility service; stock Android/HyperOS global navigation and system surfaces; optional access requested only for a feature that needs it. These constraints do not prevent a much closer launcher-owned visual and interaction match.

This audit changes no APK or release. It establishes a concrete next design and implementation scope against the released v4 foundation.

## Appendix: inspectable implementation evidence

These links pin the findings to the released source, rather than the current moving branch.

| Finding | Released implementation |
| --- | --- |
| Appearance choices, swatches and separate settings rendering | [DotPreferences](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotPreferences.kt#L147) |
| Fixed surface mix and alpha | [DotSurface](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotSurface.java#L7) |
| Saved-wallpaper blur cache | [DotBackdrop](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotBackdrop.kt#L12) |
| Engine icon palette differs from local tile surface path | [polish.py](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/patches/polish.py#L32) |
| Provider definitions and face variants | [DotTiles](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/DotTiles.kt#L102) |
| Away/usage tap opens access settings | [TileActions](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/TileActions.java#L21) |
| Away graphic and Focus artwork | [TileArtwork](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/TileArtwork.java#L351) |
| Per-family invalidation cadence and generic serif | [TileArtwork](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/TileArtwork.java#L470) |
| Single-image sticker fit and clipping behaviour | [PhotoView](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/PhotoView.java#L68) |
| Fixed-height style sheet and shared controls | [TileConfiguration](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/widgets/TileConfiguration.java#L64) |
| Hidden gesture opens an Activity | [HomeActivity](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/HomeActivity.kt#L38) |
| Hidden app page and checkbox editor | [DotHidden](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotHidden.kt#L17) |
| Custom group drag representation and collision policy | [DotArrange](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotArrange.kt#L68) |
| Text-based cover chooser and fixed preview layout | [DotFolderPreview](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotFolderPreview.java#L12) |
| Backup preference scope | [DotBackup](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/overlay/lawnchair/src/dev/dothome/ui/DotBackup.java#L13) |
| Offline and privileged-feature exclusions | [offline.py](https://github.com/Nrajesh0/Launcher-by-rj/blob/b94b5e89a3a8830103a1a3a82cac7bc355d08c37/foundation/patches/offline.py) |
| Actual upstream category adapter used by the pinned foundation | [LawnchairAlphabeticalAppsList](https://github.com/LawnchairLauncher/lawnchair/blob/505dbc40e6154c05158b5d0271c45f6a885a411b/lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt#L88) |

The accompanying comparison matrix contains 74 separately scoped audit items. Its statuses refer to capability/design evidence, not a numerical quality score. The standalone HTML includes original-content evidence images, filters and this complete written report.
