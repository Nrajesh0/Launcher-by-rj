package dev.dothome.migration;

import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.LauncherApps;
import android.os.*;
import android.widget.Toast;
import com.android.launcher3.*;
import com.android.launcher3.model.*;
import com.android.launcher3.model.data.*;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.widget.custom.CustomWidgetManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** Convert the previous durable workspace once, as one native database transaction. */
public final class DotMigration {
  private static final AtomicBoolean running = new AtomicBoolean();
  private static final String MARKER = "legacyImported";
  private static final Map<String,String> TILES = new HashMap<>();
  static {
    String[][] entries={{"media","Media"},{"clock","Clock"},{"date","Date"},{"battery","Battery"},{"breathing","Focus"},{"away","Away"},{"photo","Photo"},{"notes","Notes"},{"agenda","Agenda"},{"countdown","Countdown"},{"timer","Timer"},{"usage","Usage"},{"compass","Compass"},{"torch","Torch"},{"bluetooth","Bluetooth"},{"sound","Sound"},{"dnd","Dnd"}};
    for(String[] entry:entries) TILES.put(entry[0],entry[1]);
  }

  public static void start(Launcher launcher) {
    var prefs=launcher.getSharedPreferences("dot-style",0);
    if(prefs.getBoolean(MARKER,false)||!running.compareAndSet(false,true)) return;
    LauncherAppState app=LauncherAppState.getInstance(launcher);
    app.getModel().enqueueModelUpdateTask((task,model,apps)->{
      try {
        ModelDbController db=app.getModel().getModelDbController();
        db.getDb().execSQL("CREATE TABLE IF NOT EXISTS dot_migration (name TEXT PRIMARY KEY)");
        try(var cursor=db.getDb().rawQuery("SELECT name FROM dot_migration WHERE name=?",new String[]{MARKER})) {
          if(cursor.moveToFirst()) { prefs.edit().putBoolean(MARKER,true).commit();return; }
        }
        File file=new File(launcher.getFilesDir(),"workspace-v1.json");
        JSONObject data=file.isFile()?new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8)):fresh(launcher);
        LayoutValidator.validate(data);
        JSONObject settings=data.getJSONObject("settings");
        int columns=settings.optInt("columns",4), rows=settings.optInt("rows",7);
        JSONArray source=WorkspaceModel.normalise(data.getJSONArray("items"),columns,rows);
        boolean sample=knownUnconfiguredSample(source);
        if(sample) source=WorkspaceModel.normalise(fresh(launcher).getJSONArray("items"),columns,rows);
        ArrayList<ItemInfo> converted=new ArrayList<>();
        var tileState=launcher.getSharedPreferences("dot-tiles",0).edit();
        for(int n=0;n<source.length();n++) {
          JSONObject old=source.getJSONObject(n);
          if(old.optString("type").equals("weather")) continue;
          ItemInfo info=convert(launcher,db,old,tileState);
          info.container=LauncherSettings.Favorites.CONTAINER_DESKTOP;
          info.screenId=old.optInt("page"); info.cellX=old.getInt("x");info.cellY=old.getInt("y");
          info.spanX=old.optInt("w",1);info.spanY=old.optInt("h",1);
          converted.add(info);
          if(info instanceof FolderInfo) {
            JSONArray children=old.optJSONArray("children");
            if(children!=null) for(int child=0;child<children.length();child++) {
              WorkspaceItemInfo entry=app(launcher,children.optString(child),"App");
              entry.id=db.generateNewItemId();entry.container=info.id;entry.screenId=0;entry.rank=child;entry.cellX=child%3;entry.cellY=child/3;
              converted.add(entry);
            }
          }
        }
        JSONArray dock=data.optJSONArray("dock");
        if(dock!=null) for(int slot=0;slot<Math.min(4,dock.length());slot++) {
          String key=dock.optString(slot);if(key.isEmpty())continue;
          WorkspaceItemInfo entry=app(launcher,key,"App");entry.id=db.generateNewItemId();entry.container=LauncherSettings.Favorites.CONTAINER_HOTSEAT;entry.screenId=slot;entry.rank=slot;entry.cellX=slot;entry.cellY=0;
          converted.add(entry);
        }
        // Construct every row before deleting anything. A failure rolls back the
        // whole DB operation; the old JSON and pre-foundation snapshot remain.
        ArrayList<ContentValues> values=new ArrayList<>();
        for(ItemInfo info:converted) {
          ContentWriter writer=new ContentWriter(launcher);info.onAddToDatabase(writer);
          ContentValues row=writer.getValues(launcher);row.put(LauncherSettings.Favorites._ID,info.id);values.add(row);
        }
        if(!tileState.commit()) throw new IllegalStateException("Widget state could not be saved");
        try(var transaction=db.newTransaction()) {
          db.delete(LauncherSettings.Favorites.TABLE_NAME,null,null);
          for(ContentValues row:values) if(db.insert(LauncherSettings.Favorites.TABLE_NAME,row)<0) throw new IllegalStateException("Workspace insert failed");
          db.getDb().execSQL("INSERT INTO dot_migration(name) VALUES (?)",new Object[]{MARKER});
          transaction.commit();
        }
        prefs.edit().putBoolean(MARKER,true).commit();
        app.getModel().forceReload();
      } catch(Exception failure) {
        android.util.Log.e("DotMigration","Previous layout kept for recovery",failure);
        launcher.runOnUiThread(()->Toast.makeText(launcher,"Previous layout is safe; import could not finish",Toast.LENGTH_LONG).show());
      } finally { running.set(false); }
    });
  }

  private static ItemInfo convert(Context context,ModelDbController db,JSONObject old,SharedPreferences.Editor tileState) throws Exception {
    String type=old.getString("type");ItemInfo info;
    if(type.equals("app")) info=app(context,old.optString("key"),old.optString("label","App"));
    else if(type.equals("folder")) { FolderInfo folder=new FolderInfo();folder.title=old.optString("name","Folder");info=folder; }
    else if(type.equals("widget")) {
      int id=old.optInt("widgetId",-1);var provider=AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
      ComponentName component=provider!=null?provider.provider:ComponentName.unflattenFromString(old.optString("provider"));
      if(component==null) throw new IllegalArgumentException("Widget provider is missing");
      LauncherAppWidgetInfo widget=new LauncherAppWidgetInfo(id,component);
      if(provider==null) widget.restoreStatus=LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY|LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
      info=widget;
    } else {
      String name=TILES.get(type);
      if(type.equals("clock")) name=new String[]{"DotClock","Clock","SerifClock","DialClock","DigitalClock"}[Math.max(0,Math.min(4,old.optInt("style")))];
      if(type.equals("photo") && old.optInt("style")==2) name="Sticker";
      if(name==null) throw new IllegalArgumentException("Unsupported saved widget: "+type);
      ComponentName component=new ComponentName(context.getPackageName(),"#custom-widget-dev.dothome.widgets.DotTiles$"+name);
      CustomWidgetManager manager=CustomWidgetManager.INSTANCE.get(context);
      if(manager.getWidgetProvider(component)==null) throw new IllegalStateException("Local widget is unavailable: "+name);
      info=new LauncherAppWidgetInfo(manager.allocateCustomAppWidgetId(component),component);
    }
    info.id=db.generateNewItemId();
    if(info instanceof LauncherAppWidgetInfo && !type.equals("widget")) tileState.putString("item-"+info.id,old.toString());
    return info;
  }

  private static WorkspaceItemInfo app(Context context,String key,String label) throws Exception {
    int at=key.lastIndexOf('@');String flat=at>=0?key.substring(0,at):key;
    UserHandle user=at>=0?context.getSystemService(UserManager.class).getUserForSerialNumber(Long.parseLong(key.substring(at+1))):android.os.Process.myUserHandle();
    ComponentName component=ComponentName.unflattenFromString(flat);
    WorkspaceItemInfo info=new WorkspaceItemInfo();info.itemType=LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;info.title=label;info.user=user==null?android.os.Process.myUserHandle():user;
    boolean available=false;
    if(component!=null && user!=null) for(var candidate:context.getSystemService(LauncherApps.class).getActivityList(component.getPackageName(),user)) if(candidate.getComponentName().equals(component)) { available=true;info.title=candidate.getLabel();break; }
    if(available) info.intent=Intent.makeMainActivity(component);
    else { info.itemType=1; info.intent=new Intent(context,UnavailableApp.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).putExtra("label",label).putExtra("original",key);info.user=android.os.Process.myUserHandle(); }
    return info;
  }

  private static boolean knownUnconfiguredSample(JSONArray source) {
    if(source.length()!=9)return false;
    String[] types={"clock","photo","bluetooth","dnd","timer","photo","torch","battery","breathing"};
    int[][] cells={{0,0,2,2},{2,0,2,2},{0,2,1,1},{1,2,1,1},{2,4,2,1},{0,5,1,1},{1,5,1,1},{0,6,2,1},{2,5,2,2}};
    for(int n=0;n<9;n++) {
      JSONObject item=source.optJSONObject(n);
      if(item==null||!item.optString("type").equals(types[n])||!item.optString("uri").isEmpty()||item.optInt("page")!=0||item.optInt("x")!=cells[n][0]||item.optInt("y")!=cells[n][1]||item.optInt("w",1)!=cells[n][2]||item.optInt("h",1)!=cells[n][3]||item.optInt("style")!=(n==0?1:n==1?2:n==7?2:0))return false;
    }
    return true;
  }

  private static JSONObject fresh(Context context) throws Exception {
    JSONObject data=new JSONObject().put("schema",2).put("settings",new JSONObject().put("columns",4).put("rows",7).put("pages",2));
    JSONArray items=new JSONArray();String[] types={"clock","date","bluetooth","dnd","battery","breathing","sound","torch"};
    int[][] cells={{0,0,2,2},{2,0,2,2},{0,2,1,1},{1,2,1,1},{0,6,2,1},{2,5,2,2},{0,5,1,1},{1,5,1,1}};
    for(int n=0;n<types.length;n++)items.put(new JSONObject().put("id","default-"+n).put("type",types[n]).put("page",0).put("x",cells[n][0]).put("y",cells[n][1]).put("w",cells[n][2]).put("h",cells[n][3]).put("style",n==0?1:n==1?2:0));
    JSONArray dock=new JSONArray();UserManager users=context.getSystemService(UserManager.class);long serial=users.getSerialNumberForUser(android.os.Process.myUserHandle());
    Intent[] intents={new Intent(Intent.ACTION_DIAL),new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)};
    for(Intent intent:intents) {
      String key="";var resolved=context.getPackageManager().resolveActivity(intent,0);
      if(resolved!=null)for(var candidate:context.getSystemService(LauncherApps.class).getActivityList(resolved.activityInfo.packageName,android.os.Process.myUserHandle())) {key=candidate.getComponentName().flattenToString()+"@"+serial;break;}
      dock.put(key);
    }
    return data.put("items",items).put("dock",dock);
  }
}
