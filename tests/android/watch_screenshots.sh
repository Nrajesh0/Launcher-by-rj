#!/usr/bin/env bash
# Preserve only screenshots produced in the fresh emulator's synthetic test run.
set +e
mkdir -p build/ui-test/foundation-screenshots
for ((attempt=0; attempt<75; attempt++)); do
  date -u '+%H:%M:%S'
  ps -eo pid,rss,comm | grep -E 'qemu|emulator|adb' || true
  adb get-state 2>&1 || exit 0
  adb shell dumpsys activity activities 2>/dev/null | grep -m1 -E 'mResumedActivity|topResumedActivity' || true
  adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/ >/dev/null 2>&1
  sleep 4
done
