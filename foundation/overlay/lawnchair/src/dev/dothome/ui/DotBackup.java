package dev.dothome.ui;

import android.app.*;
import android.content.*;
import android.net.Uri;
import com.android.launcher3.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** User-selected local documents, a validated layout, staged images and recoverable settings. */
public final class DotBackup {
  private static final Set<String> PREFS=Set.of("dot-style","dot-tiles","dot-folders","dot-categories");
  public static void export(Activity activity,Uri uri) {
    var state=LauncherAppState.getInstance(activity);
    state.getModel().enqueueModelUpdateTask((task,model,apps)->{
      try {
        JSONObject backup=new JSONObject().put("format","dot-home-native-2").put("settings",DotSettingsBackup.export(activity));
        var grid=InvariantDeviceProfile.INSTANCE.get(activity);
        backup.put("columns",grid.numColumns).put("rows",grid.numRows);
        JSONArray rows=new JSONArray();
        try(var cursor=state.getModel().getModelDbController().getDb().query("favorites",null,null,null,null,null,"_id")) {
          while(cursor.moveToNext()) {
            JSONObject row=new JSONObject();
            for(int n=0;n<cursor.getColumnCount();n++) {
              String key=cursor.getColumnName(n);
              switch(cursor.getType(n)) {
                case android.database.Cursor.FIELD_TYPE_NULL:row.put(key,JSONObject.NULL);break;
                case android.database.Cursor.FIELD_TYPE_INTEGER:row.put(key,cursor.getLong(n));break;
                case android.database.Cursor.FIELD_TYPE_STRING:row.put(key,cursor.getString(n));break;
                case android.database.Cursor.FIELD_TYPE_BLOB:row.put(key,new JSONObject().put("blob",android.util.Base64.encodeToString(cursor.getBlob(n),android.util.Base64.NO_WRAP)));break;
                default:throw new IllegalArgumentException("Unexpected layout column");
              }
            }
            rows.put(row);
          }
        }
        backup.put("items",rows);
        JSONObject settings=new JSONObject();
        for(String name:PREFS)settings.put(name,new JSONObject(activity.getSharedPreferences(name,0).getAll()));
        backup.put("preferences",settings).put("media",DotBackupMedia.export(activity));
        try(var output=activity.getContentResolver().openOutputStream(uri,"wt")){output.write(backup.toString().getBytes(StandardCharsets.UTF_8));}
        activity.runOnUiThread(()->android.widget.Toast.makeText(activity,"Layout backup saved",android.widget.Toast.LENGTH_SHORT).show());
      }catch(Exception error){message(activity,"Backup could not be saved");}
    });
  }
  public static void inspect(Activity activity,Uri uri) {
    var launcher=app.lawnchair.LawnchairLauncher.Companion.getInstance();if(launcher!=null&&DotArrange.locked(launcher)){message(activity,"Unfreeze Home layout before restoring");return;}
    new Thread(()->{
      try {
        String input;
        try(var stream=activity.getContentResolver().openInputStream(uri)){byte[] bytes=stream.readNBytes(36*1024*1024+1);if(bytes.length>36*1024*1024)throw new IllegalArgumentException();input=new String(bytes,StandardCharsets.UTF_8);}
        JSONObject backup=new JSONObject(input);
        if(!Set.of("dot-home-native-1","dot-home-native-2").contains(backup.getString("format")))throw new IllegalArgumentException();
        var grid=InvariantDeviceProfile.INSTANCE.get(activity);
        if(backup.getInt("columns")!=grid.numColumns || backup.getInt("rows")!=grid.numRows){message(activity,"Choose the backup's "+backup.getInt("columns")+" × "+backup.getInt("rows")+" grid before restoring");return;}
        JSONArray rows=backup.getJSONArray("items");if(rows.length()>1000)throw new IllegalArgumentException();
        DotBackupMedia.validate(backup.optJSONObject("media"));
        if(backup.optJSONObject("settings")!=null)DotSettingsBackup.validate(backup.getJSONObject("settings"));
        validatePreferences(backup.getJSONObject("preferences"));
        Set<Integer> ids=new HashSet<>(),folders=new HashSet<>();List<android.graphics.Rect> cells=new ArrayList<>();List<Integer> pages=new ArrayList<>();
        for(int n=0;n<rows.length();n++) {
          JSONObject row=rows.getJSONObject(n);int type=row.getInt("itemType"),id=row.getInt("_id"),container=row.getInt("container");
          if(id<0 || !ids.add(id) || !Set.of(0,1,2,4,5,6).contains(type))throw new IllegalArgumentException();
          if(type==2)folders.add(id);
          if(container==-100) {
            int x=row.getInt("cellX"),y=row.getInt("cellY"),w=row.getInt("spanX"),h=row.getInt("spanY"),page=row.getInt("screen");
            if(x<0||y<0||w<1||h<1||x+w>grid.numColumns||y+h>grid.numRows||page<0)throw new IllegalArgumentException();
            var rect=new android.graphics.Rect(x,y,x+w,y+h);
            for(int j=0;j<cells.size();j++)if(pages.get(j)==page&&android.graphics.Rect.intersects(cells.get(j),rect))throw new IllegalArgumentException();
            cells.add(rect);pages.add(page);
          } else if(container==-101){if(row.getInt("screen")<0||row.getInt("screen")>3)throw new IllegalArgumentException();}
          else if(container<0 || !Set.of(0,1,6).contains(type))throw new IllegalArgumentException();
        }
        for(int n=0;n<rows.length();n++){int parent=rows.getJSONObject(n).getInt("container");if(parent>=0&&!folders.contains(parent))throw new IllegalArgumentException();}
        activity.runOnUiThread(()->new AlertDialog.Builder(activity).setTitle("Restore "+rows.length()+" Home items?").setMessage("This replaces your Home layout. Apps stay installed. Owned photos are restored from the backup. External photos and widgets may need reconnecting. Your current system wallpaper stays selected; a saved wallpaper image can be applied separately.").setPositiveButton("Restore",(dialog,which)->restore(activity,backup)).setNegativeButton("Cancel",null).show());
      }catch(Exception invalid){message(activity,"This layout backup is invalid; Home was kept unchanged");}
    },"dot-backup-read").start();
  }
  private static void restore(Activity activity,JSONObject backup) {
    var state=LauncherAppState.getInstance(activity);
    state.getModel().enqueueModelUpdateTask((task,model,apps)->{
      try {
        var controller=state.getModel().getModelDbController();Set<String> columns=new HashSet<>();
        try(var cursor=controller.getDb().rawQuery("PRAGMA table_info(favorites)",null)){while(cursor.moveToNext())columns.add(cursor.getString(1));}
        JSONArray rows=backup.getJSONArray("items");List<ContentValues> values=new ArrayList<>();
        for(int n=0;n<rows.length();n++) {
          JSONObject row=rows.getJSONObject(n);ContentValues value=new ContentValues();
          for(var keys=row.keys();keys.hasNext();) {
            String key=keys.next();if(!columns.contains(key))continue;Object data=row.get(key);
            if(data==JSONObject.NULL)value.putNull(key);else if(data instanceof Number)value.put(key,((Number)data).longValue());else if(data instanceof String)value.put(key,(String)data);else if(data instanceof JSONObject blob)value.put(key,android.util.Base64.decode(blob.getString("blob"),android.util.Base64.DEFAULT));else throw new IllegalArgumentException();
          }
          if(row.getInt("itemType")==4 && android.appwidget.AppWidgetManager.getInstance(activity).getAppWidgetInfo(row.optInt("appWidgetId",-1))==null)value.put("restored",1);
          values.add(value);
        }
        JSONObject preferences=new JSONObject(backup.getJSONObject("preferences").toString());
        validatePreferences(preferences);
        // WallpaperManager is an independent system service. Keep its current selection;
        // imported backup images are offered through the crop/apply confirmation instead.
        JSONObject style=preferences.optJSONObject("dot-style");
        if(style!=null){var current=activity.getSharedPreferences("dot-style",0).getAll();for(String key:Set.of("wallpaper","wallpaperApplied","externalWallpaper","ownedWallpaperId","observedWallpaperId")){style.remove(key);if(current.containsKey(key))style.put(key,current.get(key));}}
        JSONObject oldPreferences=new JSONObject();for(String name:PREFS)oldPreferences.put(name,new JSONObject(activity.getSharedPreferences(name,0).getAll()));
        JSONObject oldSettings=DotSettingsBackup.export(activity),settings=backup.optJSONObject("settings");
        if(settings!=null)DotSettingsBackup.validate(settings);
        // All input and media decode bounds are checked before changing the database.
        try(var media=DotBackupMedia.stage(activity,DotBackupMedia.validate(backup.optJSONObject("media")),preferences);var tx=controller.newTransaction()) {
          try {
            controller.delete("favorites",null,null);
            for(ContentValues value:values)if(controller.insert("favorites",value)<0)throw new IllegalStateException();
            if(settings!=null)DotSettingsBackup.restore(activity,settings);
            applyPreferences(activity,preferences);
            tx.commit();media.accept();
          }catch(Exception failure){
            try{DotSettingsBackup.restore(activity,oldSettings);applyPreferences(activity,oldPreferences);}catch(Exception rollback){failure.addSuppressed(rollback);}
            throw failure;
          }
        }
        DotAppearance.invalidate();
        boolean dark=activity.getSharedPreferences("dot-style",0).getBoolean("dark",false);
        app.lawnchair.preferences.PreferenceManager.getInstance(activity).getLauncherTheme().set(dark?"dark":"light");
        state.getModel().forceReload();activity.runOnUiThread(()->{var launcher=app.lawnchair.LawnchairLauncher.Companion.getInstance();if(launcher!=null)launcher.recreateIfNotScheduled();});message(activity,"Home layout restored");
      }catch(Exception error){DotAppearance.invalidate();state.getModel().forceReload();message(activity,"Restore failed; the previous layout was kept");}
    });
  }
  private static void validatePreferences(JSONObject source)throws Exception {
    for(String name:PREFS){JSONObject values=source.optJSONObject(name);if(values==null)continue;if(values.length()>4000)throw new IllegalArgumentException();
      for(var keys=values.keys();keys.hasNext();){String key=keys.next();Object value=values.get(key);if(name.equals("dot-style")){
          if(Set.of("dark","manualAccent","reduceMotion","wallpaperApplied","externalWallpaper","homeOnlyIcons","legacyImported","torchActive").contains(key)&&!(value instanceof Boolean))throw new IllegalArgumentException();
          if(Set.of("accent","wallpaperAccent","ownedWallpaperId","observedWallpaperId").contains(key)&&!(value instanceof Number))throw new IllegalArgumentException();
          if(Set.of("material","wallpaper","backupWallpaper").contains(key)&&!(value instanceof String))throw new IllegalArgumentException();
        }else if(!(value instanceof String))throw new IllegalArgumentException();
        if(key.length()>512||!(value instanceof String||value instanceof Boolean||value instanceof Number)||value instanceof String&&((String)value).length()>1000000)throw new IllegalArgumentException();}
    }
  }
  private static void applyPreferences(Context context,JSONObject source)throws Exception {
    for(String name:PREFS){JSONObject values=source.optJSONObject(name);if(values==null)continue;var editor=context.getSharedPreferences(name,0).edit().clear();
      for(var keys=values.keys();keys.hasNext();){String key=keys.next();Object value=values.get(key);if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Float||value instanceof Double)editor.putFloat(key,((Number)value).floatValue());else if(value instanceof Number)editor.putLong(key,((Number)value).longValue());else if(value instanceof String)editor.putString(key,(String)value);}
      if(!editor.commit())throw new java.io.IOException("Settings could not be saved");
    }
  }
  private static void message(Activity activity,String text){activity.runOnUiThread(()->android.widget.Toast.makeText(activity,text,android.widget.Toast.LENGTH_LONG).show());}
}
