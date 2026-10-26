package dev.dothome.widgets;
import android.view.*;
/** Reuse real Home renderers in a settings preview, including newly selected material. */
public final class TileRefresh {
  public static void onUnlock(android.content.Context context) {
    var preferences=context.getSharedPreferences("dot-tiles",0);var edit=preferences.edit();boolean changed=false;
    for(var entry:preferences.getAll().entrySet())if(entry.getValue() instanceof String value)try {
      org.json.JSONObject item=new org.json.JSONObject(value);
      if(item.optString("type").equals("photo")&&item.optBoolean("rotateOnUnlock")&&PhotoCollection.cycle(item,1)){edit.putString(entry.getKey(),item.toString());changed=true;}
    }catch(Exception ignored){}
    if(changed)edit.apply();
  }
  public interface Active { void setActive(boolean active); }
  public static void setActive(View view,boolean active) {
    if(view instanceof Active tile) tile.setActive(active);
    if(view instanceof ViewGroup group) for(int n=0;n<group.getChildCount();n++) setActive(group.getChildAt(n),active);
  }
  public static void sync(View view) {
    if(view instanceof TileArtwork art) art.a.sync();
    if(view instanceof ViewGroup group) for(int n=0;n<group.getChildCount();n++) sync(group.getChildAt(n));
  }
}
