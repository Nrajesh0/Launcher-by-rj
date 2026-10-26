package dev.dothome.ui;
import android.content.*;
import android.widget.Toast;
import com.android.launcher3.*;
import com.android.launcher3.model.data.*;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.widget.custom.CustomWidgetManager;
import java.util.*;
import org.json.JSONObject;

/** Presets add one native page; existing pages, the dock and appearance are retained. */
public final class DotPresets {
  public static void add(Launcher launcher,boolean lucent){
    if(DotArrange.locked(launcher)){Toast.makeText(launcher,"Home layout is frozen",Toast.LENGTH_SHORT).show();return;}
    LauncherAppState app=LauncherAppState.getInstance(launcher);app.getModel().enqueueModelUpdateTask((task,model,apps)->{
      try{
        var db=app.getModel().getModelDbController();var grid=InvariantDeviceProfile.INSTANCE.get(launcher);if(grid.numColumns<4||grid.numRows<6)throw new IllegalArgumentException();
        String[][] classic={{"Clock","clock","0","0","2","2","1"},{"Date","date","2","0","2","2","2"},{"Weather","weather","0","3","2","2","0"},{"Focus","breathing","2","3","2","2","0"},{"Torch","torch","0","2","1","1","0"},{"Sound","sound","1","2","1","1","0"},{"Bluetooth","bluetooth","2","2","1","1","0"},{"Dnd","dnd","3","2","1","1","0"},{"Battery","battery","0","5","2","1","2"},{"Usage","usage","2","5","2","1","0"}};
        String[][] translucent={{"Away","away","2","0","2","2","0"},{"Focus","breathing","0","2","2","2","0"},{"Sticker","photo","2","2","2","2","2"},{"Clock","clock","2","4","2","2","1"},{"WeatherCompact","weather","0","4","2","1","0"},{"Battery","battery","0","5","2","1","2"},{"Torch","torch","0","0","1","1","0"},{"Sound","sound","1","0","1","1","0"},{"Bluetooth","bluetooth","0","1","1","1","0"},{"Dnd","dnd","1","1","1","1","0"}};
        int screen=db.getNewScreenId();ArrayList<ContentValues> rows=new ArrayList<>();var saved=launcher.getSharedPreferences("dot-tiles",0).edit();
        for(String[] tile:lucent?translucent:classic){ComponentName component=new ComponentName(launcher.getPackageName(),"#custom-widget-dev.dothome.widgets.DotTiles$"+tile[0]);var manager=CustomWidgetManager.INSTANCE.get(launcher);if(manager.getWidgetProvider(component)==null)throw new IllegalStateException();LauncherAppWidgetInfo item=new LauncherAppWidgetInfo(manager.allocateCustomAppWidgetId(component),component);item.id=db.generateNewItemId();item.container=LauncherSettings.Favorites.CONTAINER_DESKTOP;item.screenId=screen;int x=Integer.parseInt(tile[2]);item.cellX=x>=2?x+grid.numColumns-4:x;item.cellY=Integer.parseInt(tile[3]);item.spanX=Integer.parseInt(tile[4]);item.spanY=Integer.parseInt(tile[5]);ContentWriter writer=new ContentWriter(launcher);item.onAddToDatabase(writer);ContentValues row=writer.getValues(launcher);row.put("_id",item.id);rows.add(row);saved.putString("item-"+item.id,new JSONObject().put("type",tile[1]).put("style",Integer.parseInt(tile[6])).put("w",item.spanX).put("h",item.spanY).put("page",screen).toString());}
        var snapshot=DotUndo.capture(db);try(var tx=db.newTransaction()){for(ContentValues row:rows)if(db.insert("favorites",row)<0)throw new IllegalStateException();tx.commit();}saved.apply();DotUndo.remember(snapshot,db);app.getModel().forceReload();launcher.runOnUiThread(()->{DotUndo.show(launcher);Toast.makeText(launcher,"Layout added on a new Home page",Toast.LENGTH_SHORT).show();});
      }catch(Exception unavailable){launcher.runOnUiThread(()->Toast.makeText(launcher,"Layout could not be added; existing pages kept",Toast.LENGTH_SHORT).show());}
    });
  }
}
