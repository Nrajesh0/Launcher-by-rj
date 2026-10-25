# Nothing OS 5 launcher reference baseline

Target frozen on 25 October 2026: Nothing OS 5.0 general-release Home, with Classic and Translucent styles and separate light/dark appearance. These are reference sources, not redistributed artwork or bundled software.

- Official overview: https://nothing.tech/nothing-os
- Official feature/interaction explanation: https://nothing.community/d/61730
- Home material reference: https://cdn.nothing.community/2026-08-25/1787655333-429526-image5-1.png
- Batch editing recording: https://cdn.nothing.community/2026-08-24/1787549689-179917-hsbatchq-ezgifcom-video-to-gif-converter.gif
- Widget recording: https://cdn.nothing.community/2026-08-24/1787536756-511941-widgets50-ezgifcom-video-to-gif-converter-1.gif
- User-supplied reference photographs in the conversation provide additional home compositions, circular dock/search treatment and widget menus. Their captions are not independent OS-version evidence.

Observed: white/dark rounded widget surfaces, circular 1×1 controls, whitespace between mixed-size items, restrained wallpaper tint, transparent sticker edges, and a type hierarchy distinct from generic system dialog cards. The official cropped Home image does not establish complete page or drawer measurements. No exact pixel-parity claim is made for unmeasured screens.

Implementation tokens: 4-column default Home, 7 rows, 5dp item inset, 20dp widget corners, 46dp normal icon artwork, 52dp search field and 16dp drawer side inset. These are responsive implementation choices, not claimed measurements of a Nothing device. They scale with Android density, preserve system navigation insets and allow 4/5-column layouts. Large icons and widgets use cell spans rather than ad-hoc pixel offsets.

Evidence required: actual light/dark Home, drawer, folder, widget picker/style choices, customisation/appearance, group editing, system Home/Recents and interrupted/cancelled pulls. CI uploads screenshots and motion recording with its exact source commit. Generated mockups are not visual acceptance evidence. Physical POCO comparison and final user acceptance remain separate from emulator functionality.

Typeface provenance: unmodified Geist Regular (Git blob 71cb8160a35149c54c83f5ea92f034f3bae67e61) and Medium (492b319a5ecd257d01264061522224a144564b0c), from https://github.com/vercel/geist-font. SIL Open Font License is included both in LICENSES/Geist-OFL.txt and the installed APK's assets/licenses/Geist-OFL.txt. There is no runtime font download.
