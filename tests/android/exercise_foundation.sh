#!/usr/bin/env bash
set -euo pipefail
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
adb shell wm density 280
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb logcat -c
adb install -r --no-incremental build/ui-test/app.apk
adb install -r --no-incremental build/ui-test/tests.apk
nohup adb logcat -v threadtime -s AndroidRuntime ActivityManager ActivityTaskManager libc art Zygote DotMigration DotFoundation AppWidgetHostView > build/ui-test/runtime-live.txt 2>&1 < /dev/null &
nohup bash tests/android/watch_screenshots.sh > build/ui-test/capture-status.txt 2>&1 < /dev/null &
timeout 420s adb shell am instrument -w dev.dothome.test/dev.dothome.test.FoundationSmoke > build/ui-test/foundation-result.txt || echo RUNNER_ERROR_OR_TIMEOUT >> build/ui-test/foundation-result.txt
cat build/ui-test/foundation-result.txt
timeout 20s adb logcat -d -s AndroidRuntime DotMigration CustomWidgetManager DotFoundation LoaderTask WorkspaceItemProcessor WidgetInflater LauncherModel AppWidgetHostView LauncherAppWidgetHostView > build/ui-test/foundation-runtime.txt
timeout 20s adb pull /sdcard/Download build/ui-test/foundation-screenshots
grep -q 'PASS: foundation' build/ui-test/foundation-result.txt
! grep -q 'FATAL EXCEPTION' build/ui-test/foundation-runtime.txt

# The following suites have independent, fresh upgrade fixtures. Collect every
# failure in one run; any failed assertion still fails the complete release gate.
checks_failed=false
run_check() {
  local seconds="$1" instrument="$2" result="$3" expected="$4"
  timeout "$seconds" adb shell am instrument -w "dev.dothome.test/dev.dothome.test.$instrument" > "build/ui-test/$result" || checks_failed=true
  cat "build/ui-test/$result"
  if ! grep -q "$expected" "build/ui-test/$result"; then checks_failed=true; fi
}
run_check 420 ExperienceSmoke experience-result.txt 'PASS: v5'
timeout 20s adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/
adb uninstall dev.dothome
adb install --no-incremental build/ui-test/previous.apk
adb install -r --no-incremental build/ui-test/tests.apk
run_check 60 LegacySeed legacy-seed.txt 'PASS: synthetic'
timeout 20s adb logcat -d -s AndroidRuntime > build/ui-test/legacy-seed-runtime.txt
adb install -r --no-incremental build/ui-test/app.apk
run_check 150 FoundationUpgrade upgrade-result.txt 'PASS: v3-to-v5'
timeout 20s adb logcat -d -s AndroidRuntime DotMigration > build/ui-test/upgrade-runtime.txt
if grep -q 'FATAL EXCEPTION' build/ui-test/upgrade-runtime.txt; then checks_failed=true; fi
timeout 20s adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/
adb uninstall dev.dothome
adb install --no-incremental build/ui-test/previous-v4.apk
adb install -r --no-incremental build/ui-test/tests.apk
run_check 180 FoundationV4Seed v4-seed-result.txt 'PASS: synthetic v4'
adb install -r --no-incremental build/ui-test/app.apk
run_check 120 FoundationV4Upgrade v4-upgrade-result.txt 'PASS: v4-to-v5'
timeout 20s adb logcat -d -s AndroidRuntime DotMigration > build/ui-test/v4-upgrade-runtime.txt
if grep -q 'FATAL EXCEPTION' build/ui-test/v4-upgrade-runtime.txt; then checks_failed=true; fi
timeout 20s adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/
if "$checks_failed"; then exit 1; fi
