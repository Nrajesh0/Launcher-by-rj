package dev.dothome.ui;
import android.content.Context;
import android.graphics.Color;
/** Shared launcher-owned surface and ink colours for tiles and dock search. */
public final class DotSurface {
  public static int ink(Context context) {return DotAppearance.get(context).ink;}
  public static int paper(Context context) {
    return DotAppearance.get(context).surface;
  }
}
