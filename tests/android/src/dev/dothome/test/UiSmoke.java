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
  final java.util.List<Long> frameTimes =
      java.util.Collections.synchronizedList(new java.util.ArrayList<>());

  String frameReport() {
    java.util.List<Long> copy;
    synchronized (frameTimes) {
      copy = new java.util.ArrayList<>(frameTimes);
    }
    if (copy.isEmpty()) return "FRAME_METRICS: unavailable on this emulator\n";
    java.util.Collections.sort(copy);
    return "FRAME_METRICS: frames="
        + copy.size()
        + " p95_ms="
        + copy.get(Math.min(copy.size() - 1, (int) (copy.size() * .95))) / 1000000.0
        + " max_ms="
        + copy.get(copy.size() - 1) / 1000000.0
        + " (emulator; not physical-device performance)\n";
  }

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

  android.view.accessibility.AccessibilityNodeInfo findSystemText(
      android.view.accessibility.AccessibilityNodeInfo node, String text) {
    if (node == null) return null;
    if (node.getText() != null && node.getText().toString().equalsIgnoreCase(text)) return node;
    for (int n = 0; n < node.getChildCount(); n++) {
      android.view.accessibility.AccessibilityNodeInfo result =
          findSystemText(node.getChild(n), text);
      if (result != null) return result;
    }
    return null;
  }

  boolean clickSystemText(String text) {
    android.view.accessibility.AccessibilityNodeInfo node =
        findSystemText(
            getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
                .getRootInActiveWindow(),
            text);
    if (node == null || !node.isVisibleToUser() || !node.isEnabled()) return false;
    // PermissionController's radio labels are not always accessibility-clickable.
    // Touch the visible label just as a user would, rather than granting the role
    // from a shell command, so this still exercises the real confirmation flow.
    android.graphics.Rect bounds = new android.graphics.Rect();
    node.getBoundsInScreen(bounds);
    if (bounds.isEmpty()) return false;
    long down = SystemClock.uptimeMillis();
    UiAutomation automation =
        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
    boolean accepted = true;
    for (int action : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
      MotionEvent event =
          MotionEvent.obtain(
              down,
              SystemClock.uptimeMillis(),
              action,
              bounds.exactCenterX(),
              bounds.exactCenterY(),
              0);
      event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
      accepted &= automation.injectInputEvent(event, true);
      event.recycle();
      SystemClock.sleep(80);
    }
    return accepted;
  }

  void chooseDefaultHome() throws Exception {
    android.app.role.RoleManager roles = home.getSystemService(android.app.role.RoleManager.class);
    if (roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))
      throw new AssertionError("Fresh fixture already holds Home role; chooser was not exercised");
    boolean displayed = false;
    for (int n = 0; n < 50; n++) {
      android.view.accessibility.AccessibilityNodeInfo root =
          getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
              .getRootInActiveWindow();
      String pkg = root == null ? "" : String.valueOf(root.getPackageName());
      if (pkg.contains("permissioncontroller")) {
        displayed = true;
        break;
      }
      SystemClock.sleep(100);
    }
    if (!displayed) throw new AssertionError("Automatic default-launcher chooser did not open");
    screenshot("default-launcher-prompt");
    if (!clickSystemText("Dot Home"))
      throw new AssertionError("Could not tap the visible Dot Home option");
    SystemClock.sleep(750);
    screenshot("default-launcher-selected");
    boolean confirmed = false;
    for (int n = 0; n < 30 && !confirmed; n++) {
      confirmed =
          clickSystemText("Set as default")
              || clickSystemText("Set default")
              || clickSystemText("Always")
              || clickSystemText("Use app");
      if (!confirmed) SystemClock.sleep(100);
    }
    for (int n = 0; n < 50 && !roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME); n++)
      SystemClock.sleep(100);
    if (!roles.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))
      throw new AssertionError("System Home selection failed; confirmed=" + confirmed);
    onUi(() -> {});
  }

  void syncActivity() {
    try {
      Class<?> type =
          Class.forName("dev.dothome.HomeActivity", true, getTargetContext().getClassLoader());
      Field current = type.getDeclaredField("activeHome");
      current.setAccessible(true);
      Activity actual = (Activity) current.get(null);
      if (actual != null && !actual.isDestroyed()) {
        home = actual;
        ui = field(home, "ui");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void onUi(Runnable work) {
    java.util.concurrent.atomic.AtomicReference<Throwable> failure =
        new java.util.concurrent.atomic.AtomicReference<>();
    runOnMainSync(
        () -> {
          try {
            syncActivity();
            work.run();
          } catch (Throwable error) {
            failure.set(error);
          }
        });
    if (failure.get() != null)
      throw new AssertionError("UI-thread assertion failed", failure.get());
    waitForIdleSync();
    SystemClock.sleep(450);
  }

  void assertText(String expected) {
    final boolean[] found = {false};
    runOnMainSync(
        () -> {
          syncActivity();
          found[0] = contains(home.getWindow().getDecorView(), expected);
        });
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
    assertNoSystemError();
    android.graphics.Bitmap bitmap=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).takeScreenshot();
    if(bitmap==null)throw new AssertionError("Screenshot was unavailable: "+name);
    var resolver=getTargetContext().getContentResolver();
    android.content.ContentValues values=new android.content.ContentValues();
    values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name+".png");
    values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");
    values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/DotHomeTest");
    android.net.Uri uri=resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
    if(uri==null)throw new AssertionError("Screenshot document could not be created");
    try(java.io.OutputStream output=resolver.openOutputStream(uri)) {
      if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output))throw new AssertionError("Screenshot encoding failed");
    } finally {bitmap.recycle();}
  }

  boolean hasSystemError(android.view.accessibility.AccessibilityNodeInfo node) {
    if (node == null) return false;
    CharSequence text = node.getText();
    if (text != null) {
      String value = text.toString().toLowerCase(java.util.Locale.ROOT);
      if (value.contains("responding") || value.contains("keeps stopping")) return true;
    }
    for (int n = 0; n < node.getChildCount(); n++)
      if (hasSystemError(node.getChild(n))) return true;
    return false;
  }

  void assertNoSystemError() {
    if (hasSystemError(
        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            .getRootInActiveWindow()))
      throw new AssertionError("System error dialog covers the launcher");
  }

  String systemTree(android.view.accessibility.AccessibilityNodeInfo node, int depth) {
    if (node == null || depth > 15) return "";
    android.graphics.Rect bounds = new android.graphics.Rect();
    node.getBoundsInScreen(bounds);
    StringBuilder text = new StringBuilder();
    text.append(node.getPackageName())
        .append(" ")
        .append(node.getClassName())
        .append(" text=")
        .append(node.getText())
        .append(" bounds=")
        .append(bounds)
        .append("\n");
    for (int n = 0; n < node.getChildCount() && text.length() < 12000; n++)
      text.append(systemTree(node.getChild(n), depth + 1));
    return text.toString();
  }

  boolean awaitFocus(boolean expected) {
    final boolean[] value = {false};
    for (int n = 0; n < 50; n++) {
      runOnMainSync(
          () -> {
            syncActivity();
            value[0] = home.hasWindowFocus();
          });
      if (value[0] == expected) return true;
      SystemClock.sleep(100);
    }
    return false;
  }

  void swipe(View origin, float fx, float fy, float dxDp, float dyDp, boolean cancel) {
    int[] at = new int[2];
    int[] size = new int[2];
    onUi(
        () -> {
          origin.getLocationOnScreen(at);
          size[0] = origin.getWidth();
          size[1] = origin.getHeight();
        });
    float density = home.getResources().getDisplayMetrics().density;
    long down = SystemClock.uptimeMillis();
    for (int n = 0; n <= 12; n++) {
      int action =
          n == 0
              ? MotionEvent.ACTION_DOWN
              : n == 12
                  ? (cancel ? MotionEvent.ACTION_CANCEL : MotionEvent.ACTION_UP)
                  : MotionEvent.ACTION_MOVE;
      MotionEvent event =
          MotionEvent.obtain(
              down,
              SystemClock.uptimeMillis(),
              action,
              at[0] + size[0] * fx + dxDp * density * n / 12f,
              at[1] + size[1] * fy + dyDp * density * n / 12f,
              0);
      event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
      if (!getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
          .injectInputEvent(event, true)) throw new AssertionError("Touch injection failed");
      event.recycle();
      SystemClock.sleep(16);
    }
    waitForIdleSync();
    SystemClock.sleep(500);
  }

  void assertDrawerVisible() {
    assertText("Apps");
    onUi(
        () -> {
          View panel = (View) field(ui, "activePanel");
          if (!field(ui, "state").equals("drawer")
              || panel.getTranslationY() > 1
              || panel.getAlpha() < .95f)
            throw new AssertionError("Swipe left the drawer hidden or off-screen");
        });
  }

  void assertHome() {
    onUi(
        () -> {
          if (field(ui, "overlay") != null || !field(ui, "state").equals("home"))
            throw new AssertionError("Unexpected Home overlay after a cancelled/short swipe");
        });
  }

  void configureFixture(boolean apply) throws Exception {
    for (int n = 0; n < 80; n++) {
      if (clickSystemText(apply ? "Apply test widget" : "Cancel test widget")) {
        if (!awaitFocus(true))
          throw new AssertionError("Widget configuration button did not return to Home");
        SystemClock.sleep(300);
        return;
      }
      // Exercise the actual system widget-consent UI when Home was not auto-granted binding.
      clickSystemText("Allow");
      clickSystemText("Create");
      SystemClock.sleep(100);
    }
    throw new AssertionError("Widget configuration did not open");
  }

  void checkProviderWidget() throws Exception {
    android.appwidget.AppWidgetManager manager =
        android.appwidget.AppWidgetManager.getInstance(home);
    android.appwidget.AppWidgetProviderInfo provider = null;
    for (android.appwidget.AppWidgetProviderInfo candidate : manager.getInstalledProviders())
      if (candidate.provider.getClassName().equals("dev.dothome.test.WidgetProvider"))
        provider = candidate;
    if (provider == null) throw new AssertionError("Test widget provider is not discoverable");
    final android.appwidget.AppWidgetProviderInfo chosen = provider;
    int before = ((JSONArray) field(home, "items")).length();
    onUi(
        () ->
            call(
                home,
                "addProvider",
                new Class[] {android.appwidget.AppWidgetProviderInfo.class},
                chosen));
    configureFixture(true);
    onUi(() -> {});
    JSONArray items = (JSONArray) field(home, "items");
    JSONObject widget = null;
    for (int n = 0; n < items.length(); n++)
      if (items.getJSONObject(n).optString("provider").contains("WidgetProvider"))
        widget = items.getJSONObject(n);
    if (widget == null || items.length() != before + 1)
      throw new AssertionError("Configured provider was not placed");
    int id = widget.getInt("widgetId");
    if (manager.getAppWidgetInfo(id) == null)
      throw new AssertionError("Placed widget is not bound");
    assertText("Provider widget configured");
    screenshot("provider-widget");
    final JSONObject installed = widget;
    onUi(
        () ->
            call(
                home,
                "addProvider",
                new Class[] {android.appwidget.AppWidgetProviderInfo.class},
                chosen));
    configureFixture(false);
    onUi(() -> {});
    if (((JSONArray) field(home, "items")).length() != before + 1
        || ((Number) field(home, "pendingWidget")).intValue() != -1)
      throw new AssertionError("Cancelled widget changed layout or leaked pending binding");
    onUi(() -> call(ui, "remove", new Class[] {JSONObject.class}, installed));
    if (manager.getAppWidgetInfo(id) != null
        || ((JSONArray) field(home, "items")).length() != before)
      throw new AssertionError("Widget removal did not release host binding");
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      // Connect before the app opens a system dialog so its window events are
      // observed from the start, rather than attaching halfway through setup.
      getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
      assertNoSystemError();
      home =
          startActivitySync(
              new Intent()
                  .setClassName("dev.dothome", "dev.dothome.HomeActivity")
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      waitForIdleSync();
      SystemClock.sleep(1800);
      ui = field(home, "ui");
      chooseDefaultHome();
      onUi(
          () ->
              home.getWindow()
                  .addOnFrameMetricsAvailableListener(
                      (window, metrics, dropped) ->
                          frameTimes.add(
                              metrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION)),
                      new Handler(Looper.getMainLooper())));
      // A Home intent may legitimately close setup after the role is granted.
      // Assert ownership above, then open setup explicitly to check its contents.
      onUi(() -> call(ui, "setup", new Class[] {}));
      assertText("Welcome to Dot Home");
      assertText("Confirmed by Android");
      screenshot("setup");
      onUi(
          () -> {
            home.getSharedPreferences("runtime", 0).edit().putBoolean("setupSeen2", true).commit();
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

      // Exercise physical Home swipes; reflection-only drawer opening missed the regression.
      View board = (View) field(home, "board");
      swipe(board, .5f, .72f, 0, -120, false);
      assertDrawerVisible();
      screenshot("drawer");
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(500);
      assertHome();
      swipe(board, .5f, .72f, 0, -40, false);
      assertHome();
      swipe(board, .5f, .72f, 0, -120, true);
      assertHome();
      // Swiping over an interactive Home tile must cancel its click and open Apps.
      View tile = ((android.view.ViewGroup) board).getChildAt(0);
      swipe(tile, .5f, .8f, 0, -100, false);
      assertDrawerVisible();
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(500);
      assertHome();
      // The hotseat lies outside the workspace pager and must work too.
      View dock = (View) field(ui, "hotseat");
      swipe(dock, .125f, .5f, 0, -120, false);
      assertDrawerVisible();
      screenshot("drawer-from-dock");
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(500);
      assertHome();
      // Horizontal paging must remain a page gesture rather than opening Apps.
      swipe(board, .8f, .5f, -160, 0, false);
      assertHome();
      onUi(
          () -> {
            if (((Number) field(home, "page")).intValue() != 1)
              throw new AssertionError("Horizontal paging was intercepted by drawer recognition");
            call(ui, "goPage", new Class[] {int.class, boolean.class}, 0, false);
          });
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
      boolean oldDark = ((JSONObject) field(home, "settings")).optBoolean("dark");
      onUi(() -> call(ui, "appearance", new Class[] {}));
      assertText("Classic");
      assertText("Translucent");
      if (!clickSystemText("Dark")) throw new AssertionError("Dark theme choice missing");
      SystemClock.sleep(500);
      if (!((JSONObject) field(home, "settings")).optBoolean("dark"))
        throw new AssertionError("Theme preview did not update Home");
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(500);
      if (((JSONObject) field(home, "settings")).optBoolean("dark") != oldDark)
        throw new AssertionError("Cancelling appearance changed the saved theme");
      // Changing a wallpaper preview must not commit it or change independent appearance.
      String oldPreset = ((JSONObject) field(home, "settings")).optString("wallpaperPreset");
      onUi(() -> call(ui, "wallpaperPresets", new Class[] {}));
      if (!clickSystemText("Ocean")) throw new AssertionError("Wallpaper preview choice missing");
      SystemClock.sleep(300);
      if (!((JSONObject) field(home, "settings")).optString("wallpaperPreset").equals("ocean"))
        throw new AssertionError("Wallpaper preview did not change");
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(400);
      if (!((JSONObject) field(home, "settings")).optString("wallpaperPreset").equals(oldPreset)
          || ((JSONObject) field(home, "settings")).optBoolean("dark") != oldDark)
        throw new AssertionError("Wallpaper cancellation changed committed appearance");
      onUi(() -> call(ui, "customise", new Class[] {}));
      onUi(() -> call(ui, "close", new Class[] {}));
      onUi(() -> call(ui, "overview", new Class[] {}));
      assertText("Home screen");
      screenshot("overview");
      if (!clickSystemText("Select items")) throw new AssertionError("Batch editing entry missing");
      SystemClock.sleep(500);
      assertText("0 selected");
      if (!clickSystemText("0 selected")) throw new AssertionError("Select-all control missing");
      SystemClock.sleep(250);
      Object batch = field(ui, "batch");
      if (((java.util.Set<?>) field(batch, "selected")).isEmpty())
        throw new AssertionError("Batch selection did not select Home items");
      screenshot("batch-selection");
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(400);
      if ((boolean) field(batch, "active"))
        throw new AssertionError("Back did not leave batch editing");
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
      onUi(
          () -> {
            try {
              JSONObject media = new JSONObject().put("type", "media").put("style", 1);
              LinearLayout tileView = new LinearLayout(home);
              tileView.setOrientation(1);
              call(
                  home,
                  "fillTile",
                  new Class[] {LinearLayout.class, JSONObject.class},
                  tileView,
                  media);
              TextView title = (TextView) tileView.getChildAt(0);
              if (title.getCurrentTextColor() != 0xfff0f0e9)
                throw new AssertionError("Inverted media palette was not applied in light mode");
            } catch (JSONException error) {
              throw new RuntimeException(error);
            }
          });
      checkProviderWidget();
      onUi(() -> call(ui, "navigation", new Class[] {}));
      assertText("System navigation");
      assertText("Android handles navigation");
      screenshot("navigation");
      android.content.pm.PackageInfo installed =
          home.getPackageManager()
              .getPackageInfo(
                  "dev.dothome", PackageManager.GET_SERVICES | PackageManager.GET_PERMISSIONS);
      for (android.content.pm.ServiceInfo service : installed.services)
        if ("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(service.permission))
          throw new AssertionError("App still registers an Accessibility service");
      for (String permission : installed.requestedPermissions)
        if (permission.equals("android.permission.SYSTEM_ALERT_WINDOW")
            || permission.equals("android.permission.WRITE_SECURE_SETTINGS"))
          throw new AssertionError("App requests navigation override permission: " + permission);
      // System key events verify the normal Android Home/Back/Recents contract;
      // the app has no service, input injector or global navigation implementation.
      shell("input keyevent KEYCODE_BACK");
      SystemClock.sleep(500);
      assertHome();
      shell("am start -a android.settings.SETTINGS");
      if (!awaitFocus(false)) throw new AssertionError("Settings did not take focus");
      shell("input keyevent KEYCODE_HOME");
      if (!awaitFocus(true)) throw new AssertionError("System Home did not return to Dot Home");
      assertHome();
      screenshot("system-home");
      shell("input keyevent KEYCODE_APP_SWITCH");
      SystemClock.sleep(900);
      assertNoSystemError();
      android.view.accessibility.AccessibilityNodeInfo recentRoot =
          getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
              .getRootInActiveWindow();
      String recentPackage = recentRoot == null ? "" : String.valueOf(recentRoot.getPackageName());
      if (!recentPackage.equals("com.android.launcher3")
          && !recentPackage.equals("com.android.systemui"))
        throw new AssertionError("System Recents surface missing: " + recentPackage);
      if (!awaitFocus(false)) throw new AssertionError("System Recents did not take focus");
      screenshot("system-recents");
      shell("input keyevent KEYCODE_HOME");
      if (!awaitFocus(true)) throw new AssertionError("System Home did not leave Recents");
      screenshot("home-after-navigation");
      result.putString(
          "stream",
          "PASS: home, automatic system Home chooser, real swipe-up drawer from"
              + " workspace/tile/dock, short/cancelled swipe rollback, horizontal paging,"
              + " integrated drawer, visual picker, styles, customisation, overview, folder, normal"
              + " Android Back/Home/Recents, no Accessibility service or global overlay"
              + " permissions, and offline permission.\n");
      result.putString("stream", result.getString("stream", "") + frameReport());
      finish(Activity.RESULT_OK, result);
    } catch (Throwable error) {
      String diagnostics = "";
      try {
        shell("screencap -p /sdcard/Download/failure.png");
        diagnostics =
            systemTree(
                getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
                    .getRootInActiveWindow(),
                0);
      } catch (Throwable ignored) {
      }
      result.putString(
          "stream", "FAIL: " + android.util.Log.getStackTraceString(error) + diagnostics);
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
