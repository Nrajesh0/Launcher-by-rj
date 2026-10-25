package dev.dothome;

import java.util.*;
import org.json.*;

/**
 * Explicit, stable workspace coordinates. Legacy first-fit is used only for migration/new items.
 */
public final class WorkspaceModel {
  public static final int MAX_PAGES = 64;

  public static boolean fits(
      JSONArray items, String ignore, int page, int x, int y, int w, int h, int columns, int rows) {
    if (page < 0
        || page >= MAX_PAGES
        || x < 0
        || y < 0
        || w < 1
        || h < 1
        || x + w > columns
        || y + h > rows) return false;
    for (int i = 0; i < items.length(); i++) {
      JSONObject a = items.optJSONObject(i);
      if (a == null || a.optString("id").equals(ignore) || a.optInt("page") != page || !a.has("x"))
        continue;
      if (x < a.optInt("x") + a.optInt("w", 1)
          && x + w > a.optInt("x")
          && y < a.optInt("y") + a.optInt("h", 1)
          && y + h > a.optInt("y")) return false;
    }
    return true;
  }

  public static int[] vacancy(
      JSONArray items, String ignore, int preferredPage, int w, int h, int columns, int rows) {
    for (int page = Math.max(0, preferredPage); page < MAX_PAGES; page++)
      for (int y = 0; y <= rows - h; y++)
        for (int x = 0; x <= columns - w; x++)
          if (fits(items, ignore, page, x, y, w, h, columns, rows)) return new int[] {page, x, y};
    return null;
  }

  public static JSONArray normalise(JSONArray original, int columns, int rows)
      throws JSONException {
    JSONArray result = new JSONArray();
    for (int i = 0; i < original.length(); i++) {
      JSONObject a = new JSONObject(original.getJSONObject(i).toString());
      int w = Math.min(columns, Math.max(1, a.optInt("w", 1))),
          h = Math.min(rows, Math.max(1, a.optInt("h", 1)));
      int page = a.optInt("page"), x = a.optInt("x", -1), y = a.optInt("y", -1);
      if (!fits(result, null, page, x, y, w, h, columns, rows)) {
        int[] p = vacancy(result, null, page, w, h, columns, rows);
        if (p == null)
          throw new JSONException("Workspace is full; previous layout has been preserved");
        page = p[0];
        x = p[1];
        y = p[2];
      }
      a.put("page", page);
      a.put("x", x);
      a.put("y", y);
      a.put("w", w);
      a.put("h", h);
      result.put(a);
    }
    return result;
  }

  public static void move(
      JSONArray items, JSONObject a, int page, int x, int y, int columns, int rows)
      throws JSONException {
    if (!fits(
        items, a.optString("id"), page, x, y, a.optInt("w", 1), a.optInt("h", 1), columns, rows))
      throw new JSONException("Occupied position");
    a.put("page", page);
    a.put("x", x);
    a.put("y", y);
  }

  /** Atomic group translation. Original JSON is never changed on success or failure. */
  public static JSONArray moveGroup(
      JSONArray original,
      Set<String> selected,
      String anchorId,
      int page,
      int x,
      int y,
      int columns,
      int rows)
      throws JSONException {
    if (selected == null || selected.isEmpty() || !selected.contains(anchorId))
      throw new JSONException("Select an anchor item");
    JSONArray stationary = new JSONArray();
    ArrayList<JSONObject> moving = new ArrayList<>();
    JSONObject anchor = null;
    HashSet<String> found = new HashSet<>();
    for (int n = 0; n < original.length(); n++) {
      JSONObject item = new JSONObject(original.getJSONObject(n).toString());
      String id = item.getString("id");
      if (selected.contains(id)) {
        if (!found.add(id)) throw new JSONException("Duplicate item identifier");
        moving.add(item);
        if (id.equals(anchorId)) anchor = item;
      } else stationary.put(item);
    }
    if (anchor == null || !found.equals(selected)) throw new JSONException("Selection changed");
    int dx = x - anchor.getInt("x"), dy = y - anchor.getInt("y"), dp = page - anchor.getInt("page");
    for (JSONObject item : moving) {
      int px = item.getInt("x") + dx, py = item.getInt("y") + dy, pp = item.getInt("page") + dp;
      if (!fits(
          stationary, null, pp, px, py, item.optInt("w", 1), item.optInt("h", 1), columns, rows))
        throw new JSONException("The selected group does not fit here");
      item.put("x", px).put("y", py).put("page", pp);
      stationary.put(item);
    }
    // Preserve original draw order and stable item identity.
    JSONArray result = new JSONArray();
    for (int n = 0; n < original.length(); n++) {
      String id = original.getJSONObject(n).getString("id");
      for (int at = 0; at < stationary.length(); at++)
        if (stationary.getJSONObject(at).getString("id").equals(id)) {
          result.put(stationary.getJSONObject(at));
          break;
        }
    }
    return result;
  }
}
