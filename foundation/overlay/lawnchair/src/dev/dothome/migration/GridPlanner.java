package dev.dothome.migration;

import java.util.*;

/** Deterministic first-fit placement shared by the workspace and its invariant tests. */
public final class GridPlanner {
  public static List<int[]> place(List<int[]> sizes, int columns) {
    if (columns < 1 || columns > 12) throw new IllegalArgumentException("columns");
    boolean[][] used = new boolean[1024][columns];
    List<int[]> result = new ArrayList<>();
    for (int[] s : sizes) {
      int w = Math.max(1, Math.min(columns, s[0])), h = Math.max(1, Math.min(6, s[1]));
      boolean placed = false;
      outer:
      for (int y = 0; y <= 1024 - h; y++)
        for (int x = 0; x <= columns - w; x++) {
          boolean ok = true;
          for (int dy = 0; dy < h; dy++)
            for (int dx = 0; dx < w; dx++) if (used[y + dy][x + dx]) ok = false;
          if (ok) {
            for (int dy = 0; dy < h; dy++)
              for (int dx = 0; dx < w; dx++) used[y + dy][x + dx] = true;
            result.add(new int[] {x, y, w, h});
            placed = true;
            break outer;
          }
        }
      if (!placed) throw new IllegalArgumentException("Layout too large");
    }
    return result;
  }
}
