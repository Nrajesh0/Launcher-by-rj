# Launcher foundation

The previous Activity/pager engine was rejected by the user for visual and interaction quality.
This directory carries the replacement's source overlays and removal patches over pinned
Launcher3/Lawnchair source. `upstream.properties` freezes both repositories. The upstream
copyright and licence files stay in the prepared tree and corresponding-source distribution.

Online search, remote defaults, updater/download and bug-report upload implementations,
Accessibility components and privileged navigation entries are removed before compilation.
This is mandatory even though the packaged app has no Internet permission. The raw upstream
APK is never a Dot Home release.

The installed package, Home component and signing identity remain `dev.dothome`,
`dev.dothome.HomeActivity` and the existing private local key. The old placement file is retained
with a pre-foundation recovery copy. Migration, native widgets, appearance and device testing
must pass before switching the release build to this foundation.
