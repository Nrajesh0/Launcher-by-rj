package dev.dothome;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.app.usage.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.*;
import android.hardware.*;
import android.hardware.camera2.*;
import android.media.*;
import android.media.session.*;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class HomeActivity extends Activity implements SensorEventListener {
  static final int BIND = 10, CONFIG = 11, PHOTO = 12, WALL = 13, EXPORT = 14, IMPORT = 15;
  static final String STORE = "workspace-v1.json";
  JSONArray items = new JSONArray(), dock = new JSONArray();
  JSONObject settings = new JSONObject();
  final ArrayList<App> apps = new ArrayList<>();
  final ExecutorService worker = Executors.newFixedThreadPool(2);
  final Handler handler = new Handler(Looper.getMainLooper());
  final HashMap<String, String> packMap = new HashMap<>();
  String loadedPack = "";
  final HashMap<String, Drawable> icons = new HashMap<>();
  LinearLayout root, top, bottom;
  LauncherUi ui;
  Board board;
  ScrollView scroll;
  Dialog drawer;
  EditText search;
  AppWidgetHost host;
  int page = 0, pendingWidget = -1;
  String pendingPhoto = null;
  boolean frozen = false, foreground = false, torch = false;
  String movingId = null, lastSavedLayout = "";
  long lastCompassUpdate = 0;
  int fg, bg, card, muted, accent = Color.rgb(218, 74, 61), battery = 0;
  boolean charging = false;
  String agenda = "Tap to connect calendar", usage = "Tap to allow usage access";
  float heading;
  SensorManager sensors;
  Sensor rotation;
  android.content.SharedPreferences prefs;
  final Runnable ticker =
      new Runnable() {
        public void run() {
          if (foreground) {
            refreshData();
            refreshTiles();
            handler.postDelayed(this, 30000);
          }
        }
      };
  final BroadcastReceiver packages =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          loadApps();
        }
      };

  final BroadcastReceiver notificationChanges =
      new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
          if (ui != null) {
            ui.refreshBadges();
            refreshTiles();
          }
        }
      };

  static class App {
    String label, key;
    ComponentName component;
    UserHandle user;
    LauncherActivityInfo info;
  }

  int dp(float x) {
    return Math.round(x * getResources().getDisplayMetrics().density);
  }

  JSONObject obj(String type) {
    JSONObject x = new JSONObject();
    put(x, "id", UUID.randomUUID().toString());
    put(x, "type", type);
    put(x, "page", page);
    put(x, "w", 1);
    put(x, "h", 1);
    return x;
  }

  static void put(JSONObject o, String k, Object v) {
    try {
      o.put(k, v);
    } catch (Exception ignored) {
    }
  }

  boolean dark() {
    return settings.optBoolean("dark", true);
  }

  int cols() {
    return settings.optInt("columns", 4);
  }

  void colors() {
    bg = Color.parseColor(dark() ? "#101010" : "#EAE7E0");
    fg = Color.parseColor(dark() ? "#F1EEE8" : "#191919");
    card = Color.parseColor(dark() ? "#252525" : "#DCD8D0");
    muted = Color.parseColor(dark() ? "#A8A59E" : "#625F59");
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    prefs = getSharedPreferences("runtime", 0);
    android.content.SharedPreferences.Editor cleanup = prefs.edit();
    for (String key : prefs.getAll().keySet()) if (key.startsWith("weather")) cleanup.remove(key);
    cleanup.apply();
    load();
    lastSavedLayout = items.toString();
    colors();
    host = new AppWidgetHost(this, 1024);
    sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
    rotation = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    pendingWidget = prefs.getInt("pendingWidget", -1);
    if (state != null) {
      pendingWidget = state.getInt("pendingWidget", -1);
      pendingPhoto = state.getString("pendingPhoto");
    }
    IntentFilter f = new IntentFilter();
    f.addAction(Intent.ACTION_PACKAGE_ADDED);
    f.addAction(Intent.ACTION_PACKAGE_REMOVED);
    f.addAction(Intent.ACTION_PACKAGE_CHANGED);
    f.addDataScheme("package");
    registerReceiver(packages, f, Context.RECEIVER_EXPORTED);
    registerReceiver(
        notificationChanges,
        new IntentFilter("dev.dothome.NOTIFICATION_CHANGED"),
        Context.RECEIVER_NOT_EXPORTED);
    getOnBackInvokedDispatcher()
        .registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> onBackPressed());
    render();
    if (getIntent().getBooleanExtra("navigationSetup", false)) handler.post(() -> ui.navigation());
    loadApps();
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("pendingWidget", pendingWidget);
    b.putString("pendingPhoto", pendingPhoto);
  }

  @Override
  public void onResume() {
    super.onResume();
    foreground = true;
    try {
      host.startListening();
    } catch (Exception e) {
    }
    if (rotation != null) sensors.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
    colors();
    render();
    handler.removeCallbacks(ticker);
    handler.post(ticker);
  }

  @Override
  public void onPause() {
    super.onPause();
    foreground = false;
    clearMediaCallbacks();
    handler.removeCallbacks(ticker);
    sensors.unregisterListener(this);
    try {
      host.stopListening();
    } catch (Exception e) {
    }
    save();
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(packages);
    unregisterReceiver(notificationChanges);
    worker.shutdownNow();
    super.onDestroy();
  }

  @Override
  public void onNewIntent(Intent i) {
    super.onNewIntent(i);
    if (ui != null) ui.close();
    if (drawer != null) drawer.dismiss();
    page = 0;
    render();
    if (i.getBooleanExtra("navigationSetup", false)) ui.navigation();
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration c) {
    super.onConfigurationChanged(c);
    render();
  }

  @Override
  public void onBackPressed() {
    if (ui != null && ui.back()) return;
    if (drawer != null && drawer.isShowing()) drawer.dismiss();
    else if (movingId != null) {
      movingId = null;
      toast("Move cancelled");
    } else if (page != 0) {
      page = 0;
      render();
    } else if (scroll != null) scroll.smoothScrollTo(0, 0);
  }

  @Override
  public void onSensorChanged(SensorEvent e) {
    float[] r = new float[9], o = new float[3];
    SensorManager.getRotationMatrixFromVector(r, e.values);
    SensorManager.getOrientation(r, o);
    heading = (float) ((Math.toDegrees(o[0]) + 360) % 360);
    if (board != null && android.os.SystemClock.elapsedRealtime() - lastCompassUpdate > 100) {
      lastCompassUpdate = android.os.SystemClock.elapsedRealtime();
      for (int i = 0; i < board.getChildCount(); i++) {
        View v = board.getChildAt(i);
        if (v instanceof LinearLayout
            && v.getTag() instanceof JSONObject
            && ((JSONObject) v.getTag()).optString("type").equals("compass")) {
          LinearLayout tile = (LinearLayout) v;
          if (tile.getChildCount() > 1 && tile.getChildAt(1) instanceof TextView)
            ((TextView) tile.getChildAt(1))
                .setText(
                    Math.round(heading)
                        + "°  "
                        + new String[] {"N", "NE", "E", "SE", "S", "SW", "W", "NW"}
                            [Math.round(heading / 45) % 8]);
        }
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor s, int a) {}

  void load() {
    try {
      JSONObject data =
          new JSONObject(
              new String(
                  java.nio.file.Files.readAllBytes(new File(getFilesDir(), STORE).toPath()),
                  java.nio.charset.StandardCharsets.UTF_8));
      validateBackup(data);
      OfflineMigration.apply(data);
      items = data.getJSONArray("items");
      settings = data.getJSONObject("settings");
      dock = data.optJSONArray("dock");
      if (dock == null) dock = new JSONArray();
    } catch (Exception e) {
      try {
        File original = new File(getFilesDir(), STORE);
        if (original.exists())
          java.nio.file.Files.copy(
              original.toPath(),
              new File(getFilesDir(), "unreadable-layout.json").toPath(),
              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception ignored) {
      }
      defaults();
    }
  }

  void defaults() {
    items = new JSONArray();
    settings = new JSONObject();
    dock = new JSONArray();
    put(settings, "wallpaperPreset", "ocean");
    put(settings, "labels", false);
    referenceItems(0);
  }

  void referenceItems(int targetPage) {
    String[] types = {"clock", "date", "breathing", "photo", "torch", "battery"};
    int[][] cells = {
      {0, 0, 2, 2}, {2, 0, 2, 1}, {2, 3, 2, 2}, {0, 3, 1, 1}, {1, 3, 1, 1}, {0, 4, 2, 1}
    };
    for (int n = 0; n < types.length; n++) {
      JSONObject x = obj(types[n]);
      put(x, "page", targetPage);
      put(x, "x", cells[n][0]);
      put(x, "y", cells[n][1]);
      put(x, "w", cells[n][2]);
      put(x, "h", cells[n][3]);
      put(x, "style", n == 0 ? 1 : n == 1 ? 2 : n == 5 ? 1 : 0);
      put(x, "opacity", 85);
      items.put(x);
    }
  }

  void save() {
    try {
      String current = items.toString();
      if (!lastSavedLayout.isEmpty() && !lastSavedLayout.equals(current)) {
        prefs.edit().putString("undo", lastSavedLayout).apply();
        lastSavedLayout = current;
      }
      JSONObject x = new JSONObject();
      put(x, "schema", 2);
      put(x, "items", items);
      put(x, "settings", settings);
      put(x, "dock", dock);
      android.util.AtomicFile file = new android.util.AtomicFile(new File(getFilesDir(), STORE));
      FileOutputStream out = null;
      try {
        out = file.startWrite();
        out.write(x.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        file.finishWrite(out);
      } catch (Exception e) {
        file.failWrite(out);
        throw e;
      }
    } catch (Exception e) {
      toast("Couldn't save layout. Check available storage.");
    }
  }

  void loadApps() {
    worker.execute(
        () -> {
          ArrayList<App> result = new ArrayList<>();
          try {
            LauncherApps la = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
            UserManager um = (UserManager) getSystemService(USER_SERVICE);
            for (UserHandle user : la.getProfiles()) {
              for (LauncherActivityInfo i : la.getActivityList(null, user)) {
                if (i.getComponentName().getPackageName().equals(getPackageName())) continue;
                App a = new App();
                a.info = i;
                a.label = i.getLabel().toString();
                a.component = i.getComponentName();
                a.user = user;
                a.key = a.component.flattenToString() + "@" + um.getSerialNumberForUser(user);
                result.add(a);
              }
            }
            result.sort((a, b) -> java.text.Collator.getInstance().compare(a.label, b.label));
          } catch (Exception e) {
          }
          runOnUiThread(
              () -> {
                apps.clear();
                apps.addAll(result);
                icons.clear();
                if (!settings.optBoolean("dockInitialised", false)) {
                  String[] categories = {
                    Intent.CATEGORY_APP_CONTACTS,
                    Intent.CATEGORY_APP_MESSAGING,
                    "camera",
                    Intent.CATEGORY_APP_BROWSER
                  };
                  for (int slot = 0; slot < 4; slot++) {
                    Intent intent =
                        slot == 0
                            ? new Intent(Intent.ACTION_DIAL)
                            : slot == 2
                                ? new Intent(
                                    android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                                : new Intent(Intent.ACTION_MAIN).addCategory(categories[slot]);
                    android.content.pm.ResolveInfo resolved =
                        getPackageManager()
                            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    App chosen = null;
                    if (resolved != null && resolved.activityInfo != null)
                      for (App app : apps)
                        if (app.component
                            .getPackageName()
                            .equals(resolved.activityInfo.packageName)) {
                          chosen = app;
                          break;
                        }
                    if (chosen != null && dock.optString(slot, "").isEmpty())
                      try {
                        dock.put(slot, chosen.key);
                      } catch (Exception ignored) {
                      }
                  }
                  put(settings, "dockInitialised", true);
                  save();
                }
                render();
              });
        });
  }

  App app(String key) {
    for (App a : apps) if (a.key.equals(key)) return a;
    return null;
  }

  void launch(String key) {
    App a = app(key);
    if (a == null) {
      toast("App unavailable in this profile");
      return;
    }
    try {
      ((LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE))
          .startMainActivity(a.component, a.user, null, null);
    } catch (Exception e) {
      toast("Unable to launch. The profile may be paused.");
    }
  }

  Drawable icon(App a) {
    Drawable d = icons.get(a.key);
    if (d == null) {
      try {
        String pack = settings.optString("iconPack", "");
        if (!pack.equals(loadedPack)) {
          packMap.clear();
          loadedPack = pack;
          try {
            android.content.res.Resources res =
                getPackageManager().getResourcesForApplication(pack);
            int xml = res.getIdentifier("appfilter", "xml", pack);
            if (xml != 0) {
              android.content.res.XmlResourceParser parser = res.getXml(xml);
              while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT)
                if (parser.getEventType() == org.xmlpull.v1.XmlPullParser.START_TAG
                    && parser.getName().equals("item")) {
                  String component = parser.getAttributeValue(null, "component"),
                      drawable = parser.getAttributeValue(null, "drawable");
                  if (component != null && drawable != null) packMap.put(component, drawable);
                }
              parser.close();
            }
          } catch (Exception ignored) {
          }
        }
        String drawable = packMap.get("ComponentInfo{" + a.component.flattenToString() + "}");
        if (drawable != null) {
          android.content.res.Resources resources =
              getPackageManager().getResourcesForApplication(pack);
          int id = resources.getIdentifier(drawable, "drawable", pack);
          if (id != 0) d = resources.getDrawable(id, null).mutate();
        }
        if (d == null)
          d = a.info.getBadgedIcon(getResources().getDisplayMetrics().densityDpi).mutate();
      } catch (Exception e) {
        d = getPackageManager().getDefaultActivityIcon().mutate();
      }
      icons.put(a.key, d);
    }
    if (settings.optBoolean("mono", true)) {
      if (d instanceof AdaptiveIconDrawable) {
        Drawable mono = ((AdaptiveIconDrawable) d).getMonochrome();
        if (mono != null) {
          Bitmap bitmap = Bitmap.createBitmap(dp(64), dp(64), Bitmap.Config.ARGB_8888);
          Canvas canvas = new Canvas(bitmap);
          Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
          paint.setColor(fg);
          canvas.drawCircle(dp(32), dp(32), dp(30), paint);
          mono = mono.mutate();
          mono.setTint(bg);
          mono.setBounds(dp(10), dp(10), dp(54), dp(54));
          mono.draw(canvas);
          return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
        }
      }
      ColorMatrix cm = new ColorMatrix();
      cm.setSaturation(0);
      d.setColorFilter(new ColorMatrixColorFilter(cm));
      Bitmap bitmap = Bitmap.createBitmap(dp(64), dp(64), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(fg);
      canvas.drawCircle(dp(32), dp(32), dp(30), paint);
      d.setBounds(dp(14), dp(14), dp(50), dp(50));
      d.draw(canvas);
      return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    } else d.clearColorFilter();
    return d;
  }

  TextView text(String s, float size, int color) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setGravity(Gravity.CENTER_VERTICAL);
    t.setFontFeatureSettings("tnum");
    return t;
  }

  TextView label(String s) {
    TextView t = text(s, 11, muted);
    t.setLetterSpacing(.13f);
    t.setTypeface(Typeface.MONOSPACE);
    return t;
  }

  GradientDrawable shape(int color, float radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  void click(View v, Runnable action) {
    v.setOnTouchListener(
        (view, event) -> {
          if (ui == null || ui.duration() == 0) return false;
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              view.animate().scaleX(.96f).scaleY(.96f).setDuration(90).start();
              break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
              view.animate().scaleX(1).scaleY(1).setDuration(140).start();
              break;
          }
          return false;
        });
    v.setOnClickListener(
        w -> {
          w.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
          action.run();
        });
  }

  void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  void open(Intent i) {
    try {
      startActivity(i);
    } catch (Exception e) {
      toast("This action is unavailable on this device");
    }
  }

  void render() {
    if (isFinishing()) return;
    if (ui == null) ui = new LauncherUi(this);
    ui.refresh();
  }

  View appView(App a, boolean withLabel) {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(1);
    box.setGravity(Gravity.CENTER);
    ImageView im = new ImageView(this);
    im.setImageDrawable(icon(a));
    int size = dp(settings.optInt("iconSize", 46));
    FrameLayout iconFrame = new FrameLayout(this);
    im.setScaleType(ImageView.ScaleType.FIT_CENTER);
    iconFrame.addView(im, new FrameLayout.LayoutParams(-1, -1));
    View badge = new View(this);
    badge.setTag("badge:" + a.component.getPackageName());
    badge.setBackground(shape(accent, 100));
    FrameLayout.LayoutParams badgePosition =
        new FrameLayout.LayoutParams(dp(8), dp(8), Gravity.TOP | Gravity.END);
    iconFrame.addView(badge, badgePosition);
    badge.setVisibility(
        settings.optBoolean("notificationDots", true)
                && prefs
                    .getStringSet("badgePackages", java.util.Collections.emptySet())
                    .contains(a.component.getPackageName())
            ? View.VISIBLE
            : View.GONE);
    box.addView(iconFrame, new LinearLayout.LayoutParams(size, size));
    if (withLabel) {
      TextView title = text(a.label, 11, fg);
      title.setGravity(Gravity.CENTER);
      title.setSingleLine(true);
      title.setEllipsize(android.text.TextUtils.TruncateAt.END);
      box.addView(title, new LinearLayout.LayoutParams(-1, dp(22)));
    }
    box.setContentDescription(a.label);
    click(box, () -> launch(a.key));
    return box;
  }

  class Board extends ViewGroup {
    final int boardPage;
    ArrayList<JSONObject> visible = new ArrayList<>();
    int hoverX = -1, hoverY = -1, resizeW = 1, resizeH = 1;
    float downX, downY;
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Handler touchHandler = new Handler(Looper.getMainLooper());
    boolean longPressed, resizeTouch;
    final Runnable hold =
        () -> {
          longPressed = true;
          ui.overview();
        };

    Board(int page) {
      super(HomeActivity.this);
      boardPage = page;
      setWillNotDraw(false);
      for (int i = 0; i < items.length(); i++) {
        JSONObject x = items.optJSONObject(i);
        if (x != null && x.optInt("page") == page) {
          visible.add(x);
          addView(itemView(x));
        }
      }
    }

    int cw() {
      return Math.max(1, getWidth() / cols());
    }

    int ch() {
      return Math.max(1, getHeight() / ui.rows());
    }

    @Override
    protected void onMeasure(int ws, int hs) {
      int width = MeasureSpec.getSize(ws), height = MeasureSpec.getSize(hs);
      setMeasuredDimension(width, height);
      for (int i = 0; i < getChildCount(); i++) {
        JSONObject x = visible.get(i);
        View child = getChildAt(i);
        int itemWidth = Math.max(1, x.optInt("w", 1) * width / cols() - dp(10));
        int itemHeight = Math.max(1, x.optInt("h", 1) * height / ui.rows() - dp(10));
        if (x.optString("type").equals("app") && child instanceof LinearLayout) {
          LinearLayout iconBox = (LinearLayout) child;
          if (iconBox.getChildAt(0) instanceof FrameLayout) {
            int labelHeight = iconBox.getChildCount() > 1 ? dp(22) : 0;
            int iconSize =
                x.optInt("w", 1) > 1 && x.optInt("h", 1) > 1
                    ? (int) Math.min(itemWidth * .76f, itemHeight - labelHeight - dp(12))
                    : dp(settings.optInt("iconSize", 46));
            iconSize =
                Math.max(1, Math.min(iconSize, Math.min(itemWidth, itemHeight - labelHeight)));
            View iconFrame = iconBox.getChildAt(0);
            ViewGroup.LayoutParams old = iconFrame.getLayoutParams();
            if (old.width != iconSize || old.height != iconSize)
              iconFrame.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
          }
        }
        getChildAt(i)
            .measure(
                MeasureSpec.makeMeasureSpec(
                    Math.max(1, x.optInt("w", 1) * width / cols() - dp(10)), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(
                    Math.max(1, x.optInt("h", 1) * height / ui.rows() - dp(10)),
                    MeasureSpec.EXACTLY));
      }
    }

    @Override
    protected void onLayout(boolean c, int l, int t, int r, int b) {
      for (int i = 0; i < getChildCount(); i++) {
        JSONObject x = visible.get(i);
        View view = getChildAt(i);
        int left = x.optInt("x") * cw() + dp(5), top = x.optInt("y") * ch() + dp(5);
        view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
      }
    }

    @Override
    protected void onDraw(Canvas c) {
      if (ui.resizing != null && ui.resizing.optInt("page") == boardPage) {
        JSONObject x = ui.resizing;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(accent);
        RectF outline =
            new RectF(
                x.optInt("x") * cw() + dp(3),
                x.optInt("y") * ch() + dp(3),
                (x.optInt("x") + x.optInt("w", 1)) * cw() - dp(3),
                (x.optInt("y") + x.optInt("h", 1)) * ch() - dp(3));
        c.drawRoundRect(outline, dp(24), dp(24), paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(outline.right, outline.bottom, dp(8), paint);
      }
      if (ui.dragging != null) {
        paint.setColor(0x447f7f7f);
        for (int y = 0; y < ui.rows(); y++)
          for (int x = 0; x < cols(); x++)
            c.drawCircle((x + .5f) * cw(), (y + .5f) * ch(), dp(2), paint);
        if (hoverX >= 0) {
          JSONObject moving = ui.dragging;
          boolean fits =
              WorkspaceModel.fits(
                  items,
                  ui.dragNew ? null : moving.optString("id"),
                  boardPage,
                  hoverX,
                  hoverY,
                  moving.optInt("w", 1),
                  moving.optInt("h", 1),
                  cols(),
                  ui.rows());
          paint.setColor(fits ? 0x557fba91 : 0x55da4a3d);
          c.drawRoundRect(
              new RectF(
                  hoverX * cw() + dp(5),
                  hoverY * ch() + dp(5),
                  (hoverX + moving.optInt("w", 1)) * cw() - dp(5),
                  (hoverY + moving.optInt("h", 1)) * ch() - dp(5)),
              dp(22),
              dp(22),
              paint);
        }
      }
    }

    @Override
    public boolean onDragEvent(DragEvent e) {
      if (e.getAction() == DragEvent.ACTION_DRAG_STARTED)
        return e.getLocalState() instanceof JSONObject;
      if (ui.dragging == null) return false;
      switch (e.getAction()) {
        case DragEvent.ACTION_DRAG_LOCATION:
          hoverX = Math.max(0, Math.min(cols() - 1, (int) e.getX() / cw()));
          hoverY = Math.max(0, Math.min(ui.rows() - 1, (int) e.getY() / ch()));
          invalidate();
          long now = android.os.SystemClock.elapsedRealtime();
          if (now - ui.edgeAt > 800) {
            if (e.getX() > getWidth() - dp(24) && boardPage < ui.pages() - 1) {
              ui.edgeAt = now;
              ui.goPage(boardPage + 1, true);
            } else if (e.getX() < dp(24) && boardPage > 0) {
              ui.edgeAt = now;
              ui.goPage(boardPage - 1, true);
            }
          }
          return true;
        case DragEvent.ACTION_DROP:
          JSONObject target = null;
          for (JSONObject x : visible)
            if (e.getX() >= x.optInt("x") * cw()
                && e.getX() < (x.optInt("x") + x.optInt("w", 1)) * cw()
                && e.getY() >= x.optInt("y") * ch()
                && e.getY() < (x.optInt("y") + x.optInt("h", 1)) * ch()) target = x;
          return ui.drop(
              boardPage,
              Math.max(0, (int) e.getX() / cw()),
              Math.max(0, (int) e.getY() / ch()),
              target);
        case DragEvent.ACTION_DRAG_EXITED:
        case DragEvent.ACTION_DRAG_ENDED:
          hoverX = -1;
          invalidate();
          return true;
      }
      return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
      return ui.resizing != null && ui.resizing.optInt("page") == boardPage;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (ui.resizing != null && ui.resizing.optInt("page") == boardPage) {
        JSONObject x = ui.resizing;
        if (e.getAction() == MotionEvent.ACTION_DOWN)
          resizeTouch =
              Math.abs(e.getX() - (x.optInt("x") + x.optInt("w", 1)) * cw()) < dp(32)
                  && Math.abs(e.getY() - (x.optInt("y") + x.optInt("h", 1)) * ch()) < dp(32);
        if (!resizeTouch) return true;
        resizeW =
            Math.max(
                1, Math.min(cols() - x.optInt("x"), Math.round(e.getX() / cw()) - x.optInt("x")));
        resizeH =
            Math.max(
                1,
                Math.min(ui.rows() - x.optInt("y"), Math.round(e.getY() / ch()) - x.optInt("y")));
        int[] allowed = constrainedSize(x, resizeW, resizeH);
        resizeW = allowed[0];
        resizeH = allowed[1];
        if (e.getAction() == MotionEvent.ACTION_UP) {
          if (WorkspaceModel.fits(
              items,
              x.optString("id"),
              boardPage,
              x.optInt("x"),
              x.optInt("y"),
              resizeW,
              resizeH,
              cols(),
              ui.rows())) {
            put(x, "w", resizeW);
            put(x, "h", resizeH);
            ui.resizing = null;
            save();
            render();
          } else toast("No room for that size");
        } else if (e.getAction() == MotionEvent.ACTION_CANCEL) ui.resizing = null;
        invalidate();
        return true;
      }
      switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
          downX = e.getX();
          downY = e.getY();
          longPressed = false;
          touchHandler.postDelayed(hold, ViewConfiguration.getLongPressTimeout());
          return true;
        case MotionEvent.ACTION_MOVE:
          if (Math.abs(e.getX() - downX) > dp(12) || Math.abs(e.getY() - downY) > dp(12))
            touchHandler.removeCallbacks(hold);
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          touchHandler.removeCallbacks(hold);
          return true;
      }
      return true;
    }
  }

  int index(String id) {
    for (int i = 0; i < items.length(); i++)
      if (id.equals(items.optJSONObject(i).optString("id"))) return i;
    return -1;
  }

  View itemView(JSONObject x) {
    String type = x.optString("type");
    View v;
    if (type.equals("app")) {
      App a = app(x.optString("key"));
      v =
          a == null
              ? text(x.optString("label", "App unavailable"), 12, muted)
              : appView(a, settings.optBoolean("labels", true));
    } else if (type.equals("folder")) {
      LinearLayout box = new LinearLayout(this);
      box.setOrientation(1);
      box.setGravity(Gravity.CENTER);
      box.setBackground(shape(card, x.optInt("w", 1) == 1 ? 100 : 28));
      String cover = x.optString("cover", "");
      JSONArray children = x.optJSONArray("children");
      boolean large = x.optInt("w", 1) > 1;
      if (!cover.isEmpty()) {
        TextView symbol = text(cover, large ? 54 : 28, fg);
        symbol.setGravity(Gravity.CENTER);
        box.addView(symbol, new LinearLayout.LayoutParams(-1, 0, 1));
        click(symbol, () -> folder(x));
      } else if (large) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        if (children != null)
          for (int i = 0; i < Math.min(4, children.length()); i++) {
            App app = app(children.optString(i));
            View child = app == null ? text("○", 22, fg) : appView(app, false);
            if (child instanceof LinearLayout && ((LinearLayout) child).getChildAt(0) != null)
              ((LinearLayout) child)
                  .getChildAt(0)
                  .setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));
            grid.addView(child, new ViewGroup.LayoutParams(dp(52), dp(52)));
          }
        box.addView(grid, new LinearLayout.LayoutParams(-2, 0, 1));
      } else {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        if (children != null)
          for (int n = 0; n < Math.min(4, children.length()); n++) {
            App app = app(children.optString(n));
            ImageView image = new ImageView(this);
            if (app != null) image.setImageDrawable(icon(app));
            grid.addView(image, new ViewGroup.LayoutParams(dp(20), dp(20)));
          }
        box.addView(grid);
        click(grid, () -> folder(x));
      }
      TextView title = text(x.optString("name", "Folder"), 11, fg);
      title.setGravity(Gravity.CENTER);
      title.setSingleLine();
      box.addView(title, new LinearLayout.LayoutParams(-1, dp(24)));
      click(title, () -> folder(x));
      box.setContentDescription(x.optString("name", "Folder"));
      if (children == null || children.length() == 0) click(box, () -> folder(x));
      v = box;
    } else if (type.equals("widget")) {
      int id = x.optInt("widgetId", -1);
      AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id);
      if (info == null) {
        v = text("Widget unavailable\nLong press to replace", 13, muted);
      } else {
        try {
          AppWidgetHostView hv = host.createView(this, id, info);
          hv.setAppWidget(id, info);
          hv.setOnLongClickListener(
              w -> {
                ui.beginDrag(hv, x, false);
                return true;
              });
          hv.addOnLayoutChangeListener(
              (w, l, t, r, b, ol, ot, or, ob) -> {
                if (r - l != or - ol || b - t != ob - ot)
                  hv.updateAppWidgetSize(
                      null,
                      (int) ((r - l) / getResources().getDisplayMetrics().density),
                      (int) ((b - t) / getResources().getDisplayMetrics().density),
                      (int) ((r - l) / getResources().getDisplayMetrics().density),
                      (int) ((b - t) / getResources().getDisplayMetrics().density));
              });
          v = hv;
        } catch (Exception e) {
          v = text("Widget couldn't load", 13, muted);
        }
      }
    } else {
      LinearLayout tile = new LinearLayout(this);
      tile.setOrientation(1);
      tile.setPadding(dp(16), dp(12), dp(16), dp(12));
      tile.setBackground(shape(type.equals("battery") ? fg : card, 26));
      tile.setTag(x);
      fillTile(tile, x);
      v = tile;
    }
    if (!type.equals("widget"))
      v.setOnLongClickListener(
          w -> {
            ui.beginDrag(w, x, false);
            return true;
          });
    return v;
  }

  void fillTile(LinearLayout tile, JSONObject x) {
    tile.removeAllViews();
    String type = x.optString("type");
    if ((!type.equals("media") && !type.equals("photo"))
        || (type.equals("photo")
            && x.optString("uri").isEmpty()
            && x.optInt("w", 1) == 1
            && x.optInt("h", 1) == 1)) {
      tile.setPadding(0, 0, 0, 0);
      tile.setBackgroundColor(Color.TRANSPARENT);
      WidgetArtwork art = new WidgetArtwork(this, x, false);
      tile.addView(art, new LinearLayout.LayoutParams(-1, -1));
      click(tile, () -> tileAction(x));
      return;
    }
    tile.setPadding(dp(14), dp(12), dp(14), dp(12));
    int ink = type.equals("battery") ? bg : fg;
    if (type.equals("clock")) {
      DotClock clock = new DotClock();
      tile.addView(clock, new LinearLayout.LayoutParams(-1, 0, 1));
      TextView date =
          label(
              new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
                  .format(new Date())
                  .toUpperCase());
      tile.addView(date);
      click(tile, () -> open(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
      return;
    }
    String title = type.toUpperCase(Locale.ROOT), value = "";
    switch (type) {
      case "date":
        title = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date()).toUpperCase();
        value = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());
        break;
      case "battery":
        title = charging ? "CHARGING" : "BATTERY";
        value = battery + "%";
        break;
      case "notes":
        title = "NOTE";
        value = x.optString("text", "Tap to write a note");
        break;
      case "agenda":
        title = "UP NEXT";
        value = agenda;
        break;
      case "countdown":
        title = x.optString("name", "COUNTDOWN");
        long delta = x.optLong("deadline") - System.currentTimeMillis();
        value =
            x.optLong("deadline") == 0
                ? "Set a date"
                : delta <= 0 ? "Today" : ((delta + 86399999) / 86400000) + " days";
        break;
      case "compass":
        title = "COMPASS";
        value =
            rotation == null
                ? "Sensor unavailable"
                : Math.round(heading)
                    + "°  "
                    + new String[] {"N", "NE", "E", "SE", "S", "SW", "W", "NW"}
                        [(Math.round(heading / 45)) % 8];
        break;
      case "usage":
        title = "SCREEN TIME";
        value = usage;
        break;
      case "controls":
        title = "QUICK CONTROLS";
        value = "Bluetooth · Torch · Sound";
        break;
      case "media":
        fillMedia(tile);
        return;
      case "photo":
        String uri = x.optString("uri");
        if (!uri.isEmpty()) {
          ImageView im =
              new ImageView(this) {
                @Override
                protected void onSizeChanged(int w, int h, int oldW, int oldH) {
                  super.onSizeChanged(w, h, oldW, oldH);
                  if (x.optInt("style") == 2) return;
                  Drawable image = getDrawable();
                  if (image == null
                      || image.getIntrinsicWidth() <= 0
                      || image.getIntrinsicHeight() <= 0) return;
                  float scale =
                      Math.max(
                          w / (float) image.getIntrinsicWidth(),
                          h / (float) image.getIntrinsicHeight());
                  Matrix matrix = new Matrix();
                  matrix.setScale(scale, scale);
                  matrix.postTranslate(
                      (w - image.getIntrinsicWidth() * scale) / 2f,
                      (h - image.getIntrinsicHeight() * scale)
                          * Math.max(0, Math.min(2, x.optInt("crop", 1)))
                          / 2f);
                  setImageMatrix(matrix);
                }
              };
          im.setScaleType(
              x.optInt("style") == 2 ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.MATRIX);
          im.setBackground(
              shape(
                  x.optInt("style") == 2 ? Color.TRANSPARENT : card,
                  x.optInt("style") == 1 ? 1000 : 24));
          if (x.optInt("style") == 2) {
            tile.setPadding(0, 0, 0, 0);
            tile.setBackgroundColor(Color.TRANSPARENT);
          }
          im.setClipToOutline(true);
          try {
            im.setImageDrawable(scaledImage(Uri.parse(uri)));
            tile.addView(im, new LinearLayout.LayoutParams(-1, -1));
            click(tile, () -> pickPhoto(x));
            return;
          } catch (Exception e) {
          }
        }
        title = "PHOTO";
        value = "Choose a photo";
        break;
    }
    TextView small = label(title);
    small.setTextColor(type.equals("battery") ? bg : muted);
    tile.addView(small);
    TextView large = text(value, type.equals("date") || type.equals("battery") ? 27 : 15, ink);
    large.setMaxLines(3);
    large.setEllipsize(android.text.TextUtils.TruncateAt.END);
    tile.addView(large, new LinearLayout.LayoutParams(-1, 0, 1));
    click(tile, () -> tileAction(x));
  }

  void refreshTiles() {
    if (ui == null) return;
    for (Board b : ui.boards)
      for (int i = 0; i < b.getChildCount(); i++) {
        View v = b.getChildAt(i);
        if (v instanceof LinearLayout && v.getTag() instanceof JSONObject)
          fillTile((LinearLayout) v, (JSONObject) v.getTag());
      }
  }

  void tileAction(JSONObject x) {
    switch (x.optString("type")) {
      case "notes":
        input(
            "Edit note",
            x.optString("text"),
            s -> {
              put(x, "text", s);
              save();
              render();
            });
        break;
      case "agenda":
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED)
          requestPermissions(new String[] {Manifest.permission.READ_CALENDAR}, 21);
        else open(new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI));
        break;
      case "countdown":
        input(
            "Countdown title",
            x.optString("name", "A good day"),
            s -> {
              put(x, "name", s);
              Calendar c = Calendar.getInstance();
              new DatePickerDialog(
                      this,
                      (v, y, m, d) -> {
                        c.set(y, m, d, 0, 0, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        put(x, "deadline", c.getTimeInMillis());
                        save();
                        render();
                      },
                      c.get(Calendar.YEAR),
                      c.get(Calendar.MONTH),
                      c.get(Calendar.DAY_OF_MONTH))
                  .show();
            });
        break;
      case "controls":
        controls();
        break;
      case "photo":
        pickPhoto(x);
        break;
      case "usage":
        open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        break;
      case "date":
        open(
            new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis())));
        break;
      case "breathing":
        toast("Follow the circle: breathe slowly in and out");
        break;
      case "torch":
        toggleTorch();
        break;
      case "bluetooth":
        open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        break;
      case "sound":
        open(new Intent(Settings.ACTION_SOUND_SETTINGS));
        break;
      case "dnd":
        open(new Intent("android.settings.ZEN_MODE_SETTINGS"));
        break;
      case "battery":
        open(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        break;
      default:
        toast("Long press for widget options");
    }
  }

  final java.util.HashMap<
          android.media.session.MediaController, android.media.session.MediaController.Callback>
      mediaCallbacks = new java.util.HashMap<>();

  void clearMediaCallbacks() {
    for (java.util.Map.Entry<
            android.media.session.MediaController, android.media.session.MediaController.Callback>
        e : mediaCallbacks.entrySet())
      try {
        e.getKey().unregisterCallback(e.getValue());
      } catch (Exception ignored) {
      }
    mediaCallbacks.clear();
  }

  void fillMedia(LinearLayout tile) {
    tile.addView(label("NOW PLAYING"));
    MediaController controller = null;
    try {
      MediaSessionManager mm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
      List<MediaController> cs = mm.getActiveSessions(new ComponentName(this, MediaAccess.class));
      for (MediaController c : cs) {
        if (controller == null) controller = c;
        if (c.getPlaybackState() != null
            && c.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
          controller = c;
          break;
        }
      }
    } catch (SecurityException ignored) {
    }
    if (controller == null) {
      tile.addView(text("Connect your music", 17, fg), new LinearLayout.LayoutParams(-1, 0, 1));
      click(tile, () -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
      return;
    }
    final MediaController c = controller;
    boolean observed = false;
    for (MediaController registered : mediaCallbacks.keySet())
      if (registered.controlsSameSession(c)) {
        observed = true;
        break;
      }
    if (!observed) {
      if (mediaCallbacks.size() >= 8) clearMediaCallbacks();
      MediaController.Callback callback =
          new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
              handler.post(() -> refreshTiles());
            }

            @Override
            public void onMetadataChanged(MediaMetadata data) {
              handler.post(() -> refreshTiles());
            }
          };
      c.registerCallback(callback, handler);
      mediaCallbacks.put(c, callback);
    }
    MediaMetadata data = c.getMetadata();
    if (data != null) {
      Bitmap art = data.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
      if (art == null) art = data.getBitmap(MediaMetadata.METADATA_KEY_ART);
      LinearLayout row = new LinearLayout(this);
      row.setGravity(Gravity.CENTER_VERTICAL);
      ImageView image = new ImageView(this);
      image.setScaleType(ImageView.ScaleType.CENTER_CROP);
      image.setBackground(shape(bg, 14));
      image.setClipToOutline(true);
      if (art != null) image.setImageBitmap(art);
      else image.setImageResource(android.R.drawable.ic_media_play);
      row.addView(image, new LinearLayout.LayoutParams(dp(48), dp(48)));
      TextView artist =
          text(
              String.valueOf(
                  data.getText(MediaMetadata.METADATA_KEY_ARTIST) == null
                      ? "Music"
                      : data.getText(MediaMetadata.METADATA_KEY_ARTIST)),
              12,
              muted);
      artist.setPadding(dp(12), 0, 0, 0);
      artist.setMaxLines(2);
      row.addView(artist, new LinearLayout.LayoutParams(0, dp(48), 1));
      tile.addView(row);
    }

    String title =
        data == null ? "Media" : String.valueOf(data.getText(MediaMetadata.METADATA_KEY_TITLE));
    TextView name = text(title, 16, fg);
    name.setMaxLines(2);
    tile.addView(name, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout buttons = new LinearLayout(this);
    String[] names = {
      "‹",
      c.getPlaybackState() != null && c.getPlaybackState().getState() == PlaybackState.STATE_PLAYING
          ? "Ⅱ"
          : "▶",
      "›"
    };
    for (int n = 0; n < 3; n++) {
      final int action = n;
      TextView b = text(names[n], 22, fg);
      b.setGravity(Gravity.CENTER);
      buttons.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1));
      click(
          b,
          () -> {
            MediaController.TransportControls tc = c.getTransportControls();
            if (action == 0) tc.skipToPrevious();
            else if (action == 2) tc.skipToNext();
            else if (c.getPlaybackState() != null
                && c.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) tc.pause();
            else tc.play();
            handler.postDelayed(() -> refreshTiles(), 250);
          });
    }
    tile.addView(buttons);
  }

  void refreshData() {
    Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (b != null) {
      battery =
          b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
              * 100
              / Math.max(1, b.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
      int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
      charging =
          status == BatteryManager.BATTERY_STATUS_CHARGING
              || status == BatteryManager.BATTERY_STATUS_FULL;
    }
    worker.execute(
        () -> {
          String next = "No upcoming events";
          if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
              == PackageManager.PERMISSION_GRANTED) {
            long now = System.currentTimeMillis();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, now);
            ContentUris.appendId(builder, now + 86400000L * 7);
            try (Cursor c =
                getContentResolver()
                    .query(
                        builder.build(),
                        new String[] {
                          CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN
                        },
                        null,
                        null,
                        CalendarContract.Instances.BEGIN + " ASC")) {
              if (c != null && c.moveToFirst())
                next =
                    new SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                            .format(new Date(c.getLong(1)))
                        + "\n"
                        + c.getString(0);
            } catch (Exception e) {
              next = "Calendar unavailable";
            }
          } else next = "Tap to connect calendar";
          agenda = next;
          try {
            android.app.AppOpsManager ops =
                (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE);
            if (ops.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    getPackageName())
                == android.app.AppOpsManager.MODE_ALLOWED) {
              Calendar midnight = Calendar.getInstance();
              midnight.set(Calendar.HOUR_OF_DAY, 0);
              midnight.set(Calendar.MINUTE, 0);
              midnight.set(Calendar.SECOND, 0);
              midnight.set(Calendar.MILLISECOND, 0);
              long total = 0;
              Map<String, UsageStats> stats =
                  ((UsageStatsManager) getSystemService(USAGE_STATS_SERVICE))
                      .queryAndAggregateUsageStats(
                          midnight.getTimeInMillis(), System.currentTimeMillis());
              for (UsageStats s : stats.values()) total += s.getTotalTimeInForeground();
              usage = (total / 3600000) + "h " + (total / 60000 % 60) + "m · app usage";
            }
          } catch (Exception ignored) {
          }
          runOnUiThread(() -> refreshTiles());
        });
  }

  Drawable scaledImage(Uri uri) throws Exception {
    return android.graphics.ImageDecoder.decodeDrawable(
        android.graphics.ImageDecoder.createSource(getContentResolver(), uri),
        (decoder, info, source) -> {
          int sample =
              Math.max(1, Math.max(info.getSize().getWidth(), info.getSize().getHeight()) / 1600);
          decoder.setTargetSampleSize(sample);
        });
  }

  void input(String title, String initial, java.util.function.Consumer<String> done) {
    EditText e = new EditText(this);
    e.setText(initial);
    e.setSelectAllOnFocus(true);
    e.setPadding(dp(20), dp(12), dp(20), dp(12));
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(e)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Save", (d, w) -> done.accept(e.getText().toString().trim()))
        .show();
  }

  void menu(String title, String[] options, java.util.function.IntConsumer choice) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(options, (d, n) -> choice.accept(n))
        .setNegativeButton("Close", null)
        .show();
  }

  void addMenu() {
    ui.overview();
  }

  void addTile() {
    ui.widgetPicker();
  }

  void pickWidget() {
    ui.widgetPicker();
  }

  void itemMenu(JSONObject x, View v) {
    ui.itemActions(x, v);
  }

  void folder(JSONObject x) {
    ui.folder(x);
  }

  void drawer(java.util.function.Consumer<App> pick) {
    ui.drawer(pick);
  }

  void settings() {
    ui.customise();
  }

  void appOptions(App a) {
    if (a == null) return;
    ArrayList<String> names = new ArrayList<>();
    names.add("App information");
    ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
    LauncherApps la = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
    try {
      LauncherApps.ShortcutQuery q =
          new LauncherApps.ShortcutQuery()
              .setPackage(a.component.getPackageName())
              .setQueryFlags(
                  LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                      | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                      | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
      List<ShortcutInfo> ss = la.getShortcuts(q, a.user);
      if (ss != null)
        for (ShortcutInfo s : ss)
          if (s.isEnabled()) {
            shortcuts.add(s);
            names.add(String.valueOf(s.getShortLabel()));
          }
    } catch (Exception ignored) {
    }
    menu(
        a.label,
        names.toArray(new String[0]),
        n -> {
          try {
            if (n == 0) la.startAppDetailsActivity(a.component, a.user, null, null);
            else la.startShortcut(shortcuts.get(n - 1), null, null);
          } catch (Exception e) {
            toast("Shortcut unavailable");
          }
        });
  }

  int categoryOf(App a) {
    JSONObject overrides = settings.optJSONObject("categories");
    return overrides != null && overrides.has(a.key)
        ? overrides.optInt(a.key, -1)
        : a.info.getApplicationInfo().category;
  }

  boolean hidden(String key) {
    JSONObject h = settings.optJSONObject("hidden");
    return h != null && h.optBoolean(key);
  }

  void chooseDock() {
    menu(
        "Dock",
        new String[] {"Set slot 1", "Set slot 2", "Set slot 3", "Set slot 4", "Clear dock"},
        n -> {
          if (n == 4) {
            dock = new JSONArray();
            save();
            render();
          } else
            drawer(
                a -> {
                  try {
                    dock.put(n, a.key);
                  } catch (Exception ignored) {
                  }
                  save();
                  render();
                });
        });
  }

  void legacySettings() {
    String[] choices = {
      "Set as default Home",
      "Theme: " + (dark() ? "Dark" : "Light"),
      "Monochrome icons: " + settings.optBoolean("mono", true),
      "App labels: " + settings.optBoolean("labels", true),
      "Grid: " + cols() + " columns",
      "Layout lock: " + settings.optBoolean("locked", false),
      "Choose wallpaper",
      "Clear wallpaper",
      "Export layout",
      "Import layout",
      "Manage Android widgets",
      "Gesture navigation & compatibility",
      "About / restore default layout",
      "Undo last layout change",
      "Restore pre-import recovery copy",
      "Icon pack"
    };
    menu(
        "Home settings",
        choices,
        n -> {
          switch (n) {
            case 0:
              RoleManager rm = (RoleManager) getSystemService(ROLE_SERVICE);
              if (rm.isRoleAvailable(RoleManager.ROLE_HOME))
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 50);
              else open(new Intent(Settings.ACTION_HOME_SETTINGS));
              break;
            case 1:
              put(settings, "dark", !dark());
              break;
            case 2:
              put(settings, "mono", !settings.optBoolean("mono", true));
              break;
            case 3:
              put(settings, "labels", !settings.optBoolean("labels", true));
              break;
            case 4:
              put(settings, "columns", cols() == 4 ? 5 : 4);
              break;
            case 5:
              put(settings, "locked", !settings.optBoolean("locked", false));
              break;
            case 6:
              pickDocument(WALL, "image/*");
              break;
            case 7:
              settings.remove("wallpaper");
              break;
            case 8:
              Intent export =
                  new Intent(Intent.ACTION_CREATE_DOCUMENT)
                      .setType("application/json")
                      .putExtra(Intent.EXTRA_TITLE, "dot-home-layout.json")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              startActivityForResult(export, EXPORT);
              break;
            case 9:
              pickDocument(IMPORT, "application/json");
              break;
            case 10:
              manageWidgets();
              break;
            case 11:
              new AlertDialog.Builder(this)
                  .setTitle("POCO / HyperOS")
                  .setMessage(
                      "Dot Home controls your home screen and widgets. HyperOS controls native"
                          + " navigation, Recents, lock screen and app transitions.\n\n"
                          + "Choose the supported navigation mode in system settings. Keep POCO"
                          + " Launcher installed.\n\n"
                          + "Android "
                          + Build.VERSION.RELEASE
                          + " · "
                          + Build.MANUFACTURER
                          + " "
                          + Build.MODEL
                          + "\nBuild: "
                          + Build.DISPLAY)
                  .setPositiveButton(
                      "System settings", (d, w) -> open(new Intent(Settings.ACTION_SETTINGS)))
                  .setNegativeButton("Close", null)
                  .show();
              break;
            case 15:
              chooseIconPack();
              break;
            case 14:
              try {
                JSONObject recovery =
                    new JSONObject(
                        java.nio.file.Files.readString(
                            new File(getFilesDir(), "before-import.json").toPath()));
                validateBackup(recovery);
                OfflineMigration.apply(recovery);
                items = recovery.getJSONArray("items");
                settings = recovery.getJSONObject("settings");
                dock = recovery.optJSONArray("dock");
                if (dock == null) dock = new JSONArray();
                save();
                render();
                toast("Recovery layout restored; widgets may need rebinding");
              } catch (Exception e) {
                toast("No valid recovery copy available");
              }
              break;
            case 13:
              try {
                String undo = prefs.getString("undo", "");
                if (!undo.isEmpty()) {
                  JSONObject legacy = new JSONObject();
                  put(legacy, "items", new JSONArray(undo));
                  OfflineMigration.apply(legacy);
                  items = legacy.getJSONArray("items");
                  save();
                  render();
                } else toast("No layout edit to undo");
              } catch (Exception e) {
                toast("Undo unavailable");
              }
              break;
            case 12:
              new AlertDialog.Builder(this)
                  .setTitle("Dot Home " + BuildInfo.VERSION_NAME)
                  .setMessage(
                      "Independent Nothing-inspired native launcher. Fully offline. No Internet"
                          + " permission, account, ads or analytics.\n\n"
                          + "Layout reset affects Dot Home only.")
                  .setPositiveButton("Close", null)
                  .setNeutralButton(
                      "Reset layout",
                      (d, w) ->
                          new AlertDialog.Builder(this)
                              .setMessage("Reset Dot Home layout and settings?")
                              .setNegativeButton("Cancel", null)
                              .setPositiveButton(
                                  "Reset",
                                  (dd, ww) -> {
                                    for (int i = 0; i < items.length(); i++) {
                                      JSONObject x = items.optJSONObject(i);
                                      if (x.optString("type").equals("widget"))
                                        host.deleteAppWidgetId(x.optInt("widgetId"));
                                    }
                                    defaults();
                                    save();
                                    render();
                                  })
                              .show())
                  .show();
              break;
          }
          save();
          render();
        });
  }

  void chooseIconPack() {
    java.util.LinkedHashMap<String, String> packs = new java.util.LinkedHashMap<>();
    packs.put("", "System icons");
    for (String action :
        new String[] {
          "org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.anddoes.launcher.THEME"
        })
      for (ResolveInfo r : getPackageManager().queryIntentActivities(new Intent(action), 0))
        packs.put(r.activityInfo.packageName, r.loadLabel(getPackageManager()).toString());
    String[] keys = packs.keySet().toArray(new String[0]);
    menu(
        "Icon pack",
        packs.values().toArray(new String[0]),
        n -> {
          put(settings, "iconPack", keys[n]);
          icons.clear();
          loadedPack = "!";
          save();
          render();
        });
  }

  void controls() {
    menu(
        "Quick controls",
        new String[] {
          "Bluetooth settings",
          "Toggle flashlight",
          "Do Not Disturb settings",
          "Set timer in Clock",
          "Sound settings"
        },
        n -> {
          if (n == 0) open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
          if (n == 1) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
              requestPermissions(new String[] {Manifest.permission.CAMERA}, 22);
              return;
            }
            try {
              CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
              for (String id : cm.getCameraIdList()) {
                if (Boolean.TRUE.equals(
                    cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                  cm.setTorchMode(id, !torch);
                  torch = !torch;
                  break;
                }
              }
            } catch (Exception e) {
              toast("Flashlight unavailable or camera in use");
            }
          }
          if (n == 2) open(new Intent("android.settings.ZEN_MODE_SETTINGS"));
          if (n == 3) open(new Intent(AlarmClock.ACTION_SET_TIMER));
          if (n == 4) open(new Intent(Settings.ACTION_SOUND_SETTINGS));
        });
  }

  void pickDocument(int request, String mime) {
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType(mime)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
        request);
  }

  void pickPhoto(JSONObject x) {
    pendingPhoto = x.optString("id");
    pickDocument(PHOTO, "image/*");
  }

  void addProvider(AppWidgetProviderInfo info) {
    if (pendingWidget >= 0) cancelWidget();
    pendingWidget = host.allocateAppWidgetId();
    prefs.edit().putInt("pendingWidget", pendingWidget).apply();
    if (AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(pendingWidget, info.provider))
      configureWidget();
    else
      startActivityForResult(
          new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider),
          BIND);
  }

  void toggleTorch() {
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.CAMERA}, 22);
      return;
    }
    try {
      CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
      for (String id : cm.getCameraIdList())
        if (Boolean.TRUE.equals(
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
          cm.setTorchMode(id, !torch);
          torch = !torch;
          refreshTiles();
          break;
        }
    } catch (Exception e) {
      toast("Flashlight unavailable or camera in use");
    }
  }

  void configureWidget() {
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(pendingWidget);
    if (info == null) {
      cancelWidget();
      return;
    }
    if (info.configure != null)
      try {
        host.startAppWidgetConfigureActivityForResult(this, pendingWidget, 0, CONFIG, null);
      } catch (Exception e) {
        cancelWidget();
        toast("Widget configuration unavailable");
      }
    else finishWidget();
  }

  int[] constrainedSize(JSONObject item, int width, int height) {
    String type = item.optString("type");
    if (type.equals("app") || type.equals("folder")) {
      int size = width > 1 || height > 1 ? 2 : 1;
      return new int[] {size, size};
    }
    if (type.equals("clock") || type.equals("media")) {
      width = Math.max(2, width);
      height = Math.max(2, height);
    }
    if (type.equals("widget")) {
      AppWidgetProviderInfo info =
          AppWidgetManager.getInstance(this).getAppWidgetInfo(item.optInt("widgetId", -1));
      if (info != null) {
        float cellW =
            board == null
                ? getResources().getDisplayMetrics().widthPixels / (float) cols()
                : board.cw();
        float cellH = board == null ? dp(90) : board.ch();
        if ((info.resizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) == 0)
          width = item.optInt("w", 1);
        else {
          width =
              Math.max(width, Math.max(1, (int) Math.ceil((info.minResizeWidth + dp(10)) / cellW)));
          if (info.maxResizeWidth > 0)
            width =
                Math.min(
                    width, Math.max(1, (int) Math.floor((info.maxResizeWidth + dp(10)) / cellW)));
        }
        if ((info.resizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) == 0)
          height = item.optInt("h", 1);
        else {
          height =
              Math.max(
                  height, Math.max(1, (int) Math.ceil((info.minResizeHeight + dp(10)) / cellH)));
          if (info.maxResizeHeight > 0)
            height =
                Math.min(
                    height, Math.max(1, (int) Math.floor((info.maxResizeHeight + dp(10)) / cellH)));
        }
      }
    }
    return new int[] {width, height};
  }

  void finishWidget() {
    JSONObject x = obj("widget");
    put(x, "widgetId", pendingWidget);
    AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(pendingWidget);
    if (info == null) {
      cancelWidget();
      toast("Widget provider unavailable");
      return;
    }
    float cellW =
        board == null
            ? getResources().getDisplayMetrics().widthPixels / (float) cols()
            : board.cw();
    float cellH = board == null ? dp(90) : board.ch();
    int width =
        Math.max(
            info.targetCellWidth, Math.max(1, (int) Math.ceil((info.minWidth + dp(10)) / cellW)));
    int height =
        Math.max(
            info.targetCellHeight, Math.max(1, (int) Math.ceil((info.minHeight + dp(10)) / cellH)));
    if (width > cols() || height > ui.rows()) {
      cancelWidget();
      toast("Widget needs a larger grid; change Home layout first");
      return;
    }
    put(x, "w", width);
    put(x, "h", height);
    put(x, "provider", info.provider.flattenToString());
    try {
      JSONArray candidate = new JSONArray(items.toString());
      candidate.put(x);
      items = WorkspaceModel.normalise(candidate, cols(), ui.rows());
    } catch (Exception error) {
      cancelWidget();
      toast("No room; existing layout preserved");
      return;
    }
    pendingWidget = -1;
    prefs.edit().remove("pendingWidget").apply();
    save();
    render();
    int at = index(x.optString("id"));
    if (at >= 0) ui.goPage(items.optJSONObject(at).optInt("page"), true);
  }

  void cancelWidget() {
    if (pendingWidget >= 0) host.deleteAppWidgetId(pendingWidget);
    pendingWidget = -1;
    prefs.edit().remove("pendingWidget").apply();
  }

  void configureExisting(JSONObject x) {
    int id = x.optInt("widgetId", -1);
    try {
      host.startAppWidgetConfigureActivityForResult(this, id, 0, 60, null);
    } catch (Exception e) {
      toast("This widget has no configuration screen");
    }
  }

  void manageWidgets() {
    ArrayList<JSONObject> widgets = new ArrayList<>();
    for (int i = 0; i < items.length(); i++)
      if (items.optJSONObject(i).optString("type").equals("widget"))
        widgets.add(items.optJSONObject(i));
    if (widgets.isEmpty()) {
      pickWidget();
      return;
    }
    String[] names = new String[widgets.size()];
    for (int i = 0; i < names.length; i++)
      names[i] = widgets.get(i).optString("provider", "Android widget");
    menu("Manage widgets", names, n -> itemMenu(widgets.get(n), root));
  }

  @Override
  public void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    try {
      if (request == BIND) {
        if (result == RESULT_OK) configureWidget();
        else cancelWidget();
        return;
      }
      if (request == CONFIG) {
        if (result == RESULT_OK) finishWidget();
        else cancelWidget();
        return;
      }
      if (result != RESULT_OK || data == null || data.getData() == null) return;
      Uri uri = data.getData();
      if (request == PHOTO || request == WALL) {
        getContentResolver()
            .takePersistableUriPermission(
                uri, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (request == WALL) put(settings, "wallpaper", uri.toString());
        else {
          int i = index(pendingPhoto);
          if (i >= 0) put(items.optJSONObject(i), "uri", uri.toString());
        }
        save();
        render();
      }
      if (request == EXPORT) {
        JSONObject backup = new JSONObject();
        put(backup, "schema", 2);
        put(backup, "settings", settings);
        JSONArray copy = new JSONArray(items.toString());
        for (int i = 0; i < copy.length(); i++) {
          JSONObject x = copy.getJSONObject(i);
          x.remove("widgetId");
        }
        put(backup, "items", copy);
        put(backup, "dock", dock);
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
          out.write(backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        toast("Layout exported. Android widgets need rebinding after import.");
      }
      if (request == IMPORT) {
        JSONObject candidate;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
          byte[] raw = in.readNBytes(1048577);
          if (raw.length > 1048576) throw new Exception("File too large");
          candidate = new JSONObject(new String(raw, java.nio.charset.StandardCharsets.UTF_8));
        }
        validateBackup(candidate);
        OfflineMigration.apply(candidate);
        final JSONObject incoming = candidate;
        new AlertDialog.Builder(this)
            .setTitle("Replace current layout?")
            .setMessage(
                "Android widgets will need to be added again. Photos may need to be selected again."
                    + " A recovery copy of your current layout will be saved.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                "Import",
                (d, w) -> {
                  try {
                    java.nio.file.Files.copy(
                        new File(getFilesDir(), STORE).toPath(),
                        new File(getFilesDir(), "before-import.json").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    JSONArray newItems = incoming.getJSONArray("items");
                    for (int i = 0; i < newItems.length(); i++) {
                      JSONObject x = newItems.getJSONObject(i);
                      x.remove("widgetId");
                    }
                    for (int i = 0; i < items.length(); i++) {
                      JSONObject x = items.optJSONObject(i);
                      if (x.optString("type").equals("widget"))
                        host.deleteAppWidgetId(x.optInt("widgetId"));
                    }
                    items = newItems;
                    settings = incoming.getJSONObject("settings");
                    dock = incoming.optJSONArray("dock");
                    if (dock == null) dock = new JSONArray();
                    page = 0;
                    save();
                    render();
                  } catch (Exception e) {
                    toast("Import failed; previous layout kept");
                  }
                })
            .show();
      }
    } catch (Exception e) {
      toast("Couldn't complete action: " + e.getMessage());
    }
  }

  void validateBackup(JSONObject x) throws Exception {
    LayoutValidator.validate(x);
  }

  class DotClock extends View {
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    String[] patterns = {
      "111101101101111",
      "010110010010111",
      "111001111100111",
      "111001111001111",
      "101101111001001",
      "111100111001111",
      "111100111101111",
      "111001001001001",
      "111101111101111",
      "111101111001111"
    };

    DotClock() {
      super(HomeActivity.this);
      setContentDescription("Current time");
    }

    @Override
    protected void onDraw(Canvas canvas) {
      String time =
          new SimpleDateFormat(
                  android.text.format.DateFormat.is24HourFormat(HomeActivity.this)
                      ? "HH:mm"
                      : "hh:mm",
                  Locale.US)
              .format(new Date());
      float unit = Math.min(getWidth() / 19f, getHeight() / 6f),
          left = (getWidth() - unit * 18) / 2,
          top = (getHeight() - unit * 5) / 2;
      for (int k = 0; k < 5; k++) {
        char c = time.charAt(k);
        int width = c == ':' ? 1 : 3;
        for (int row = 0; row < 5; row++)
          for (int col = 0; col < width; col++) {
            boolean on =
                c == ':' ? (row == 1 || row == 3) : patterns[c - '0'].charAt(row * 3 + col) == '1';
            paint.setColor(
                on
                    ? (c == ':' ? accent : fg)
                    : (dark() ? Color.rgb(52, 52, 52) : Color.rgb(199, 194, 185)));
            canvas.drawCircle(
                left + col * unit + unit / 2, top + row * unit + unit / 2, unit * .30f, paint);
          }
        left += (width + 1) * unit;
      }
      postInvalidateDelayed(1000);
    }
  }
}
