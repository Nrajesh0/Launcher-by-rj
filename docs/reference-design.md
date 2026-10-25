# Visual reference decisions

The images supplied in this conversation guide proportions and visual language. They include a mixture of Home, lock-screen, app and System UI screens; thumbnail OS-version captions have not been verified as release documentation. No product photograph, wallpaper, proprietary font or logo artwork was extracted into the APK.

| Reference detail | Launcher implementation / boundary |
| --- | --- |
| Sparse layout and generous gaps | Explicit cells, drag placement, optional styled page; existing layouts are preserved on upgrade. |
| Circular analogue clocks, serif and segmented numbers | Original Canvas clock faces with shared previews, 12/24-hour format and locale-aware dates. |
| Round monochrome apps and smaller dock | Adaptive monochrome layers where available, consistent circular fallback containers, configurable dock. Fallbacks do not recreate proprietary icons. |
| White/ink/sage/red cards and translucent surfaces | Built-in colour/opacity choices, original offline Ocean/Soft mist backgrounds and local-image wallpaper selection. Transparency is not live background blur. |
| Focus orbital artwork | Original breathing animation with reduced-motion support. It does not claim medical effects. |
| Floating photo/sticker | Local images with rectangle/circle/sticker presentation and an original decorative car drawing when the sticker has no chosen image; source-image transparency is preserved. No automatic object extraction is implied. |
| Media artwork, notes and date cards | Actual local media-session data, editable notes, date countdowns and a local elapsed timer; no invented flight status or online content. |
| Earbud/headphone batteries | Provider widgets may supply these. No fake accessory percentages or new Bluetooth data permission is added. |
| Weather, online galleries and cloud/AI services | Excluded under the required no-Internet contract. |
| Glyph timers | POCO has no Nothing Glyph hardware; timers can be delegated to the installed Clock app. |
| Lock screen, fingerprint target, notifications and Quick Settings shade | Controlled by HyperOS; a Home replacement cannot install Nothing System UI or change the secure unlock flow. |
| Gesture pill | Optional Accessibility edge actions need no root/ADB. Firmware controls hiding three-button navigation and privileged app/Recents animations. |

The Android 16 UI workflow retains screenshots and, when supported by the emulator encoder, a short motion recording. These demonstrate our implementation; exact visual/motion parity and physical POCO behaviour remain separate acceptance checks.
