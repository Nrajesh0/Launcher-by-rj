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
  String agenda = "Tap to connect calendar",
      usage = "Tap to allow usage access";
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
    android.content.SharedPreferences.Editor cleanup=prefs.edit(); for(String key:prefs.getAll().keySet())if(key.startsWith("weather"))cleanup.remove(key); cleanup.apply();
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
    getOnBackInvokedDispatcher()
        .registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> onBackPressed());
    render();
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
    worker.shutdownNow();
    super.onDestroy();
  }

  @Override
  public void onNewIntent(Intent i) {
    super.onNewIntent(i);
    if (drawer != null) drawer.dismiss();
    page = 0;
    render();
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration c) {
    super.onConfigurationChanged(c);
    render();
  }

  @Override
  public void onBackPressed() {
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
    String[] types = {"clock", "date", "battery", "notes"};
    for (String t : types) {
      JSONObject x = obj(t);
      put(x, "w", t.equals("clock") ? 4 : 2);
      put(x, "h", t.equals("clock") ? 2 : 1);
      if (t.equals("notes")) put(x, "text", "Make room for what matters.");
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
      put(x, "schema", 1);
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
      ColorMatrix cm = new ColorMatrix();
      cm.setSaturation(0);
      d.setColorFilter(new ColorMatrixColorFilter(cm));
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
    colors();
    root = new LinearLayout(this);
    root.setOrientation(1);
    root.setBackgroundColor(bg);
    root.setPadding(dp(20), 0, dp(20), 0);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, in) -> {
          android.graphics.Insets sys =
              in.getInsets(
                  WindowInsets.Type.systemBars()
                      | WindowInsets.Type.displayCutout()
                      | WindowInsets.Type.ime());
          root.setPadding(dp(20), sys.top, dp(20), sys.bottom);
          return in;
        });
    final View surface = root;
    surface.post(
        () -> {
          WindowInsetsController controller = surface.getWindowInsetsController();
          if (controller != null)
            controller.setSystemBarsAppearance(
                dark() ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        });
    String wall = settings.optString("wallpaper", "");
    if (!wall.isEmpty())
      try {
        Drawable d = scaledImage(Uri.parse(wall));
        root.setBackground(d);
      } catch (Exception ignored) {
      }
    top = new LinearLayout(this);
    top.setGravity(Gravity.CENTER_VERTICAL);
    TextView brand = label("DOT / HOME");
    top.addView(brand, new LinearLayout.LayoutParams(0, dp(50), 1));
    TextView edit = text("＋", 28, fg);
    edit.setContentDescription("Add apps or widgets");
    top.addView(edit, new LinearLayout.LayoutParams(dp(44), dp(50)));
    click(edit, () -> addMenu());
    TextView options = text("···", 24, fg);
    options.setContentDescription("Home settings");
    top.addView(options, new LinearLayout.LayoutParams(dp(38), dp(50)));
    click(options, () -> settings());
    root.addView(top);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    board = new Board();
    scroll.addView(board, new ScrollView.LayoutParams(-1, -1));
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    bottom = new LinearLayout(this);
    bottom.setOrientation(1);
    LinearLayout pages = new LinearLayout(this);
    pages.setGravity(Gravity.CENTER);
    for (int p = 0; p < 3; p++) {
      final int n = p;
      TextView dot = text(p == page ? "●" : "○", 14, p == page ? accent : muted);
      dot.setGravity(Gravity.CENTER);
      pages.addView(dot, new LinearLayout.LayoutParams(dp(36), dp(30)));
      click(
          dot,
          () -> {
            page = n;
            render();
          });
    }
    bottom.addView(pages);
    LinearLayout dockRow = new LinearLayout(this);
    dockRow.setGravity(Gravity.CENTER);
    for (int p = 0; p < 4; p++) {
      String key = dock.optString(p, "");
      App a = app(key);
      View v = a == null ? text("＋", 26, muted) : appView(a, false);
      if (a == null) click(v, () -> chooseDock());
      v.setOnLongClickListener(
          w -> {
            chooseDock();
            return true;
          });
      dockRow.addView(v, new LinearLayout.LayoutParams(0, dp(66), 1));
    }
    bottom.addView(dockRow);
    TextView bar = text("⌕   Search apps", 15, fg);
    bar.setPadding(dp(22), 0, dp(22), 0);
    bar.setBackground(shape(card, 32));
    bar.setContentDescription("Open app drawer and search");
    click(bar, () -> drawer(null));
    bottom.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
    bottom.setPadding(0, 0, 0, dp(10));
    root.addView(bottom);
    root.setOnLongClickListener(
        v -> {
          addMenu();
          return true;
        });
  }

  View appView(App a, boolean withLabel) {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(1);
    box.setGravity(Gravity.CENTER);
    ImageView im = new ImageView(this);
    im.setImageDrawable(icon(a));
    int size = dp(settings.optInt("iconSize", 46));
    box.addView(im, new LinearLayout.LayoutParams(size, size));
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
    ArrayList<JSONObject> visible = new ArrayList<>();
    List<int[]> positions;
    float startX, startY;

    Board() {
      super(HomeActivity.this);
      ArrayList<int[]> sizes = new ArrayList<>();
      for (int i = 0; i < items.length(); i++) {
        JSONObject x = items.optJSONObject(i);
        if (x != null && x.optInt("page") == page) {
          visible.add(x);
          sizes.add(new int[] {x.optInt("w", 1), x.optInt("h", 1)});
          addView(itemView(x));
        }
      }
      positions = GridPlanner.place(sizes, cols());
      setOnDragListener(
          (v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP
                && event.getLocalState() instanceof JSONObject) {
              JSONObject moving = (JSONObject) event.getLocalState();
              int target = -1;
              for (int n = 0; n < getChildCount(); n++) {
                View child = getChildAt(n);
                if (event.getY() >= child.getTop()
                    && event.getY() <= child.getBottom()
                    && event.getX() >= child.getLeft()
                    && event.getX() <= child.getRight()) {
                  target = index(visible.get(n).optString("id"));
                  break;
                }
              }
              int from = index(moving.optString("id"));
              if (from >= 0 && target >= 0 && from != target) {
                JSONArray next = new JSONArray();
                for (int n = 0; n < items.length(); n++)
                  next.put(items.optJSONObject(n == from ? target : n == target ? from : n));
                items = next;
                save();
                post(() -> render());
              }
              return true;
            }
            return true;
          });
    }

    @Override
    protected void onMeasure(int ws, int hs) {
      int width = MeasureSpec.getSize(ws), cell = (width) / cols(), row = dp(91);
      int rows = 1;
      for (int i = 0; i < getChildCount(); i++) {
        int[] p = positions.get(i);
        getChildAt(i)
            .measure(
                MeasureSpec.makeMeasureSpec(p[2] * cell - dp(10), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(p[3] * row - dp(10), MeasureSpec.EXACTLY));
        rows = Math.max(rows, p[1] + p[3]);
      }
      setMeasuredDimension(width, Math.max(MeasureSpec.getSize(hs), rows * row + dp(12)));
    }

    @Override
    protected void onLayout(boolean c, int l, int t, int r, int b) {
      int cell = getWidth() / cols(), row = dp(91);
      for (int i = 0; i < getChildCount(); i++) {
        int[] p = positions.get(i);
        View v = getChildAt(i);
        int x = p[0] * cell + dp(5), y = p[1] * row + dp(5);
        v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
      }
    }

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        startX = e.getX();
        startY = e.getY();
      }
      if (movingId != null) return true;
      if (e.getAction() == MotionEvent.ACTION_MOVE
          && Math.abs(e.getX() - startX) > dp(70)
          && Math.abs(e.getY() - startY) < dp(45)) return true;
      return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_UP) {
        if (movingId != null) {
          int from = index(movingId), target = -1;
          for (int n = 0; n < getChildCount(); n++) {
            View child = getChildAt(n);
            if (e.getY() >= child.getTop()
                && e.getY() <= child.getBottom()
                && e.getX() >= child.getLeft()
                && e.getX() <= child.getRight()) target = index(visible.get(n).optString("id"));
          }
          movingId = null;
          if (from >= 0 && target >= 0 && from != target) {
            JSONArray next = new JSONArray();
            for (int n = 0; n < items.length(); n++)
              next.put(items.optJSONObject(n == from ? target : n == target ? from : n));
            items = next;
            save();
            render();
          }
          return true;
        }
        float dx = e.getX() - startX, dy = e.getY() - startY;
        if (Math.abs(dx) > dp(70)) {
          page = Math.max(0, Math.min(2, page + (dx < 0 ? 1 : -1)));
          render();
        } else if (dy < -dp(70)) drawer(null);
        else if (Math.abs(dy) < dp(10)) addMenu();
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
      if (a != null
          && x.optInt("w", 1) >= 2
          && x.optInt("h", 1) >= 2
          && v instanceof LinearLayout) {
        View image = ((LinearLayout) v).getChildAt(0);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(78), dp(78)));
      }
    } else if (type.equals("folder")) {
      LinearLayout box = new LinearLayout(this);
      box.setOrientation(1);
      box.setGravity(Gravity.CENTER);
      box.setBackground(shape(card, 26));
      JSONArray children = x.optJSONArray("children");
      GridLayout grid = new GridLayout(this);
      grid.setColumnCount(2);
      if (children != null)
        for (int i = 0; i < Math.min(4, children.length()); i++) {
          App a = app(children.optString(i));
          View child = a == null ? text("○", 24, fg) : appView(a, false);
          grid.addView(child, new ViewGroup.LayoutParams(dp(54), dp(54)));
        }
      box.addView(grid);
      TextView title = text(x.optString("name", "Folder"), 12, fg);
      title.setGravity(Gravity.CENTER);
      box.addView(title);
      click(title, () -> folder(x));
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
                itemMenu(x, hv);
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
            itemMenu(x, w);
            return true;
          });
    return v;
  }

  void fillTile(LinearLayout tile, JSONObject x) {
    tile.removeAllViews();
    String type = x.optString("type");
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
          ImageView im = new ImageView(this);
          im.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
    if (board == null) return;
    for (int i = 0; i < board.getChildCount(); i++) {
      View v = board.getChildAt(i);
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
      case "battery":
        open(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        break;
      default:
        toast("Long press for widget options");
    }
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
    MediaMetadata data = c.getMetadata();
    String title =
        data == null ? "Media" : String.valueOf(data.getText(MediaMetadata.METADATA_KEY_TITLE));
    TextView name = text(title, 16, fg);
    name.setMaxLines(2);
    tile.addView(name, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout buttons = new LinearLayout(this);
    String[] names = {"‹", "▶ / Ⅱ", "›"};
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
    if (settings.optBoolean("locked", false)) {
      toast("Unlock layout in Home settings");
      return;
    }
    menu(
        "Make it yours",
        new String[] {
          "Add an app",
          "Add a built-in widget",
          "Add an Android widget",
          "Create a folder",
          "Home settings"
        },
        n -> {
          if (n == 0)
            drawer(
                a -> {
                  JSONObject x = obj("app");
                  put(x, "key", a.key);
                  put(x, "label", a.label);
                  items.put(x);
                  save();
                  render();
                });
          if (n == 1) addTile();
          if (n == 2) pickWidget();
          if (n == 3) {
            JSONObject x = obj("folder");
            put(x, "w", 2);
            put(x, "h", 2);
            put(x, "children", new JSONArray());
            input(
                "Folder name",
                "Essentials",
                s -> {
                  put(x, "name", s);
                  items.put(x);
                  save();
                  render();
                  folder(x);
                });
          }
          if (n == 4) settings();
        });
  }

  void addTile() {
    String[] types = {
      "clock",
      "date",
      "battery",
      "media",
      "agenda",
      "photo",
      "notes",
      "countdown",
      "compass",
      "usage",
      "controls"
    };
    String[] names = {
      "Dot clock",
      "Date",
      "Battery",
      "Media player",
      "Upcoming events",
      "Photo",
      "Note",
      "Countdown",
      "Compass",
      "App usage",
      "Quick controls"
    };
    menu(
        "Built-in widgets",
        names,
        n -> {
          JSONObject x = obj(types[n]);
          put(x, "w", types[n].equals("clock") || types[n].equals("controls") ? cols() : 2);
          put(
              x,
              "h",
              types[n].equals("clock") || types[n].equals("media") || types[n].equals("photo")
                  ? 2
                  : 1);
          items.put(x);
          save();
          render();
        });
  }

  void itemMenu(JSONObject x, View v) {
    if (settings.optBoolean("locked", false)) {
      toast("Layout is locked");
      return;
    }
    String type = x.optString("type");
    menu(
        type.equals("app") ? x.optString("label", "App") : type.toUpperCase(),
        new String[] {
          "Move · tap another item",
          "Resize",
          "Move to next page",
          "Edit / open",
          "Remove from Home",
          "App info / shortcuts"
        },
        n -> {
          switch (n) {
            case 0:
              movingId = x.optString("id");
              toast("Tap another Home item to swap positions");
              break;
            case 1:
              menu(
                  "Size",
                  new String[] {"1 × 1", "2 × 1", "2 × 2", "Full width × 2", "Full width × 3"},
                  s -> {
                    int[][] sizes = {{1, 1}, {2, 1}, {2, 2}, {cols(), 2}, {cols(), 3}};
                    put(x, "w", sizes[s][0]);
                    put(x, "h", sizes[s][1]);
                    save();
                    render();
                  });
              break;
            case 2:
              put(x, "page", (page + 1) % 3);
              save();
              render();
              break;
            case 3:
              if (type.equals("folder")) folder(x);
              else if (type.equals("app")) launch(x.optString("key"));
              else if (type.equals("widget")) configureExisting(x);
              else tileAction(x);
              break;
            case 4:
              if (type.equals("widget")) host.deleteAppWidgetId(x.optInt("widgetId"));
              items.remove(index(x.optString("id")));
              save();
              render();
              break;
            case 5:
              if (type.equals("app")) appOptions(app(x.optString("key")));
              break;
          }
        });
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

  void folder(JSONObject x) {
    JSONArray children = x.optJSONArray("children");
    if (children == null) {
      children = new JSONArray();
      put(x, "children", children);
    }
    final JSONArray keys = children;
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(1);
    panel.setPadding(dp(20), dp(12), dp(20), dp(12));
    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(3);
    for (int i = 0; i < keys.length(); i++) {
      final int at = i;
      App a = app(keys.optString(i));
      if (a == null) continue;
      View v = appView(a, true);
      grid.addView(v, new ViewGroup.LayoutParams(dp(85), dp(85)));
      v.setOnLongClickListener(
          w -> {
            keys.remove(at);
            save();
            render();
            toast("Removed from folder");
            return true;
          });
    }
    panel.addView(grid);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle(x.optString("name", "Folder"))
            .setView(panel)
            .setPositiveButton(
                "Add app",
                (q, n) ->
                    drawer(
                        a -> {
                          keys.put(a.key);
                          save();
                          render();
                          folder(x);
                        }))
            .setNeutralButton(
                "Rename",
                (q, n) ->
                    input(
                        "Folder name",
                        x.optString("name"),
                        s -> {
                          put(x, "name", s);
                          save();
                          render();
                        }))
            .setNegativeButton("Close", null)
            .create();
    d.show();
  }

  int categoryOf(App a) {
    JSONObject overrides = settings.optJSONObject("categories");
    return overrides != null && overrides.has(a.key)
        ? overrides.optInt(a.key, -1)
        : a.info.getApplicationInfo().category;
  }

  void drawer(java.util.function.Consumer<App> pick) {
    drawer = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(1);
    panel.setPadding(dp(22), dp(28), dp(22), dp(20));
    panel.setBackgroundColor(bg);
    LinearLayout header = new LinearLayout(this);
    TextView title = text(pick == null ? "All apps" : "Choose an app", 29, fg);
    header.addView(title, new LinearLayout.LayoutParams(0, dp(60), 1));
    TextView close = text("×", 30, fg);
    header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(60)));
    click(close, () -> drawer.dismiss());
    panel.addView(header);
    TextView hint = label("LONG PRESS AN APP FOR ACTIONS");
    panel.addView(hint);
    ScrollView sv = new ScrollView(this);
    GridLayout grid = new GridLayout(this);
    grid.setColumnCount(4);
    sv.addView(grid);
    panel.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
    search = new EditText(this);
    search.setSingleLine();
    search.setTextColor(fg);
    search.setHintTextColor(muted);
    search.setHint("Search apps");
    search.setBackground(shape(card, 28));
    search.setPadding(dp(18), 0, dp(18), 0);
    search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
    panel.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
    final int[] category = {-1};
    TextView categoryChip = text("All categories ▾", 14, fg);
    panel.addView(categoryChip, 2, new LinearLayout.LayoutParams(-1, dp(42)));
    Runnable filter =
        () -> {
          grid.removeAllViews();
          String query = search.getText().toString().toLowerCase(Locale.ROOT).trim();
          ArrayList<App> matches = new ArrayList<>();
          for (App a : apps)
            if (a.label.toLowerCase(Locale.ROOT).contains(query)
                && (!hidden(a.key) || !query.isEmpty())
                && (category[0] < 0 || categoryOf(a) == category[0])) matches.add(a);
          int width = (getResources().getDisplayMetrics().widthPixels - dp(44)) / 4;
          for (App a : matches) {
            View v = appView(a, true);
            grid.addView(v, new ViewGroup.LayoutParams(width, dp(93)));
            click(
                v,
                () -> {
                  drawer.dismiss();
                  if (pick == null) launch(a.key);
                  else pick.accept(a);
                });
            v.setOnLongClickListener(
                w -> {
                  if (pick != null) return false;
                  menu(
                      a.label,
                      new String[] {
                        "Add to Home",
                        "App info / shortcuts",
                        "Hide / show in drawer",
                        "Set category"
                      },
                      n -> {
                        if (n == 0) {
                          JSONObject x = obj("app");
                          put(x, "key", a.key);
                          put(x, "label", a.label);
                          items.put(x);
                          save();
                          drawer.dismiss();
                          render();
                        } else if (n == 1) appOptions(a);
                        else if (n == 3) {
                          String[] groups = {
                            "Games",
                            "Audio",
                            "Video",
                            "Images",
                            "Social",
                            "News",
                            "Maps",
                            "Productivity",
                            "Accessibility"
                          };
                          menu(
                              "App category",
                              groups,
                              c -> {
                                JSONObject categories = settings.optJSONObject("categories");
                                if (categories == null) {
                                  categories = new JSONObject();
                                  put(settings, "categories", categories);
                                }
                                put(categories, a.key, c);
                                save();
                              });
                        } else {
                          JSONObject h = settings.optJSONObject("hidden");
                          if (h == null) {
                            h = new JSONObject();
                            put(settings, "hidden", h);
                          }
                          put(h, a.key, !hidden(a.key));
                          save();
                          toast("Hidden apps remain searchable");
                        }
                      });
                  return true;
                });
          }
          if (matches.isEmpty()) {
            TextView empty = text("No matching apps", 16, muted);
            grid.addView(empty, new ViewGroup.LayoutParams(-1, dp(100)));
          }
        };
    click(
        categoryChip,
        () -> {
          String[] groups = {
            "All categories",
            "Games",
            "Audio",
            "Video",
            "Images",
            "Social",
            "News",
            "Maps",
            "Productivity",
            "Accessibility"
          };
          menu(
              "Categories",
              groups,
              n -> {
                category[0] = n - 1;
                categoryChip.setText(groups[n] + " ▾");
                filter.run();
              });
        });
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            filter.run();
          }

          public void afterTextChanged(android.text.Editable e) {}
        });
    search.setOnEditorActionListener(
        (v, action, event) -> {
          String q = search.getText().toString().toLowerCase(Locale.ROOT);
          for (App a : apps)
            if (a.label.toLowerCase(Locale.ROOT).contains(q)) {
              drawer.dismiss();
              if (pick == null) launch(a.key);
              else pick.accept(a);
              return true;
            }
          return false;
        });
    drawer.setContentView(panel);
    drawer
        .getWindow()
        .setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    drawer.show();
    filter.run();
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

  void settings() {
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
      "Navigation & compatibility",
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
                  JSONObject legacy=new JSONObject();put(legacy,"items",new JSONArray(undo));OfflineMigration.apply(legacy);items=legacy.getJSONArray("items");
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
                      "Independent Nothing-inspired native launcher. Fully offline. No Internet permission, account, ads or analytics.\n\n"
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

  void pickWidget() {
    if (settings.optBoolean("locked", false)) return;
    List<AppWidgetProviderInfo> providers =
        AppWidgetManager.getInstance(this).getInstalledProviders();
    providers.sort(
        (a, b) ->
            a.loadLabel(getPackageManager()).compareToIgnoreCase(b.loadLabel(getPackageManager())));
    String[] names = new String[providers.size()];
    for (int i = 0; i < names.length; i++)
      names[i] = providers.get(i).loadLabel(getPackageManager());
    menu(
        "Android widgets",
        names,
        n -> {
          AppWidgetProviderInfo info = providers.get(n);
          if (pendingWidget >= 0) cancelWidget();
          pendingWidget = host.allocateAppWidgetId();
          prefs.edit().putInt("pendingWidget", pendingWidget).apply();
          if (AppWidgetManager.getInstance(this)
              .bindAppWidgetIdIfAllowed(pendingWidget, info.provider)) configureWidget();
          else
            startActivityForResult(
                new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider),
                BIND);
        });
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

  void finishWidget() {
    JSONObject x = obj("widget");
    put(x, "widgetId", pendingWidget);
    put(x, "w", cols());
    put(x, "h", 2);
    AppWidgetProviderInfo i = AppWidgetManager.getInstance(this).getAppWidgetInfo(pendingWidget);
    if (i != null) put(x, "provider", i.provider.flattenToString());
    items.put(x);
    pendingWidget = -1;
    prefs.edit().remove("pendingWidget").apply();
    save();
    render();
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
        put(backup, "schema", 1);
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
