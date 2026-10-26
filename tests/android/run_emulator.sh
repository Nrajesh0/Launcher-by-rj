#!/usr/bin/env bash
# Repeat the complete fresh-device fixture only for a proven native emulator
# SIGSEGV, never for an application failure or an unmet UI assertion.
set -euo pipefail
if bash tests/android/run_emulator_once.sh swangle; then exit 0; fi
if [[ "$(cat build/ui-test/emulator-exit.txt 2>/dev/null)" != 139 ]]; then exit 1; fi
if grep -q 'FAIL:' build/ui-test/foundation-result.txt build/ui-test/experience-result.txt build/ui-test/v4-seed-result.txt build/ui-test/v4-upgrade-result.txt build/ui-test/upgrade-result.txt 2>/dev/null; then exit 1; fi
if grep -q 'FATAL EXCEPTION' build/ui-test/foundation-runtime.txt build/ui-test/runtime-live.txt 2>/dev/null; then exit 1; fi
echo 'ANGLE emulator exited on SIGSEGV; repeating all checks on a clean Mesa/llvmpipe device.'
archive=build/ui-test/renderer-attempt-swangle
mkdir -p "$archive"
for evidence in experience-result.txt v4-seed-result.txt v4-upgrade-result.txt v4-upgrade-runtime.txt foundation-result.txt foundation-runtime.txt runtime-live.txt capture-status.txt emulator-host.txt emulator-exit.txt upgrade-result.txt upgrade-runtime.txt legacy-seed.txt legacy-seed-runtime.txt foundation-screenshots; do
  if [[ -e "build/ui-test/$evidence" ]]; then mv "build/ui-test/$evidence" "$archive/"; fi
done
bash tests/android/run_emulator_once.sh mesa
