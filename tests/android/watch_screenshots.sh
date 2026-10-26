#!/usr/bin/env bash
# Preserve only screenshots produced in the fresh emulator's synthetic test run.
set +e
mkdir -p build/ui-test/foundation-screenshots
for ((attempt=0; attempt<75; attempt++)); do
  adb get-state >/dev/null 2>&1 || exit 0
  adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/ >/dev/null 2>&1
  sleep 4
done
