#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/workspace/android-sdk}}"
BT="${BUILD_TOOLS_DIR:-$SDK/build-tools/36.0.0}"
if [[ ! -d "$BT" && -d "$SDK/build-tools/android-16" ]]; then BT="$SDK/build-tools/android-16"; fi
mkdir -p build/ui-test/classes build/ui-test/dex
"$BT/aapt2" compile --dir tests/android/res -o build/ui-test/resources.zip
"$BT/aapt2" link -o build/ui-test/base.apk -I "$SDK/platforms/android-36/android.jar" --manifest tests/android/AndroidManifest.xml build/ui-test/resources.zip
javac -source 17 -target 17 -classpath "$SDK/platforms/android-36/android.jar" -d build/ui-test/classes tests/android/src/dev/dothome/test/*.java
jar --create --file build/ui-test/classes.jar -C build/ui-test/classes .
"$BT/d8" --lib "$SDK/platforms/android-36/android.jar" --min-api 35 --output build/ui-test/dex build/ui-test/classes.jar
cp build/ui-test/base.apk build/ui-test/unsigned.apk
(cd build/ui-test/dex && zip -q ../unsigned.apk classes*.dex)
"$BT/zipalign" -f 4 build/ui-test/unsigned.apk build/ui-test/aligned.apk
