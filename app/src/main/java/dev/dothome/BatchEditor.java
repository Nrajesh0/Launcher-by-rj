package dev.dothome;

import android.graphics.Rect;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** In-place multi-selection with transactional group moves across workspace pages. */
final class BatchEditor {
  final LauncherUi ui;
  final HomeActivity a;
  final Set<String> selected = new LinkedHashSet<>();
  final Handler handler = new Handler(android.os.Looper.getMainLooper());
  boolean active, groupDragging, consuming, moved;
  LinearLayout bar;
  TextView count;
  float downX, downY;
  JSONObject pressed;
  View origin;
  final Runnable hold = this::startGroupMove;

  void startGroupMove() {
    if (active && pressed != null && selected.contains(pressed.optString("id"))) {
      groupDragging = true;
      ui.beginDrag(origin, pressed, false);
      if (ui.dragging == null) groupDragging = false;
    }
  }

  BatchEditor(LauncherUi ui) {
    this.ui = ui;
    a = ui.a;
  }

  void start() {
    if (a.settings.optBoolean("locked", false)) {
      a.toast("Unlock the layout first");
      return;
    }
    ui.close();
    active = true;
    selected.clear();
    bar = new LinearLayout(a);
    bar.setGravity(Gravity.CENTER_VERTICAL);
    bar.setPadding(ui.d(6), ui.d(4), ui.d(6), ui.d(4));
    bar.setBackground(a.shape(a.card, 24));
    count =
        ui.button(
            "0 selected",
            () -> {
              if (a.board != null)
                for (JSONObject item : a.board.visible) selected.add(item.optString("id"));
              update();
            });
    bar.addView(count, new LinearLayout.LayoutParams(0, -1, 1));
    bar.addView(ui.button("Move", this::choosePage), new LinearLayout.LayoutParams(-2, -1));
    bar.addView(ui.button("Done", this::finish), new LinearLayout.LayoutParams(-2, -1));
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, ui.d(56), Gravity.BOTTOM);
    lp.setMargins(ui.d(12), 0, ui.d(12), ui.d(64));
    ui.screen.addView(bar, lp);
    update();
    a.toast("Tap items to select. Hold a selected item to move the group.");
  }

  void update() {
    if (count != null) count.setText(selected.size() + " selected");
    for (HomeActivity.Board board : ui.boards) board.invalidate();
  }

  void finish() {
    handler.removeCallbacksAndMessages(null);
    active = false;
    consuming = false;
    groupDragging = false;
    selected.clear();
    if (bar != null) ui.screen.removeView(bar);
    bar = null;
    update();
  }

  boolean handle(MotionEvent event) {
    if (!active || groupDragging) return false;
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      downX = event.getRawX();
      downY = event.getRawY();
      pressed = null;
      Rect bounds = new Rect();
      if (bar != null
          && bar.getGlobalVisibleRect(bounds)
          && bounds.contains((int) downX, (int) downY)) return false;
      HomeActivity.Board board = a.board;
      if (board == null
          || !board.getGlobalVisibleRect(bounds)
          || !bounds.contains((int) downX, (int) downY)) return false;
      consuming = true;
      moved = false;
      for (int n = board.getChildCount() - 1; n >= 0; n--) {
        View child = board.getChildAt(n);
        if (child.getGlobalVisibleRect(bounds) && bounds.contains((int) downX, (int) downY)) {
          pressed = board.visible.get(n);
          origin = child;
          break;
        }
      }
      if (pressed != null && selected.contains(pressed.optString("id")))
        handler.postDelayed(hold, ViewConfiguration.getLongPressTimeout());
      return true;
    }
    if (!consuming) return false;
    if (event.getPointerCount() > 1
        || Math.abs(event.getRawX() - downX) > ui.d(12)
        || Math.abs(event.getRawY() - downY) > ui.d(12)) {
      moved = true;
      handler.removeCallbacks(hold);
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      handler.removeCallbacks(hold);
      consuming = false;
      if (action == MotionEvent.ACTION_UP && !moved && pressed != null) {
        String id = pressed.optString("id");
        if (!selected.remove(id)) selected.add(id);
        update();
      }
    }
    return true;
  }

  boolean drop(int page, int x, int y) {
    if (ui.dragging == null) return false;
    try {
      a.items =
          WorkspaceModel.moveGroup(
              a.items, selected, ui.dragging.getString("id"), page, x, y, a.cols(), ui.rows());
      ui.dropChanged = true;
      a.save();
      ui.refresh();
      update();
      return true;
    } catch (JSONException invalid) {
      a.toast(invalid.getMessage());
      return false;
    }
  }

  void removeSelection() {
    if (selected.isEmpty()) return;
    JSONArray kept = new JSONArray();
    for (int n = 0; n < a.items.length(); n++) {
      JSONObject item = a.items.optJSONObject(n);
      if (item == null) continue;
      if (!selected.contains(item.optString("id"))) kept.put(item);
      else if (item.optString("type").equals("widget"))
        a.host.deleteAppWidgetId(item.optInt("widgetId"));
    }
    a.items = kept;
    selected.clear();
    ui.dropChanged = true;
    a.save();
    ui.refresh();
    update();
  }

  void choosePage() {
    if (selected.isEmpty()) {
      a.toast("Select at least one item");
      return;
    }
    String[] pages = new String[Math.min(64, ui.pages() + 1)];
    for (int n = 0; n < pages.length; n++)
      pages[n] = n < ui.pages() ? "Page " + (n + 1) : "New page";
    a.menu(
        "Move selected items",
        pages,
        page -> {
          String id = selected.iterator().next();
          JSONObject anchor = a.items.optJSONObject(a.index(id));
          if (anchor == null) return;
          try {
            a.items =
                WorkspaceModel.moveGroup(
                    a.items,
                    selected,
                    id,
                    page,
                    anchor.getInt("x"),
                    anchor.getInt("y"),
                    a.cols(),
                    ui.rows());
            HomeActivity.put(a.settings, "pages", Math.max(ui.pages(), page + 1));
            a.save();
            ui.refresh();
            ui.goPage(page, true);
            update();
          } catch (JSONException invalid) {
            a.toast(invalid.getMessage());
          }
        });
  }
}
