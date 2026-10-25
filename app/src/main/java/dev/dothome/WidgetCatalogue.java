package dev.dothome;

import java.util.Arrays;

/** Capabilities shared by gallery/configuration; no styles or sizes that a renderer cannot use. */
final class WidgetCatalogue {
  static String[] styles(String type) {
    switch (type) {
      case "clock":
        return new String[] {"DOT MATRIX", "ANALOGUE", "SERIF", "TICK DIAL", "ROUNDED DIGITAL"};
      case "photo":
        return new String[] {"RECTANGLE", "CIRCLE", "STICKER"};
      case "battery":
        return new String[] {"RING", "PERCENTAGE", "DOT MATRIX"};
      case "date":
        return new String[] {"CLASSIC", "INVERTED", "SERIF"};
      default:
        return new String[] {"CLASSIC", "INVERTED"};
    }
  }

  static int[][] sizes(String type) {
    if (Arrays.asList("torch", "bluetooth", "sound", "dnd").contains(type))
      return new int[][] {{1, 1}};
    if (Arrays.asList("clock", "media", "breathing", "away").contains(type))
      return new int[][] {{2, 2}, {4, 2}};
    if (type.equals("photo")) return new int[][] {{1, 1}, {2, 2}, {4, 2}};
    if (type.equals("agenda") || type.equals("notes")) return new int[][] {{2, 2}, {4, 2}};
    return new int[][] {{2, 1}, {2, 2}};
  }
}
