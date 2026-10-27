package dev.dothome.widgets;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;
import org.json.JSONObject;

/** Tray variants use the same renderer as the placed widget, without allocating host IDs. */
public final class TilePreviews {
  public static Bitmap render(Context context,LauncherAppWidgetProviderInfo info,int maxWidth,int[] original) {
    String name=info.provider.getClassName();
    if(!name.startsWith("#custom-widget-dev.dothome.widgets.DotTiles$")) return null;
    try {
      DotTile tile=(DotTile)Class.forName(name.substring("#custom-widget-".length())).getConstructor(Context.class).newInstance(context);
      int width=Math.max(1,Math.min(maxWidth,Math.round(220*context.getResources().getDisplayMetrics().density)));
      int height=Math.max(1,width*tile.getRows()/tile.getColumns());
      if(original!=null) original[0]=width;
      JSONObject sample=new JSONObject().put("type",tile.getKind()).put("style",tile.getFace()).put("w",tile.getColumns()).put("h",tile.getRows());
      if(tile.getKind().equals("weather"))sample.put("temperature","21").put("location","Local weather");
      TileArtwork view=new TileArtwork(new TileContext(context),sample,true);
      view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);
      Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));return bitmap;
    } catch(ReflectiveOperationException|org.json.JSONException error) { throw new IllegalStateException("Local widget preview is unavailable",error); }
  }
}
