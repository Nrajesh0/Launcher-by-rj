package dev.dothome;

import java.util.*;
import org.json.*;

public final class LayoutValidator {
  public static void validate(JSONObject x) throws Exception {
    if (x.getInt("schema") != 1) throw new Exception("Unsupported backup version");
    JSONArray list = x.getJSONArray("items");
    if (list.length() > 150) throw new Exception("Too many items");
    JSONObject s = x.getJSONObject("settings");
    if (s.optInt("columns", 4) < 4 || s.optInt("columns", 4) > 5)
      throw new Exception("Invalid grid");
    if (s.optInt("iconSize", 46) < 24 || s.optInt("iconSize", 46) > 96)
      throw new Exception("Invalid icon size");
    HashSet<String> ids = new HashSet<>();
    Set<String> types =
        new HashSet<>(
            Arrays.asList(
                "app",
                "folder",
                "widget",
                "clock",
                "date",
                "battery",
                "weather",
                "media",
                "agenda",
                "photo",
                "notes",
                "countdown",
                "compass",
                "usage",
                "controls"));
    for (int i = 0; i < list.length(); i++) {
      JSONObject item = list.getJSONObject(i);
      if (!types.contains(item.getString("type"))
          || !ids.add(item.getString("id"))
          || item.optInt("page") < 0
          || item.optInt("page") > 2
          || item.optInt("w", 1) < 1
          || item.optInt("w", 1) > 5
          || item.optInt("h", 1) < 1
          || item.optInt("h", 1) > 6) throw new Exception("Invalid layout item");
    }
    JSONArray d = x.optJSONArray("dock");
    if (d != null && d.length() > 4) throw new Exception("Invalid dock");
  }
}
