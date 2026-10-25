#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
BT="${BUILD_TOOLS_DIR:-$SDK/build-tools/36.0.0}"
if [[ ! -d "$BT" && -d "$SDK/build-tools/android-16" ]]; then BT="$SDK/build-tools/android-16"; fi
PLATFORM="$SDK/platforms/android-36/android.jar"
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
VERSION="$(python3 scripts/prepare_version.py)"
python3 tests/check_offline.py
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$BT/aapt2" link -o build/base.apk -I "$PLATFORM" --manifest build/generated/AndroidManifest.xml --java build/generated build/resources.zip
find app/src/main/java build/generated -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 17 -target 17 -classpath "$PLATFORM" -d build/classes @build/sources.txt
jar --create --file build/classes.jar -C build/classes .
"$BT/d8" --lib "$PLATFORM" --min-api 35 --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -u ../unsigned.apk classes*.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
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
