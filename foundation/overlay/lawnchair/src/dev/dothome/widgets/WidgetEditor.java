package dev.dothome.widgets;
import android.content.*;
import com.android.launcher3.model.data.LauncherAppWidgetInfo;
/** Configuration is tied to the native, durable item id. */
public final class WidgetEditor {
  public static boolean open(Context context, LauncherAppWidgetInfo info) {
    try {
      String name=info.providerName.getClassName();
      if(!name.startsWith("#custom-widget-dev.dothome.widgets.DotTiles$"))return false;
      DotTile tile=(DotTile)Class.forName(name.substring("#custom-widget-".length())).getConstructor(Context.class).newInstance(context);
      context.startActivity(new Intent(context,TileConfiguration.class).putExtra("itemId",(long)info.id).putExtra("initial",TileContext.load(context,info.id,tile.getKind(),tile.getFace()).toString()));
      return true;
    }catch(Exception unavailable){return false;}
  }
}
