package dev.dothome.test;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import org.json.*;

/** Device-level UI checks. Runs inside a fresh emulator, without app Internet access. */
public class UiSmoke extends Instrumentation {
  Activity home;
  Object ui;

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  void call(Object object, String method, Class<?>[] types, Object... args) {
    try {
      Method m = object.getClass().getDeclaredMethod(method, types);
      m.setAccessible(true);
      m.invoke(object, args);
    } catch (Exception e) {
      throw new RuntimeException(method, e);
    }
  }

  Object field(Object object, String name) {
    try {
      Field f = object.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.get(object);
    } catch (Exception e) {
      throw new RuntimeException(name, e);
    }
  }

  void onUi(Runnable work) {
    runOnMainSync(work);
    waitForIdleSync();
    SystemClock.sleep(450);
  }

  void assertText(String expected) {
    final boolean[] found = {false};
    runOnMainSync(() -> found[0] = contains(home.getWindow().getDecorView(), expected));
    if (!found[0]) throw new AssertionError("Missing UI text: " + expected);
  }

  boolean contains(View v, String text) {
    if (v instanceof TextView && ((TextView) v).getText().toString().equals(text)) return true;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        if (contains(((ViewGroup) v).getChildAt(i), text)) return true;
    return false;
  }

  void shell(String command) throws Exception {
    try (java.io.InputStream in =
        new ParcelFileDescriptor.AutoCloseInputStream(
            getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
                .executeShellCommand(command))) {
      in.readAllBytes();
    }
  }

  void screenshot(String name) throws Exception {
    ParcelFileDescriptor fd =
        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            .executeShellCommand("screencap -p /sdcard/Download/" + name + ".png");
    try (java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      in.readAllBytes();
    }
  }

  boolean awaitFocus(boolean expected) {
    final boolean[] value = {false};
    for (int n = 0; n < 50; n++) {
      runOnMainSync(() -> value[0] = home.hasWindowFocus());
      if (value[0] == expected) return true;
      SystemClock.sleep(100);
    }
    return false;
  }

  String navigationDiagnostic(Object connected) {
    return " action="
        + field(connected, "lastGlobalAction")
        + " result="
        + field(connected, "lastActionSucceeded")
        + " touches="
        + java.util.Arrays.toString((int[]) field(connected, "edgeTouches"))
        + " destroyed="
        + home.isDestroyed();
  }

