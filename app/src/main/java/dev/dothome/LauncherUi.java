package dev.dothome;

import android.animation.*;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import java.util.function.*;
import org.json.*;

/** Persistent launcher surface, visual editing and reversible in-process transitions. */
final class LauncherUi {
  final HomeActivity a;
  FrameLayout screen, workspace, overlay;
  LinearLayout pageStrip, dots, hotseat;
  Pager pager;
  TextView dragBar;
  String state = "home";
  LinearLayout activePanel;
  Rect folderOrigin;
  float sheetDownY;
  JSONObject dragging, resizing;
  String sourceFolder = null, sourceKey = null;
  boolean dragNew, dropChanged;
  View dragOrigin;
  long edgeAt;
  final ArrayList<HomeActivity.Board> boards = new ArrayList<>();
  final Handler handler = new Handler(Looper.getMainLooper());

  LauncherUi(HomeActivity activity) {
    a = activity;
  }

  int d(float x) {
    return a.dp(x);
  }

  int duration() {
    return a.settings.optBoolean("reduceMotion", false) ? 0 : 260;
  }

  TextView title(String s, int size) {
    TextView v = a.text(s, size, a.fg);
    v.setTypeface(Typeface.create("sans-serif-medium", 0));
    return v;
  }

  TextView button(String text, Runnable action) {
    TextView t = a.text(text, 15, a.fg);
    t.setGravity(Gravity.CENTER);
    t.setPadding(d(16), d(10), d(16), d(10));
    t.setBackground(a.shape(a.card, 22));
    a.click(t, action);
    return t;
  }

  LinearLayout vertical() {
    LinearLayout l = new LinearLayout(a);
    l.setOrientation(1);
    return l;
  }

  void reconcile() {
    try {
      JSONArray next = WorkspaceModel.normalise(a.items, a.cols(), rows());
      if (!next.toString().equals(a.items.toString())) {
        a.items = next;
        a.save();
      }
    } catch (Exception e) {
      a.toast(e.getMessage());
    }
  }

  int rows() {
    return a.settings.optInt("rows", 6);
  }

  int pages() {
    int n = Math.max(1, a.settings.optInt("pages", 2));
    for (int i = 0; i < a.items.length(); i++)
      n = Math.max(n, a.items.optJSONObject(i).optInt("page") + 1);
    return Math.min(64, n);
  }

  void refresh() {
    reconcile();
    a.colors();
    if (screen == null) mount();
    screen.setBackgroundColor(a.bg);
    screen.setBackground(new WallpaperArtwork(a.settings.optString("wallpaperPreset", "solid")));
    if (a.settings.optString("wallpaperPreset", "solid").equals("solid"))
      screen.setBackgroundColor(a.bg);
    a.root.setBackgroundColor(Color.TRANSPARENT);
    String wall = a.settings.optString("wallpaper", "");
    if (!wall.isEmpty())
      try {
        screen.setBackground(a.scaledImage(android.net.Uri.parse(wall)));
      } catch (Exception ignored) {
      }
    renderPages();
    renderDock();
  }

