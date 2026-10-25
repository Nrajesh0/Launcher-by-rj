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
}
