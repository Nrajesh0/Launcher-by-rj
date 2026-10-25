#!/usr/bin/env bash
# Sign a CI-produced APK locally without exporting the private key to GitHub.
set -euo pipefail
cd "$(dirname "$0")/.."
INPUT="${1:?Usage: sign_apk.sh /absolute/path/to/unsigned.apk}"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
BT="${BUILD_TOOLS_DIR:-$SDK/build-tools/36.0.0}"
if [[ ! -d "$BT" && -d "$SDK/build-tools/android-16" ]]; then BT="$SDK/build-tools/android-16"; fi
KEYSTORE="${SIGNING_KEYSTORE:?Set SIGNING_KEYSTORE to the existing private keystore}"
export SIGNING_STORE_PASS="${SIGNING_STORE_PASS:?Set the keystore password}"
export SIGNING_KEY_PASS="${SIGNING_KEY_PASS:-$SIGNING_STORE_PASS}"
ALIAS="${SIGNING_KEY_ALIAS:-dothome}"
VERSION="$(python3 -c "from pathlib import Path; print(dict(x.split('=',1) for x in Path('version.properties').read_text().splitlines() if '=' in x)['versionName'])")"
mkdir -p build/release
OUTPUT="build/release/dot-home-$VERSION.apk"
"$BT/zipalign" -f -p 4 "$INPUT" build/release/aligned.apk
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass env:SIGNING_STORE_PASS --key-pass env:SIGNING_KEY_PASS --ks-key-alias "$ALIAS" --out "$OUTPUT" build/release/aligned.apk
"$BT/apksigner" verify --verbose "$OUTPUT"
python3 tests/check_offline.py "$BT/aapt" "$OUTPUT"
(cd build/release && sha256sum "dot-home-$VERSION.apk" > "dot-home-$VERSION.apk.sha256")
printf 'Signed CI APK: %s/%s\n' "$PWD" "$OUTPUT"
