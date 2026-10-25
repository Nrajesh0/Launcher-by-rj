package dev.dothome;

import org.json.*;

/** Removes discontinued online tiles when opening a legacy layout or backup. */
public final class OfflineMigration {
  public static void apply(JSONObject data) throws JSONException {
    JSONArray original = data.getJSONArray("items");
    JSONArray offline = new JSONArray();
    for (int i = 0; i < original.length(); i++) {
      JSONObject item = original.getJSONObject(i);
      if (!"weather".equals(item.optString("type"))) offline.put(item);
    }
    data.put("items", offline);
  }
}