  void mount() {
    screen = new FrameLayout(a);
    a.root = vertical();
    a.root.setPadding(d(14), 0, d(14), 0);
    screen.addView(a.root, new FrameLayout.LayoutParams(-1, -1));
    a.setContentView(screen);
    screen.setOnApplyWindowInsetsListener(
        (v, in) -> {
          android.graphics.Insets bars =
              in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
          screen.setPadding(0, bars.top, 0, bars.bottom);
          return in;
        });
    workspace = new FrameLayout(a);
    pager = new Pager();
    pager.setHorizontalScrollBarEnabled(false);
    pager.setFillViewport(true);
    pageStrip = new LinearLayout(a);
    pager.addView(pageStrip, new HorizontalScrollView.LayoutParams(-2, -1));
    workspace.addView(pager, new FrameLayout.LayoutParams(-1, -1));
    a.root.addView(workspace, new LinearLayout.LayoutParams(-1, 0, 1));
    dots = new LinearLayout(a);
    dots.setGravity(Gravity.CENTER);
    a.root.addView(dots, new LinearLayout.LayoutParams(-1, d(26)));
    hotseat = new LinearLayout(a);
    hotseat.setGravity(Gravity.CENTER);
    a.root.addView(hotseat, new LinearLayout.LayoutParams(-1, d(80)));
    FrameLayout search = new FrameLayout(a);
    search.addView(button("Search", () -> drawer(null)), new FrameLayout.LayoutParams(-1, -1));
    View lens =
        new View(a) {
          final Paint paint = new Paint(3);

          @Override
          protected void onDraw(Canvas canvas) {
            paint.setColor(a.fg);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(d(1.5f));
            canvas.drawCircle(d(8), d(8), d(5), paint);
            canvas.drawLine(d(12), d(12), d(17), d(17), paint);
          }
        };
    FrameLayout.LayoutParams lensPosition =
        new FrameLayout.LayoutParams(d(22), d(22), Gravity.START | Gravity.CENTER_VERTICAL);
    lensPosition.leftMargin = d(18);
    search.addView(lens, lensPosition);
    a.click(search, () -> drawer(null));
    search.setContentDescription("Search apps");
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, d(50));
    sp.setMargins(d(4), d(6), d(4), d(12));
    a.root.addView(search, sp);
    dragBar = button("Remove from Home", () -> {});
    dragBar.setTextColor(a.accent);
    FrameLayout.LayoutParams trash = new FrameLayout.LayoutParams(-1, d(54), Gravity.TOP);
    trash.setMargins(d(36), d(8), d(36), 0);
    screen.addView(dragBar, trash);
    dragBar.setVisibility(View.GONE);
    dragBar.setOnDragListener(
        (v, e) -> {
          if (e.getAction() == DragEvent.ACTION_DROP && dragging != null && !dragNew) {
            remove(dragging);
            dropChanged = true;
            return true;
          }
          return true;
        });
    screen.setOnDragListener(
        (v, e) -> {
          if (e.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            endDrag();
            return true;
          }
          return true;
        });
  }

  void renderPages() {
    int width =
        screen.getWidth() > 0
            ? screen.getWidth() - d(28)
            : a.getResources().getDisplayMetrics().widthPixels - d(28);
    int current = Math.max(0, Math.min(a.page, pages() - 1));
    pageStrip.removeAllViews();
    boards.clear();
    for (int p = 0; p < pages(); p++) {
      HomeActivity.Board board = a.new Board(p);
      boards.add(board);
      pageStrip.addView(board, new LinearLayout.LayoutParams(width, -1));
    }
    a.page = current;
    a.board = boards.get(current);
    dots.removeAllViews();
    for (int p = 0; p < pages(); p++) {
      final int page = p;
      TextView dot =
          a.text(p == current ? "●" : "·", p == current ? 10 : 20, p == current ? a.fg : a.muted);
      dot.setGravity(Gravity.CENTER);
      dots.addView(dot, new LinearLayout.LayoutParams(d(22), -1));
      a.click(dot, () -> goPage(page, true));
    }
    pager.post(() -> pager.scrollTo(a.page * pager.getWidth(), 0));
  }

  void goPage(int page, boolean animate) {
    page = Math.max(0, Math.min(pages() - 1, page));
    a.page = page;
    if (!boards.isEmpty()) a.board = boards.get(page);
    if (animate && duration() > 0) pager.smoothScrollTo(page * pager.getWidth(), 0);
    else pager.scrollTo(page * pager.getWidth(), 0);
    for (int p = 0; p < dots.getChildCount(); p++) {
      TextView dot = (TextView) dots.getChildAt(p);
      dot.setText(p == page ? "●" : "·");
      dot.setTextSize(p == page ? 10 : 20);
    }
  }

  void renderDock() {
    hotseat.removeAllViews();
    for (int n = 0; n < 4; n++) {
      final int slot = n;
      HomeActivity.App app = a.app(a.dock.optString(n, ""));
      View v = app == null ? button("＋", () -> selectDock(slot)) : a.appView(app, false);
      v.setOnLongClickListener(
          w -> {
            selectDock(slot);
            return true;
          });
      FrameLayout slotView = new FrameLayout(a);
      FrameLayout.LayoutParams iconSize =
          new FrameLayout.LayoutParams(d(68), d(68), Gravity.CENTER);
      slotView.addView(v, iconSize);
      if (app == null) {
        v.setBackground(a.shape(a.card, 100));
        v.setAlpha(.65f);
      }
      hotseat.addView(slotView, new LinearLayout.LayoutParams(0, -1, 1));
    }
  }

  void refreshBadges() {
    if (screen != null) refreshBadgeTree(screen);
  }

  void refreshBadgeTree(View view) {
    Object tag = view.getTag();
    if (tag instanceof String && ((String) tag).startsWith("badge:")) {
      String name = ((String) tag).substring(6);
      view.setVisibility(
          a.settings.optBoolean("notificationDots", true)
                  && a.prefs
                      .getStringSet("badgePackages", java.util.Collections.emptySet())
                      .contains(name)
              ? View.VISIBLE
              : View.GONE);
    }
    if (view instanceof ViewGroup)
      for (int n = 0; n < ((ViewGroup) view).getChildCount(); n++)
        refreshBadgeTree(((ViewGroup) view).getChildAt(n));
  }

  void selectDock(int slot) {
    drawer(
        app -> {
          try {
            a.dock.put(slot, app.key);
          } catch (Exception ignored) {
          }
          a.save();
          renderDock();
        });
  }

  void changeState(String next) {
    state = next;
  }

  void close() {
    if (overlay == null) return;
    FrameLayout old = overlay;
    boolean closingFolder = state.equals("folder");
    LinearLayout panel = activePanel;
    Rect origin = folderOrigin;
    overlay = null;
    changeState("home");
    if (closingFolder && panel != null && origin != null && duration() > 0) {
      panel.animate().cancel();
      int[] at = new int[2];
      panel.getLocationOnScreen(at);
      float naturalX = at[0] - panel.getTranslationX(), naturalY = at[1] - panel.getTranslationY();
      panel
          .animate()
          .translationX(origin.left - naturalX)
          .translationY(origin.top - naturalY)
          .scaleX(origin.width() / (float) Math.max(1, panel.getWidth()))
          .scaleY(origin.height() / (float) Math.max(1, panel.getHeight()))
          .alpha(0)
          .setDuration(duration() / 2)
          .start();
    }
    ((InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(old.getWindowToken(), 0);
    if (duration() == 0) {
      screen.removeView(old);
      return;
    }
    old.animate()
        .alpha(0)
        .translationY(d(26))
        .setDuration(duration() / 2)
        .withEndAction(() -> screen.removeView(old))
        .start();
  }

  boolean back() {
    if (overlay != null) {
      close();
      return true;
    }
    if (resizing != null) {
      resizing = null;
      for (HomeActivity.Board b : boards) b.invalidate();
      return true;
    }
    if (dragging != null) {
      endDrag();
      return true;
    }
    return false;
  }

  LinearLayout surface(String name, boolean full) {
    close();
    overlay = new FrameLayout(a);
    overlay.setBackgroundColor(0x99000000);
    screen.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    overlay.setOnClickListener(v -> close());
    LinearLayout content = vertical();
    activePanel = content;
    content.setClickable(true);
    content.setPadding(d(22), d(20), d(22), d(18));
    content.setBackground(a.shape(a.bg, full ? 0 : 32));
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(
            -1, full ? -1 : Math.max(d(360), (int) (screen.getHeight() * .78)), Gravity.BOTTOM);
    overlay.addView(content, lp);
    LinearLayout header = new LinearLayout(a);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView label = title(name, 28);
    header.addView(label, new LinearLayout.LayoutParams(0, d(54), 1));
    header.addView(button("×", () -> close()), new LinearLayout.LayoutParams(d(48), d(48)));
    content.addView(header);
    header.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            sheetDownY = event.getRawY();
            return true;
          }
          if (event.getAction() == MotionEvent.ACTION_MOVE) {
            content.animate().cancel();
            content.setTranslationY(Math.max(0, event.getRawY() - sheetDownY));
            return true;
          }
          if (event.getAction() == MotionEvent.ACTION_UP
              || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (content.getTranslationY() > d(100)) close();
            else content.animate().translationY(0).setDuration(duration()).start();
            return true;
          }
          return false;
        });
    content.setTranslationY(d(70));
    content.setAlpha(0);
    content
        .animate()
        .translationY(0)
        .alpha(1)
        .setDuration(duration())
        .setInterpolator(new DecelerateInterpolator(1.6f))
        .start();
    return content;
  }

  void overview() {
    changeState("overview");
    LinearLayout content = surface("Home screen", false);
    content.addView(a.label("PAGES"));
    HorizontalScrollView sv = new HorizontalScrollView(a);
    LinearLayout previews = new LinearLayout(a);
    for (int p = 0; p < pages(); p++) {
      final int id = p;
      TextView preview =
          button(
              (p == a.page ? "●  " : "") + "Page " + (p + 1),
              () -> {
                close();
                goPage(id, true);
              });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d(126), d(150));
      lp.setMargins(d(4), d(12), d(4), d(18));
      previews.addView(preview, lp);
    }
    previews.addView(
        button(
            "＋ Page",
            () -> {
              if (pages() >= 64) return;
              HomeActivity.put(a.settings, "pages", pages() + 1);
              a.save();
              close();
              a.page = pages() - 1;
              refresh();
            }),
        new LinearLayout.LayoutParams(d(95), d(150)));
    sv.addView(previews);
    content.addView(sv);
    content.addView(
        button("Widgets", () -> widgetPicker()), new LinearLayout.LayoutParams(-1, d(58)));
    content.addView(
        button("Customisation", () -> customise()), new LinearLayout.LayoutParams(-1, d(58)));
    content.addView(
        button(
            "Add apps",
            () ->
                drawer(
                    app -> {
                      addApp(app);
                      close();
                    })),
        new LinearLayout.LayoutParams(-1, d(58)));
    content.addView(
        button("Home settings", () -> a.legacySettings()),
        new LinearLayout.LayoutParams(-1, d(58)));
  }

  void addApp(HomeActivity.App app) {
    JSONObject x = a.obj("app");
    HomeActivity.put(x, "key", app.key);
    HomeActivity.put(x, "label", app.label);
    addItem(x);
  }

  void addItem(JSONObject x) {
    try {
      JSONArray candidate = new JSONArray(a.items.toString());
      candidate.put(x);
      a.items = WorkspaceModel.normalise(candidate, a.cols(), rows());
      a.save();
      close();
      refresh();
      int at = a.index(x.optString("id"));
      if (at >= 0) goPage(a.items.optJSONObject(at).optInt("page"), true);
    } catch (Exception e) {
      a.toast("No room; existing layout preserved");
    }
  }

  void beginDrag(View view, JSONObject x, boolean fresh) {
    if (a.settings.optBoolean("locked", false)) {
      a.toast("Unlock the layout in customisation");
      return;
    }
    dragging = x;
    dragNew = fresh;
    dropChanged = false;
    dragOrigin = view;
    boolean started =
        view.startDragAndDrop(
            ClipData.newPlainText("workspace", x.optString("id")),
            new View.DragShadowBuilder(view),
            x,
            View.DRAG_FLAG_OPAQUE);
    if (!started) {
      dragging = null;
      return;
    }
    close();
    dragBar.setVisibility(fresh ? View.GONE : View.VISIBLE);
    if (!fresh) view.setAlpha(.35f);
    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
  }

  void endDrag() {
    if (dragging == null) return;
    JSONObject old = dragging;
    boolean showActions = !dropChanged && !dragNew;
    dragging = null;
    dragNew = false;
    sourceFolder = null;
    sourceKey = null;
    dragBar.setVisibility(View.GONE);
    if (dragOrigin != null) dragOrigin.setAlpha(1);
    for (HomeActivity.Board b : boards) b.invalidate();
    if (showActions) handler.post(() -> itemActions(old, dragOrigin));
  }

  void removeSource(String destination) {
    if (sourceFolder == null || sourceFolder.equals(destination)) return;
    int at = a.index(sourceFolder);
    if (at >= 0) {
      JSONArray keys = a.items.optJSONObject(at).optJSONArray("children");
      if (keys != null)
        for (int n = 0; n < keys.length(); n++)
          if (keys.optString(n).equals(sourceKey)) {
            keys.remove(n);
            break;
          }
    }
  }

  void remove(JSONObject x) {
    int at = a.index(x.optString("id"));
    if (at >= 0) {
      if (x.optString("type").equals("widget")) a.host.deleteAppWidgetId(x.optInt("widgetId"));
      a.items.remove(at);
      a.save();
      refresh();
    }
  }

  boolean drop(int page, int col, int row, JSONObject target) {
    JSONObject x = dragging;
    if (x == null) return false;
    try {
      boolean same =
          !dragNew && x.optInt("page") == page && x.optInt("x") == col && x.optInt("y") == row;
      if (same) return true;
      if (target != null
          && !target.optString("id").equals(x.optString("id"))
          && x.optString("type").equals("app")
          && (target.optString("type").equals("folder")
              || target.optString("type").equals("app"))) {
        JSONObject folder = target;
        if (target.optString("type").equals("app")) {
          folder = a.obj("folder");
          HomeActivity.put(folder, "page", target.optInt("page"));
          HomeActivity.put(folder, "x", target.optInt("x"));
          HomeActivity.put(folder, "y", target.optInt("y"));
          HomeActivity.put(folder, "name", "Folder");
          HomeActivity.put(folder, "children", new JSONArray().put(target.optString("key")));
          a.items.remove(a.index(target.optString("id")));
          a.items.put(folder);
        }
        JSONArray keys = folder.optJSONArray("children");
        if (keys == null) {
          keys = new JSONArray();
          HomeActivity.put(folder, "children", keys);
        }
        boolean duplicate = false;
        for (int n = 0; n < keys.length(); n++)
          if (keys.optString(n).equals(x.optString("key"))) duplicate = true;
        if (!duplicate) keys.put(x.optString("key"));
        if (!dragNew) {
          int at = a.index(x.optString("id"));
          if (at >= 0) a.items.remove(at);
        }
        removeSource(folder.optString("id"));
        dropChanged = true;
        a.save();
        refresh();
        return true;
      }
      if (!WorkspaceModel.fits(
          a.items,
          dragNew ? null : x.optString("id"),
          page,
          col,
          row,
          x.optInt("w", 1),
          x.optInt("h", 1),
          a.cols(),
          rows())) {
        a.toast("No room here");
        return false;
      }
      HomeActivity.put(x, "page", page);
      HomeActivity.put(x, "x", col);
      HomeActivity.put(x, "y", row);
      if (dragNew) a.items.put(x);
      removeSource(null);
      dropChanged = true;
      a.save();
      refresh();
      return true;
    } catch (Exception e) {
      a.toast("Couldn't place item");
      return false;
    }
  }

  void itemActions(JSONObject x, View anchor) {
    LinearLayout body =
        surface(x.optString("label", x.optString("name", x.optString("type"))), false);
    String type = x.optString("type");
    body.addView(
        button(
            "Resize on Home",
            () -> {
              resizing = x;
              close();
              for (HomeActivity.Board b : boards) b.invalidate();
              a.toast("Drag the lower-right resize handle; Back cancels");
            }),
        new LinearLayout.LayoutParams(-1, d(54)));
    if (type.equals("photo")) {
      body.addView(
          button(
              "Photo crop position",
              () ->
                  a.menu(
                      "Crop position",
                      new String[] {"Top", "Centre", "Bottom"},
                      n -> {
                        HomeActivity.put(x, "crop", n);
                        a.save();
                        close();
                        refresh();
                      })),
          new LinearLayout.LayoutParams(-1, d(54)));
    }
    if (!type.equals("app") && !type.equals("folder") && !type.equals("widget")) {
      body.addView(
          button("Style & size", () -> stylePicker(type, x)),
          new LinearLayout.LayoutParams(-1, d(54)));
    }
    if (!type.equals("app")
        && !type.equals("folder")
        && !type.equals("widget")
        && !type.equals("photo")
        && !type.equals("media")) {
      body.addView(
          button(
              "Widget colour",
              () ->
                  a.menu(
                      "Widget colour",
                      new String[] {"Theme", "Chalk", "Ink", "Sage", "Red"},
                      n -> {
                        HomeActivity.put(x, "tone", n);
                        a.save();
                        close();
                        refresh();
                      })),
          new LinearLayout.LayoutParams(-1, d(54)));
      body.addView(
          button(
              "Background opacity",
              () ->
                  a.menu(
                      "Background opacity",
                      new String[] {"Solid", "85%", "60%", "Transparent"},
                      n -> {
                        HomeActivity.put(x, "opacity", new int[] {100, 85, 60, 0}[n]);
                        a.save();
                        close();
                        refresh();
                      })),
          new LinearLayout.LayoutParams(-1, d(54)));
    }
    if (type.equals("app") || type.equals("folder")) {
      body.addView(
          button(
              x.optInt("w", 1) == 1 ? "Enlarge" : "Shrink",
              () -> resize(x, x.optInt("w", 1) == 1 ? 2 : 1, x.optInt("w", 1) == 1 ? 2 : 1)),
          new LinearLayout.LayoutParams(-1, d(54)));
    }
    if (type.equals("folder")) {
      body.addView(
          button("Folder cover", () -> folderCovers(x)), new LinearLayout.LayoutParams(-1, d(54)));
      body.addView(
          button(
              "Rename",
              () ->
                  a.input(
                      "Folder name",
                      x.optString("name"),
                      s -> {
                        HomeActivity.put(x, "name", s);
                        a.save();
                        close();
                        refresh();
                      })),
          new LinearLayout.LayoutParams(-1, d(54)));
    }
    if (type.equals("widget"))
      body.addView(
          button("Configure", () -> a.configureExisting(x)),
          new LinearLayout.LayoutParams(-1, d(54)));
    if (type.equals("app"))
      body.addView(
          button("App info & shortcuts", () -> a.appOptions(a.app(x.optString("key")))),
          new LinearLayout.LayoutParams(-1, d(54)));
    body.addView(
        button(
            "Move to next page",
            () -> {
              HomeActivity.put(x, "page", Math.min(63, x.optInt("page") + 1));
              x.remove("x");
              x.remove("y");
              a.save();
              close();
              refresh();
            }),
        new LinearLayout.LayoutParams(-1, d(54)));
    body.addView(
        button(
            "Remove from Home",
            () -> {
              remove(x);
              close();
            }),
        new LinearLayout.LayoutParams(-1, d(54)));
  }

  void resize(JSONObject x, int w, int h) {
    if (WorkspaceModel.fits(
        a.items,
        x.optString("id"),
        x.optInt("page"),
        x.optInt("x"),
        x.optInt("y"),
        w,
        h,
        a.cols(),
        rows())) {
      HomeActivity.put(x, "w", w);
      HomeActivity.put(x, "h", h);
      a.save();
      close();
      refresh();
    } else a.toast("Move this item to make room for this size");
  }

  void folderCovers(JSONObject folder) {
    LinearLayout body = surface("Folder appearance", false);
    GridLayout grid = new GridLayout(a);
    grid.setColumnCount(3);
    for (String cover : new String[] {"Apps", "◉", "✳", "▦", "♡", "◈", "☰", "＋", "◎"}) {
      TextView v =
          button(
              cover,
              () -> {
                HomeActivity.put(folder, "cover", cover.equals("Apps") ? "" : cover);
                a.save();
                close();
                refresh();
              });
      grid.addView(v, new ViewGroup.LayoutParams(d(96), d(86)));
    }
    body.addView(grid);
  }

  Rect itemBounds(JSONObject item) {
    for (HomeActivity.Board b : boards)
      for (int n = 0; n < b.visible.size(); n++)
        if (b.visible.get(n).optString("id").equals(item.optString("id"))) {
          View view = b.getChildAt(n);
          int[] at = new int[2];
          view.getLocationOnScreen(at);
          return new Rect(at[0], at[1], at[0] + view.getWidth(), at[1] + view.getHeight());
        }
    return null;
  }

  void folder(JSONObject x) {
    Rect origin = itemBounds(x);
    LinearLayout body = surface(x.optString("name", "Folder"), false);
    changeState("folder");
    folderOrigin = origin;
    body.post(
        () -> {
          if (origin == null || duration() == 0) return;
          body.animate().cancel();
          body.setTranslationY(0);
          int[] at = new int[2];
          body.getLocationOnScreen(at);
          body.setPivotX(0);
          body.setPivotY(0);
          body.setTranslationX(origin.left - at[0]);
          body.setTranslationY(origin.top - at[1]);
          body.setScaleX(origin.width() / (float) Math.max(1, body.getWidth()));
          body.setScaleY(origin.height() / (float) Math.max(1, body.getHeight()));
          body.setAlpha(.4f);
          body.animate()
              .translationX(0)
              .translationY(0)
              .scaleX(1)
              .scaleY(1)
              .alpha(1)
              .setDuration(duration())
              .setInterpolator(new DecelerateInterpolator(1.6f))
              .start();
        });
    ScrollView sc = new ScrollView(a);
    GridLayout grid = new GridLayout(a);
    grid.setColumnCount(3);
    JSONArray keys = x.optJSONArray("children");
    if (keys == null) {
      keys = new JSONArray();
      HomeActivity.put(x, "children", keys);
    }
    final JSONArray children = keys;
    for (int n = 0; n < keys.length(); n++) {
      final String key = keys.optString(n);
      HomeActivity.App app = a.app(key);
      if (app == null) continue;
      View icon = a.appView(app, true);
      grid.addView(
          icon,
          new ViewGroup.LayoutParams(
              (a.getResources().getDisplayMetrics().widthPixels - d(44)) / 3, d(100)));
      icon.setOnLongClickListener(
          v -> {
            JSONObject item = a.obj("app");
            HomeActivity.put(item, "key", app.key);
            HomeActivity.put(item, "label", app.label);
            item.remove("x");
            beginDrag(v, item, true);
            sourceFolder = x.optString("id");
            sourceKey = key;
            return true;
          });
    }
    sc.addView(grid);
    body.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
    body.addView(
        button(
            "＋ Add apps",
            () ->
                drawer(
                    app -> {
                      children.put(app.key);
                      a.save();
                      refresh();
                      folder(x);
                    })),
        new LinearLayout.LayoutParams(-1, d(52)));
  }

  void drawer(Consumer<HomeActivity.App> pick) {
    LinearLayout body = surface(pick == null ? "Apps" : "Choose an app", true);
    changeState("drawer");
    a.search = new EditText(a);
    EditText search = a.search;
    search.setSingleLine();
    search.setTextColor(a.fg);
    search.setHintTextColor(a.muted);
    search.setHint("Search apps");
    search.setPadding(d(18), 0, d(18), 0);
    search.setBackground(a.shape(a.card, 28));
    search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
    LinearLayout modes = new LinearLayout(a);
    final int[] group = {-1};
    TextView mode = button("All apps ▾", () -> {});
    modes.addView(mode, new LinearLayout.LayoutParams(-1, d(44)));
    body.addView(modes);
    ScrollView scroll = new ScrollView(a);
    GridLayout grid = new GridLayout(a);
    grid.setColumnCount(a.cols());
    scroll.addView(grid);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    body.addView(search, new LinearLayout.LayoutParams(-1, d(54)));
    final ArrayList<HomeActivity.App> matched = new ArrayList<>();
    Runnable filter =
        () -> {
          grid.removeAllViews();
          matched.clear();
          String q = search.getText().toString().toLowerCase(Locale.ROOT).trim();
          for (HomeActivity.App app : a.apps)
            if (app.label.toLowerCase(Locale.ROOT).contains(q)
                && (!a.hidden(app.key) || !q.isEmpty())
                && (group[0] < 0 || a.categoryOf(app) == group[0])) matched.add(app);
          int width = (a.getResources().getDisplayMetrics().widthPixels - d(44)) / a.cols();
          for (HomeActivity.App app : matched) {
            View view = a.appView(app, true);
            grid.addView(view, new ViewGroup.LayoutParams(width, d(98)));
            a.click(
                view,
                () -> {
                  close();
                  if (pick == null) a.launch(app.key);
                  else pick.accept(app);
                });
            view.setOnLongClickListener(
                v -> {
                  if (pick != null) {
                    pick.accept(app);
                    close();
                    return true;
                  }
                  JSONObject x = a.obj("app");
                  HomeActivity.put(x, "key", app.key);
                  HomeActivity.put(x, "label", app.label);
                  beginDrag(v, x, true);
                  return true;
                });
          }
          if (matched.isEmpty()) grid.addView(title("No matching apps", 16));
        };
    a.click(
        mode,
        () ->
            a.menu(
                "App organisation",
                new String[] {
                  "All apps",
                  "Games",
                  "Audio",
                  "Video",
                  "Images",
                  "Social",
                  "News",
                  "Maps",
                  "Productivity",
                  "Accessibility",
                  "Manage hidden apps"
                },
                n -> {
                  if (n == 10) {
                    hiddenApps();
                    return;
                  }
                  group[0] = n - 1;
                  mode.setText(
                      n == 0
                          ? "All apps ▾"
                          : new String[] {
                                    "Games",
                                    "Audio",
                                    "Video",
                                    "Images",
                                    "Social",
                                    "News",
                                    "Maps",
                                    "Productivity",
                                    "Accessibility"
                                  }
                                  [n - 1]
                              + " ▾");
                  filter.run();
                }));
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int f) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            filter.run();
          }

          public void afterTextChanged(android.text.Editable e) {}
        });
    search.setOnEditorActionListener(
        (v, id, event) -> {
          if (matched.isEmpty()) return false;
          HomeActivity.App app = matched.get(0);
          close();
          if (pick == null) a.launch(app.key);
          else pick.accept(app);
          return true;
        });
    filter.run();
    body.setOnApplyWindowInsetsListener(
        (v, in) -> {
          int bottom = in.getInsets(WindowInsets.Type.ime()).bottom;
          body.setPadding(d(22), d(20), d(22), Math.max(d(18), bottom));
          return in;
        });
  }

  void hiddenApps() {
    LinearLayout body = surface("Hidden apps", true);
    ScrollView scroll = new ScrollView(a);
    LinearLayout list = vertical();
    for (HomeActivity.App app : a.apps) {
      TextView v =
          button(
              (a.hidden(app.key) ? "●  " : "○  ") + app.label,
              () -> {
                JSONObject hidden = a.settings.optJSONObject("hidden");
                if (hidden == null) {
                  hidden = new JSONObject();
                  HomeActivity.put(a.settings, "hidden", hidden);
                }
                HomeActivity.put(hidden, app.key, !a.hidden(app.key));
                a.save();
                hiddenApps();
              });
      list.addView(v, new LinearLayout.LayoutParams(-1, d(54)));
    }
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
  }

  final String[] types = {
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
    "torch",
    "bluetooth",
    "sound",
    "dnd",
    "breathing",
    "timer"
  };
  final String[] names = {
    "Clock",
    "Date",
    "Battery",
    "Media player",
    "Quick look",
    "Photos",
    "Notes",
    "Countdown",
    "Compass",
    "Screen time",
    "Flashlight",
    "Bluetooth",
    "Sound",
    "Do Not Disturb",
    "Breathing",
    "Elapsed timer"
  };

  void widgetPicker() {
    LinearLayout body = surface("Widgets", true);
    changeState("picker");
    EditText search = new EditText(a);
    search.setSingleLine();
    search.setTextColor(a.fg);
    search.setHintTextColor(a.muted);
    search.setHint("Search widgets");
    search.setBackground(a.shape(a.card, 24));
    search.setPadding(d(16), 0, d(16), 0);
    body.addView(search, new LinearLayout.LayoutParams(-1, d(48)));
    ScrollView scroll = new ScrollView(a);
    LinearLayout list = vertical();
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    Runnable populate =
        () -> {
          list.removeAllViews();
          String q = search.getText().toString().toLowerCase(Locale.ROOT);
          for (int n = 0; n < types.length; n++) {
            final String type = types[n];
            if (!names[n].toLowerCase(Locale.ROOT).contains(q)) continue;
            LinearLayout row = vertical();
            TextView heading = title(names[n], 20);
            row.addView(heading, new LinearLayout.LayoutParams(-1, d(46)));
            JSONObject preview = sample(type, 0);
            WidgetArtwork art = new WidgetArtwork(a, preview, true);
            row.addView(art, new LinearLayout.LayoutParams(-1, d(142)));
            TextView sizes = a.label("STYLES & SIZES  →");
            row.addView(sizes, new LinearLayout.LayoutParams(-1, d(32)));
            a.click(row, () -> stylePicker(type, null));
            a.click(art, () -> stylePicker(type, null));
            art.setOnLongClickListener(
                v -> {
                  beginDrag(v, sample(type, 0), true);
                  return true;
                });
            LinearLayout.LayoutParams rowSpace = new LinearLayout.LayoutParams(-1, -2);
            rowSpace.bottomMargin = d(12);
            list.addView(row, rowSpace);
          }
          List<AppWidgetProviderInfo> providers =
              AppWidgetManager.getInstance(a).getInstalledProviders();
          providers.sort(
              (x, y) ->
                  x.loadLabel(a.getPackageManager())
                      .compareToIgnoreCase(y.loadLabel(a.getPackageManager())));
          for (AppWidgetProviderInfo info : providers) {
            String name = info.loadLabel(a.getPackageManager());
            if (!name.toLowerCase(Locale.ROOT).contains(q)) continue;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(d(8), d(12), d(8), d(12));
            ImageView image = new ImageView(a);
            try {
              Drawable preview =
                  info.loadPreviewImage(a, a.getResources().getDisplayMetrics().densityDpi);
              if (preview == null)
                preview = info.loadIcon(a, a.getResources().getDisplayMetrics().densityDpi);
              image.setImageDrawable(preview);
            } catch (Exception ignored) {
            }
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(image, new LinearLayout.LayoutParams(d(110), d(90)));
            LinearLayout texts = vertical();
            texts.addView(title(name, 17));
            texts.addView(a.label("ANDROID WIDGET"));
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            a.click(
                row,
                () -> {
                  close();
                  a.addProvider(info);
                });
            list.addView(row);
          }
          list.addView(a.label("PREVIEWS USE SAMPLE DATA · ALL BUILT-INS WORK OFFLINE"));
        };
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int f) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            populate.run();
          }

          public void afterTextChanged(android.text.Editable e) {}
        });
    populate.run();
  }

  JSONObject sample(String type, int style) {
    JSONObject x = a.obj(type);
    HomeActivity.put(x, "style", style);
    HomeActivity.put(
        x, "w", type.equals("clock") || type.equals("media") || type.equals("agenda") ? 4 : 2);
    HomeActivity.put(
        x,
        "h",
        type.equals("clock")
                || type.equals("media")
                || type.equals("photo")
                || type.equals("breathing")
            ? 2
            : 1);
    if (Arrays.asList("torch", "bluetooth", "sound", "dnd").contains(type)) {
      HomeActivity.put(x, "w", 1);
      HomeActivity.put(x, "h", 1);
    }
    return x;
  }

  void stylePicker(String type, JSONObject existing) {
    LinearLayout body = surface("Choose a style", true);
    ScrollView scroll = new ScrollView(a);
    LinearLayout list = vertical();
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    for (int style = 0; style < (type.equals("clock") ? 5 : 3); style++) {
      final int variant = style;
      JSONObject candidate = sample(type, style);
      if (existing != null)
        try {
          candidate = new JSONObject(existing.toString());
          HomeActivity.put(candidate, "style", style);
        } catch (Exception ignored) {
        }
      final JSONObject choice = candidate;
      WidgetArtwork preview = new WidgetArtwork(a, choice, true);
      LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, d(165));
      ap.setMargins(0, d(12), 0, d(8));
      list.addView(preview, ap);
      list.addView(
          a.label(
              (type.equals("clock")
                      ? new String[] {
                            "DOT MATRIX", "ANALOGUE", "SERIF", "TICK DIAL", "ROUNDED DIGITAL"
                          }
                          [style]
                      : (type.equals("photo")
                              ? new String[] {"RECTANGLE", "CIRCLE", "STICKER"}
                              : new String[] {"CLASSIC", "INVERTED", "MINIMAL"})
                          [style])
                  + " · SAMPLE PREVIEW"));
      LinearLayout sizes = new LinearLayout(a);
      int[][] options =
          type.equals("clock") || type.equals("media") || type.equals("breathing")
              ? new int[][] {{2, 2}, {4, 2}}
              : new int[][] {{1, 1}, {2, 1}, {2, 2}};
      for (int[] span : options) {
        final int w = Math.min(a.cols(), span[0]), h = span[1];
        sizes.addView(
            button(
                w + " × " + h,
                () -> {
                  if (existing != null) {
                    if (!WorkspaceModel.fits(
                        a.items,
                        existing.optString("id"),
                        existing.optInt("page"),
                        existing.optInt("x"),
                        existing.optInt("y"),
                        w,
                        h,
                        a.cols(),
                        rows())) {
                      a.toast("No room for that size here");
                      return;
                    }
                    HomeActivity.put(existing, "style", variant);
                    HomeActivity.put(existing, "w", w);
                    HomeActivity.put(existing, "h", h);
                    a.save();
                    close();
                    refresh();
                  } else {
                    HomeActivity.put(choice, "w", w);
                    HomeActivity.put(choice, "h", h);
                    addItem(choice);
                  }
                }),
            new LinearLayout.LayoutParams(0, d(52), 1));
      }
      list.addView(sizes);
      preview.setOnLongClickListener(
          v -> {
            if (existing == null) beginDrag(v, choice, true);
            return true;
          });
    }
  }

  void referencePage() {
    if (pages() >= 64) {
      a.toast("Page limit reached");
      return;
    }
    int next = pages();
    a.referenceItems(next);
    HomeActivity.put(a.settings, "pages", next + 1);
    a.save();
    close();
    refresh();
    goPage(next, true);
  }

  void wallpaperPresets() {
    LinearLayout body = surface("Wallpaper & colour", true);
    for (String preset : new String[] {"ocean", "mist", "solid"}) {
      FrameLayout preview = new FrameLayout(a);
      preview.setBackground(
          preset.equals("solid")
              ? new android.graphics.drawable.ColorDrawable(a.bg)
              : new WallpaperArtwork(preset));
      TextView label =
          button(
              preset.equals("ocean")
                  ? "Ocean"
                  : preset.equals("mist") ? "Soft mist" : "Solid background",
              () -> {
                HomeActivity.put(a.settings, "wallpaperPreset", preset);
                a.settings.remove("wallpaper");
                HomeActivity.put(a.settings, "dark", !preset.equals("mist"));
                a.save();
                refresh();
                close();
              });
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, d(50), Gravity.BOTTOM);
      preview.addView(label, lp);
      LinearLayout.LayoutParams card = new LinearLayout.LayoutParams(-1, d(155));
      card.bottomMargin = d(12);
      body.addView(preview, card);
    }
  }

  void customise() {
    LinearLayout body = surface("Customisation", true);
    ScrollView scroll = new ScrollView(a);
    LinearLayout list = vertical();
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    WidgetArtwork sample = new WidgetArtwork(a, sample("clock", 1), true);
    list.addView(sample, new LinearLayout.LayoutParams(-1, d(175)));
    list.addView(a.label("LIVE THEME PREVIEW"));
    option(
        list,
        "Appearance",
        a.dark() ? "Dark" : "Light",
        () -> {
          HomeActivity.put(a.settings, "dark", !a.dark());
          if (a.settings.optString("wallpaper", "").isEmpty()
              && !a.settings.optString("wallpaperPreset", "solid").equals("solid"))
            HomeActivity.put(a.settings, "wallpaperPreset", a.dark() ? "ocean" : "mist");
          a.save();
          refresh();
          customise();
        });
    option(list, "App grid", a.cols() + " columns · " + rows() + " rows", () -> gridPicker());
    option(
        list,
        "App labels",
        a.settings.optBoolean("labels", true) ? "Shown" : "Hidden",
        () -> {
          HomeActivity.put(a.settings, "labels", !a.settings.optBoolean("labels", true));
          a.save();
          refresh();
          customise();
        });
    option(
        list,
        "Icon treatment",
        a.settings.optBoolean("mono", true) ? "Monochrome" : "Original colour",
        () -> {
          HomeActivity.put(a.settings, "mono", !a.settings.optBoolean("mono", true));
          a.icons.clear();
          a.save();
          refresh();
          customise();
        });
    option(
        list,
        "Styled page",
        "Add a sparse layout inspired by your references",
        () -> referencePage());
    option(list, "Wallpaper & colour", "Original offline presets", () -> wallpaperPresets());
    option(list, "Setup & permissions", "Default Home and optional feature access", () -> setup());
    option(list, "Icon pack", "Choose installed pack", () -> a.chooseIconPack());
    option(
        list,
        "Wallpaper",
        "Choose a local image",
        () -> a.pickDocument(HomeActivity.WALL, "image/*"));
    option(
        list,
        "Wallpaper",
        "Use solid background",
        () -> {
          a.settings.remove("wallpaper");
          HomeActivity.put(a.settings, "wallpaperPreset", "solid");
          a.save();
          refresh();
          customise();
        });
    option(
        list,
        "Layout lock",
        a.settings.optBoolean("locked", false) ? "Locked" : "Unlocked",
        () -> {
          HomeActivity.put(a.settings, "locked", !a.settings.optBoolean("locked", false));
          a.save();
          customise();
        });
    option(
        list,
        "Notification dots",
        a.settings.optBoolean("notificationDots", true)
            ? "Enabled · requires notification access"
            : "Disabled",
        () -> {
          HomeActivity.put(
              a.settings, "notificationDots", !a.settings.optBoolean("notificationDots", true));
          a.save();
          refreshBadges();
          customise();
        });
    option(
        list,
        "Notification access",
        "Optional media controls & dots",
        () -> a.open(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    option(
        list,
        "Reduce motion",
        a.settings.optBoolean("reduceMotion", false) ? "Enabled" : "Disabled",
        () -> {
          HomeActivity.put(
              a.settings, "reduceMotion", !a.settings.optBoolean("reduceMotion", false));
          a.save();
          customise();
        });
    option(list, "Gesture navigation", "HyperOS setup and integration", () -> navigation());
    option(list, "Backups, default Home & more", "Advanced settings", () -> a.legacySettings());
  }

  void option(LinearLayout list, String name, String value, Runnable action) {
    LinearLayout row = vertical();
    row.setPadding(d(16), d(14), d(16), d(14));
    row.setBackground(a.shape(a.card, 22));
    row.addView(title(name, 18));
    row.addView(a.text(value, 13, a.muted));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, d(82));
    lp.setMargins(0, d(8), 0, 0);
    list.addView(row, lp);
    a.click(row, action);
  }

  void gridPicker() {
    LinearLayout body = surface("Home layout", true);
    GridLayout choices = new GridLayout(a);
    choices.setColumnCount(2);
    for (int n = 0; n < 4; n++) {
      final int columns = n < 2 ? 4 : 5, rows = n % 2 == 0 ? 6 : 7;
      LinearLayout option = vertical();
      option.setPadding(d(12), d(12), d(12), d(12));
      option.setBackground(a.shape(a.card, 24));
      View preview =
          new View(a) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas c) {
              float sx = getWidth() / (float) (columns + 1), sy = getHeight() / (float) (rows + 1);
              paint.setColor(a.fg);
              for (int y = 1; y <= rows; y++)
                for (int x = 1; x <= columns; x++) c.drawCircle(x * sx, y * sy, d(3), paint);
            }
          };
      option.addView(preview, new LinearLayout.LayoutParams(-1, d(190)));
      TextView label =
          title(columns + " × " + rows + (a.cols() == columns && rows() == rows ? "  ●" : ""), 17);
      label.setGravity(Gravity.CENTER);
      option.addView(label);
      GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
      lp.width = (a.getResources().getDisplayMetrics().widthPixels - d(62)) / 2;
      lp.height = d(245);
      lp.setMargins(d(3), d(5), d(3), d(5));
      choices.addView(option, lp);
      a.click(
          option,
          () -> {
            try {
              JSONArray next = WorkspaceModel.normalise(a.items, columns, rows);
              HomeActivity.put(a.settings, "columns", columns);
              HomeActivity.put(a.settings, "rows", rows);
              a.items = next;
              a.save();
              refresh();
              customise();
            } catch (Exception e) {
              a.toast("Layout change could not be applied");
            }
          });
    }
    body.addView(choices);
  }

  void setup() {
    LinearLayout body = surface("Welcome to Dot Home", true);
    changeState("setup");
    ScrollView sc = new ScrollView(a);
    LinearLayout list = vertical();
    sc.addView(list);
    body.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
    TextView intro =
        a.text(
            "Your home screen, entirely offline. Choose the features you want to enable. No"
                + " Internet permission or online option is included.",
            16,
            a.muted);
    intro.setPadding(0, d(12), 0, d(20));
    list.addView(intro);
    option(
        list,
        "Choose default Home",
        "Use Android’s launcher selection dialog",
        () -> a.requestHomeRole());
    option(
        list,
        "Set up gesture navigation",
        "Enable optional Accessibility edge actions",
        () -> navigation());
    option(list, "Widget & media access", "Review optional permissions", () -> permissions());
    TextView note =
        a.text(
            "Android does not show an all-permissions prompt at launch. Core Home needs no runtime"
                + " grants. Accessibility and notification access require separate system settings;"
                + " widgets ask only when their features need access.",
            14,
            a.muted);
    note.setPadding(0, d(18), 0, d(18));
    list.addView(note);
    list.addView(
        button(
            "Start customising",
            () -> {
              a.prefs.edit().putBoolean("setupSeen2", true).apply();
              close();
              refresh();
            }),
        new LinearLayout.LayoutParams(-1, d(56)));
  }

  void permissions() {
    LinearLayout body = surface("Optional access", true);
    ScrollView sc = new ScrollView(a);
    LinearLayout list = vertical();
    sc.addView(list);
    body.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));
    option(
        list,
        "Gesture navigation",
        GestureNavigationService.active != null ? "Running" : "Accessibility access is off",
        () -> navigation());
    option(
        list,
        "Calendar widgets",
        a.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ? "Allowed"
            : "Read events for Quick look",
        () -> a.requestPermissions(new String[] {android.Manifest.permission.READ_CALENDAR}, 21));
    option(
        list,
        "Flashlight",
        a.checkSelfPermission(android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ? "Allowed"
            : "Camera permission controls the torch",
        () -> a.requestPermissions(new String[] {android.Manifest.permission.CAMERA}, 22));
    option(
        list,
        "Media & notification dots",
        "Enable Dot Home in Notification access",
        () -> a.open(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
    option(
        list,
        "Screen time",
        "Optional usage access",
        () -> a.open(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)));
    option(
        list,
        "Photos",
        "Select individual images from the widget; no broad gallery permission",
        () -> a.toast("Add a Photos widget, then tap it to choose an image"));
  }

  void navigation() {
    LinearLayout body = surface("Gesture navigation", true);
    changeState("navigation");
    ScrollView scroll = new ScrollView(a);
    LinearLayout list = vertical();
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    int mode =
        android.provider.Settings.Global.getInt(a.getContentResolver(), "force_fsg_nav_bar", 0);
    boolean running = GestureNavigationService.active != null;
    list.addView(title(running ? "Edge gestures are running" : "Set up edge gestures", 22));
    TextView help =
        a.text(
            "Swipe inward from either side for Back. Swipe up from the bottom for Home; swipe up"
                + " and hold for Recents.\n\n"
                + "No ADB, computer or root is needed for these edge actions.\n\n"
                + "This optional Accessibility service uses narrow edge touch areas and does not"
                + " read your screen or typing. It recreates navigation actions; native HyperOS"
                + " transition animations remain controlled by the firmware.",
            15,
            a.muted);
    help.setPadding(0, d(16), 0, d(12));
    list.addView(help);
    option(
        list,
        "1 · Enable edge gestures",
        running ? "Running" : "Open Accessibility settings",
        () -> a.open(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    option(
        list,
        "2 · Add recovery tile",
        "Quick Settings entry back to this setup",
        () -> {
          android.app.StatusBarManager manager =
              (android.app.StatusBarManager) a.getSystemService(android.app.StatusBarManager.class);
          manager.requestAddTileService(
              new ComponentName(a, NavigationTileService.class),
              "Dot Home navigation",
              android.graphics.drawable.Icon.createWithResource(a, R.drawable.icon),
              a.getMainExecutor(),
              result ->
                  a.toast(
                      result == android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
                          ? "Recovery tile added"
                          : "Use Quick Settings edit to add Dot Home navigation"));
        });
    option(
        list,
        "3 · Hide the three-button bar",
        mode == 1 ? "System reports gesture mode" : "Open HyperOS navigation settings",
        () -> {
          if (GestureNavigationService.active == null) {
            a.toast("Enable edge gestures and add the recovery tile first");
            return;
          }
          a.open(new Intent("com.android.settings.NAVIGATION_MODE_SETTINGS"));
        });
    TextView note =
        a.text(
            "For a sideloaded APK, Android may first require App info → ⋮ → Allow restricted"
                + " settings. HyperOS may also need autostart/battery settings adjusted to keep the"
                + " service available. The vendor navigation screen can be unavailable or behave"
                + " differently by firmware build. Keep POCO Launcher installed.",
            13,
            a.muted);
    note.setPadding(0, d(14), 0, d(14));
    list.addView(note);
    option(
        list,
        "Restore system buttons",
        "Open navigation settings before disabling edge gestures",
        () -> a.open(new Intent("com.android.settings.NAVIGATION_MODE_SETTINGS")));
    option(
        list,
        "Disable edge gestures",
        "Restore system buttons first",
        () -> {
          if (android.provider.Settings.Global.getInt(
                  a.getContentResolver(), "force_fsg_nav_bar", 0)
              == 1) {
            a.toast("Restore system buttons in navigation settings first");
            a.open(new Intent("com.android.settings.NAVIGATION_MODE_SETTINGS"));
            return;
          }
          if (GestureNavigationService.active != null)
            GestureNavigationService.active.disableSelf();
          close();
        });
  }

  class Pager extends HorizontalScrollView {
    float downX, downY;
    VelocityTracker velocity;
    boolean vertical, drawerPull;

    Pager() {
      super(a);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
      if (dragging != null) return false;
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        downX = e.getX();
        downY = e.getY();
        vertical = false;
      }
      if (e.getAction() == MotionEvent.ACTION_MOVE
          && Math.abs(e.getY() - downY) > d(36)
          && Math.abs(e.getX() - downX) < d(30)) {
        vertical = true;
        return true;
      }
      return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        downX = e.getX();
        downY = e.getY();
        velocity = VelocityTracker.obtain();
      }
      if (velocity == null) velocity = VelocityTracker.obtain();
      velocity.addMovement(e);
      if (vertical) {
        float dy = e.getY() - downY;
        if (dy < 0 && e.getAction() == MotionEvent.ACTION_MOVE) {
          if (!drawerPull) {
            drawer(null);
            drawerPull = true;
            activePanel.animate().cancel();
          }
          activePanel.setTranslationY(Math.max(0, screen.getHeight() + dy * 2));
        }
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
          vertical = false;
          if (drawerPull) {
            drawerPull = false;
            if (dy < -d(70) && e.getAction() != MotionEvent.ACTION_CANCEL)
              activePanel
                  .animate()
                  .translationY(0)
                  .setDuration(duration())
                  .setInterpolator(new DecelerateInterpolator())
                  .start();
            else close();
          } else if (dy > d(50)) overview();
          if (velocity != null) {
            velocity.recycle();
            velocity = null;
          }
        }
        return true;
      }
      boolean result = super.onTouchEvent(e);
      if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
        velocity.computeCurrentVelocity(1000);
        int target = Math.round(getScrollX() / (float) Math.max(1, getWidth()));
        if (Math.abs(velocity.getXVelocity()) > d(600))
          target = a.page + (velocity.getXVelocity() < 0 ? 1 : -1);
        goPage(target, true);
        velocity.recycle();
        velocity = null;
      }
      return result;
    }

    @Override
    public void fling(int velocityX) {}
  }
}
