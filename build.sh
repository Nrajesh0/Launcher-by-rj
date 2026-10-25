#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
export ANDROID_HOME="$SDK"
BT="${BUILD_TOOLS_DIR:-$SDK/build-tools/36.0.0}"
if [[ ! -d "$BT" && -d "$SDK/build-tools/android-16" ]]; then BT="$SDK/build-tools/android-16"; fi
PLATFORM="$SDK/platforms/android-36/android.jar"
mkdir -p build
VERSION="$(python3 scripts/prepare_version.py)"
python3 tests/check_offline.py
GRADLE_BIN="${GRADLE_BIN:-$PWD/gradlew}"
"$GRADLE_BIN" :app:assembleRelease --no-daemon
cp app/build/outputs/apk/release/app-release-unsigned.apk build/aligned.apk
if [[ "${1:-}" == '--unsigned' ]]; then
  OUTPUT="build/dot-home-$VERSION-unsigned.apk"
  cp build/aligned.apk "$OUTPUT"
else
  KEYSTORE="${SIGNING_KEYSTORE:-keys/local-release.p12}"
  export SIGNING_STORE_PASS="${SIGNING_STORE_PASS:-dothome-local}"
  export SIGNING_KEY_PASS="${SIGNING_KEY_PASS:-$SIGNING_STORE_PASS}"
  ALIAS="${SIGNING_KEY_ALIAS:-dothome}"
  if [[ ! -f "$KEYSTORE" ]]; then
    if [[ "${CI:-false}" == 'true' ]]; then echo 'Signing key missing; refusing to change release identity.' >&2; exit 1; fi
    mkdir -p keys
    keytool -genkeypair -keystore "$KEYSTORE" -storepass:env SIGNING_STORE_PASS -keypass:env SIGNING_KEY_PASS -alias "$ALIAS" -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Dot Home Local Build' -noprompt
  fi
  OUTPUT="build/dot-home-$VERSION.apk"
  "$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass env:SIGNING_STORE_PASS --key-pass env:SIGNING_KEY_PASS --ks-key-alias "$ALIAS" --out "$OUTPUT" build/aligned.apk
  "$BT/apksigner" verify --verbose "$OUTPUT"
fi
python3 tests/check_offline.py "$BT/aapt" "$OUTPUT"
(cd build && sha256sum "$(basename "$OUTPUT")" > "$(basename "$OUTPUT").sha256")
printf 'APK built: %s/%s\n' "$PWD" "$OUTPUT"