  void bottomSwipe(Object service, boolean hold) throws Exception {
    final int[] location = new int[2];
    final int[] size = new int[2];
    onUi(
        () -> {
          View bottom = (View) ((java.util.List<?>) field(service, "edges")).get(0);
          bottom.getLocationOnScreen(location);
          size[0] = bottom.getWidth();
          size[1] = bottom.getHeight();
        });
    float x = location[0] + size[0] / 2f,
        y = location[1] + size[1] - 4 * home.getResources().getDisplayMetrics().density;
    long down = SystemClock.uptimeMillis();
    for (int n = 0; n <= 8; n++) {
      if (n == 8 && hold) SystemClock.sleep(600);
      MotionEvent e =
          MotionEvent.obtain(
              down,
              SystemClock.uptimeMillis(),
              n == 0
                  ? MotionEvent.ACTION_DOWN
                  : n == 8 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE,
              x,
              y - n * 14 * home.getResources().getDisplayMetrics().density,
              0);
      getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
          .injectInputEvent(e, true);
      e.recycle();
      SystemClock.sleep(25);
    }
    waitForIdleSync();
    SystemClock.sleep(900);
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      home =
          startActivitySync(
              new Intent()
                  .setClassName("dev.dothome", "dev.dothome.HomeActivity")
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      waitForIdleSync();
      SystemClock.sleep(1800);
      ui = field(home, "ui");
      assertText("Welcome to Dot Home");
      screenshot("setup");
      onUi(
          () -> {
            home.getSharedPreferences("runtime", 0).edit().putBoolean("setupSeen2", true).apply();
            call(ui, "close", new Class[] {});
          });
      if (getTargetContext().checkSelfPermission("android.permission.INTERNET")
          != PackageManager.PERMISSION_DENIED)
        throw new AssertionError("App has Internet permission");
      onUi(
          () -> {
            View screen = (View) field(ui, "screen");
            if (screen.getWidth() < 300 || screen.getHeight() < 600)
              throw new AssertionError("Workspace not laid out");
          });
      screenshot("home");
      // Only the emulator fixture changes native navigation mode for clean design captures.
      shell(
          "cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural");
      SystemClock.sleep(800);
      onUi(
          () -> {
            try {
              JSONObject settings = (JSONObject) field(home, "settings");
              settings.put("dark", true);
              settings.put("wallpaperPreset", "ocean");
              call(ui, "refresh", new Class[] {});
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      screenshot("home-dark");
      onUi(
          () -> {
            try {
              JSONObject settings = (JSONObject) field(home, "settings");
              settings.put("dark", false);
              settings.put("wallpaperPreset", "mist");
              call(ui, "refresh", new Class[] {});
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      screenshot("home-light");
      shell(
          "cmd overlay enable-exclusive --category"
              + " com.android.internal.systemui.navbar.threebutton");
      SystemClock.sleep(800);

      onUi(
          () ->
              call(
                  ui,
                  "drawer",
                  new Class[] {java.util.function.Consumer.class},
                  new Object[] {null}));
      assertText("Apps");
      screenshot("drawer");
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(() -> call(ui, "widgetPicker", new Class[] {}));
      assertText("Widgets");
      assertText("Clock");
      screenshot("widgets");
      onUi(
          () ->
              call(ui, "stylePicker", new Class[] {String.class, JSONObject.class}, "clock", null));
      assertText("Choose a style");
      assertText("ANALOGUE · SAMPLE PREVIEW");
      assertText("ROUNDED DIGITAL · SAMPLE PREVIEW");
      screenshot("styles");
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(() -> call(ui, "customise", new Class[] {}));
      assertText("Customisation");
      assertText("Appearance");
      screenshot("customisation");
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(() -> call(ui, "overview", new Class[] {}));
      assertText("Home screen");
      screenshot("overview");
      onUi(() -> call(ui, "referencePage", new Class[] {}));
      screenshot("styled-page");
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(
          () -> {
            try {
              JSONObject folder =
                  new JSONObject()
                      .put("id", "ui-test-folder")
                      .put("type", "folder")
                      .put("page", 0)
                      .put("w", 1)
                      .put("h", 1)
                      .put("name", "Test folder")
                      .put("children", new JSONArray());
              call(ui, "addItem", new Class[] {JSONObject.class}, folder);
              call(ui, "folder", new Class[] {JSONObject.class}, folder);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      assertText("Test folder");
      screenshot("folder");
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(() -> call(ui, "navigation", new Class[] {}));
      assertText("Gesture navigation");
      screenshot("navigation");
      // ADB is used only by automated tests on this disposable emulator, never by app setup.
      shell(
          "settings put secure enabled_accessibility_services"
              + " dev.dothome/dev.dothome.GestureNavigationService");
      shell("settings put secure accessibility_enabled 1");
      Class<?> serviceClass =
          Class.forName("dev.dothome.GestureNavigationService", true, home.getClassLoader());
      Field activeField = serviceClass.getDeclaredField("active");
      activeField.setAccessible(true);
      Object service = null;
      for (int n = 0; n < 50 && service == null; n++) {
        SystemClock.sleep(100);
        service = activeField.get(null);
      }
      if (service == null) throw new AssertionError("Gesture service did not connect");
      Object connected = service;
      onUi(
          () -> {
            java.util.List<?> edges = (java.util.List<?>) field(connected, "edges");
            if (edges.size() != 3) throw new AssertionError("Missing gesture edge windows");
          });
      android.accessibilityservice.AccessibilityServiceInfo info =
          ((android.accessibilityservice.AccessibilityService) service).getServiceInfo();
      if ((info.getCapabilities()
              & android.accessibilityservice.AccessibilityServiceInfo
                  .CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT)
          != 0) throw new AssertionError("Gesture service can read windows");
      // Swipe on the real edge overlay and verify the global Back action closes the setup.
      android.util.DisplayMetrics metrics = home.getResources().getDisplayMetrics();
      float start = 2 * metrics.density, y = metrics.heightPixels * .5f;
      long down = SystemClock.uptimeMillis();
      for (int n = 0; n <= 8; n++) {
        MotionEvent event =
            MotionEvent.obtain(
                down,
                SystemClock.uptimeMillis(),
                n == 0
                    ? MotionEvent.ACTION_DOWN
                    : n == 8 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE,
                start + n * 10 * metrics.density,
                y,
                0);
        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            .injectInputEvent(event, true);
        event.recycle();
        SystemClock.sleep(25);
      }
      waitForIdleSync();
      SystemClock.sleep(600);
      onUi(
          () -> {
            if (field(ui, "overlay") != null)
              throw new AssertionError("Back gesture did not close setup");
          });
      onUi(() -> call(ui, "navigation", new Class[] {}));
      assertText("Edge gestures are running");
      screenshot("gestures-running");
      shell("am start -a android.settings.SETTINGS");
      SystemClock.sleep(700);
      bottomSwipe(connected, false);
      screenshot("gesture-home");
      if (!awaitFocus(true))
        throw new AssertionError(
            "Home swipe did not return to launcher" + navigationDiagnostic(connected));
      bottomSwipe(connected, true);
      screenshot("gesture-recents");
      if (!awaitFocus(false))
        throw new AssertionError(
            "Hold gesture did not open Recents" + navigationDiagnostic(connected));
      bottomSwipe(connected, false);
      shell("settings put secure enabled_accessibility_services null");
      shell("settings put secure accessibility_enabled 0");
      shell(
          "cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural");
      SystemClock.sleep(800);
      screenshot("home-gesture-mode");
      result.putString(
          "stream",
          "PASS: home, integrated drawer, visual picker, style gallery, customisation, overview,"
              + " folder, navigation, live Accessibility edge windows, real Back/Home/Recents"
              + " swipes, no screen-reading capability and offline permission.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable error) {
      result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
