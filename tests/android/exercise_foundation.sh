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
adb uninstall dev.dothome
adb install --no-incremental build/ui-test/previous.apk
adb install -r --no-incremental build/ui-test/tests.apk
timeout 60s adb shell am instrument -w dev.dothome.test/dev.dothome.test.LegacySeed > build/ui-test/legacy-seed.txt
timeout 20s adb logcat -d -s AndroidRuntime > build/ui-test/legacy-seed-runtime.txt
cat build/ui-test/legacy-seed.txt
grep -q 'PASS: synthetic' build/ui-test/legacy-seed.txt
adb install -r --no-incremental build/ui-test/app.apk
timeout 150s adb shell am instrument -w dev.dothome.test/dev.dothome.test.FoundationUpgrade > build/ui-test/upgrade-result.txt
cat build/ui-test/upgrade-result.txt
timeout 20s adb logcat -d -s AndroidRuntime DotMigration > build/ui-test/upgrade-runtime.txt
timeout 20s adb pull /sdcard/Download/. build/ui-test/foundation-screenshots/
grep -q 'PASS: v3-to-v4' build/ui-test/upgrade-result.txt
! grep -q 'FATAL EXCEPTION' build/ui-test/upgrade-runtime.txt
