package dev.dothome.widgets;
import android.view.*;
/** Reuse real Home renderers in a settings preview, including newly selected material. */
public final class TileRefresh {
  public static void sync(View view) {
    if(view instanceof TileArtwork art) art.a.sync();
    if(view instanceof ViewGroup group) for(int n=0;n<group.getChildCount();n++) sync(group.getChildAt(n));
  }
}
