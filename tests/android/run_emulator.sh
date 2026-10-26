#!/usr/bin/env bash
# Own the emulator lifetime in one shell and preserve its exit code separately
# from application instrumentation. No emulator private disk/state is exported.
set -euo pipefail
mkdir -p build/ui-test
(set +e; "$ANDROID_HOME/emulator/emulator" -avd foundation -no-window -gpu swiftshader -feature -Vulkan -noaudio -no-boot-anim -no-snapshot -camera-back none -memory 4096 -cores 2 -skin 720x1600 >build/ui-test/emulator-host.txt 2>&1; echo "$?" >build/ui-test/emulator-exit.txt) &
emulator_owner=$!
cleanup() { timeout 10s adb emu kill >/dev/null 2>&1 || true; kill "$emulator_owner" 2>/dev/null || true; }
trap cleanup EXIT
booted=false
for ((attempt=0;attempt<90;attempt++)); do
  if timeout 5s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; then booted=true; break; fi
  if [ -f build/ui-test/emulator-exit.txt ]; then cat build/ui-test/emulator-exit.txt; tail -60 build/ui-test/emulator-host.txt; exit 1; fi
  sleep 2
done
$booted || { echo 'Emulator did not boot'; exit 1; }
# Let first-boot package provisioning finish before starting UI automation.
timeout 20s adb shell input keyevent 82
sleep 20
bash tests/android/exercise_foundation.sh
