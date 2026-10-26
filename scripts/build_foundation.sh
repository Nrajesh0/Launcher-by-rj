#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
# Keep the app-code stamp stable for CI/test-only edits, allowing cached release compilation.
# Release provenance still records the complete, successfully tested repository commit.
export DOT_HOME_SOURCE_COMMIT="$(git log -1 --format=%H -- foundation version.properties build.sh scripts/build_foundation.sh scripts/prepare_foundation.py app/src/main/res/font LICENSES/Geist-OFL.txt)"
python3 scripts/prepare_foundation.py
variant="${DOT_BUILD_TYPE:-Release}"
bash build/foundation/gradlew -p build/foundation "assembleLawnWithQuickstepGithub$variant" --build-cache --no-daemon --max-workers=2
version=$(python3 -c 'from pathlib import Path; print(dict(line.split("=",1) for line in Path("version.properties").read_text().splitlines() if "=" in line)["versionName"])')
if [[ "$variant" == 'Release' ]]; then
  output="build/dot-home-$version-unsigned.apk"
  cp "build/foundation/build/outputs/apk/lawnWithQuickstepGithub/release/Lawnchair.$version.github.release.apk" "$output"
else
  output="build/dot-home-$version-development.apk"
  cp "build/foundation/build/outputs/apk/lawnWithQuickstepGithub/debug/Lawnchair.$version.github.debug.apk" "$output"
fi
python3 tests/check_foundation.py "$ANDROID_HOME/build-tools/36.1.0/aapt" "$output"
(cd build && sha256sum "$(basename "$output")" > "$(basename "$output").sha256")
echo "Foundation APK built: $output"
