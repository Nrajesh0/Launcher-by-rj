package dev.dothome.test;
import android.app.*;
import android.os.*;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
/** Installs over the actual released v4 APK with its native DB and widget identities. */
public class FoundationV4Upgrade extends FoundationSmoke {
  JSONObject nativeSnapshot()throws Exception{
    SQLiteDatabase db=(SQLiteDatabase)invoke(invoke(invoke(home,"getModel"),"getModelDbController"),"getDb");JSONArray rows=new JSONArray();
    String[] columns={"_id","itemType","container","screen","cellX","cellY","spanX","spanY","rank","title","intent","appWidgetId","appWidgetProvider"};
    try(var cursor=db.query("favorites",columns,null,null,null,null,"_id")){while(cursor.moveToNext()){JSONObject row=new JSONObject();for(int n=0;n<columns.length;n++)row.put(columns[n],cursor.isNull(n)?JSONObject.NULL:cursor.getType(n)==android.database.Cursor.FIELD_TYPE_INTEGER?cursor.getLong(n):cursor.getString(n));rows.put(row);}}
    return new JSONObject().put("rows",rows).put("tiles",new JSONObject(home.getSharedPreferences("dot-tiles",0).getAll())).put("folders",new JSONObject(home.getSharedPreferences("dot-folders",0).getAll()));
  }
  java.io.File snapshotFile(){return new java.io.File(home.getFilesDir(),"synthetic-v4-upgrade.json");}
  @Override public void onStart(){Bundle result=new Bundle();try{
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);launchHomeAsync();SystemClock.sleep(1200);if(!home.getSystemService(android.app.role.RoleManager.class).isRoleHeld(android.app.role.RoleManager.ROLE_HOME))throw new AssertionError("v4 Home role was not retained by the update");homeKey();SystemClock.sleep(1000);
    JSONObject before=new JSONObject(java.nio.file.Files.readString(snapshotFile().toPath())),after=nativeSnapshot();
    if(!before.getJSONArray("rows").toString().equals(after.getJSONArray("rows").toString()))throw new AssertionError("v4 native item positions/identities changed during upgrade");
    for(String name:new String[]{"tiles","folders"}){JSONObject values=before.getJSONObject(name);for(var keys=values.keys();keys.hasNext();){String key=keys.next();if(!values.get(key).equals(after.getJSONObject(name).get(key)))throw new AssertionError("v4 "+name+" state changed: "+key);}}
    if(!home.getSharedPreferences("dot-style",0).getBoolean("dark",false)||!"translucent".equals(home.getSharedPreferences("dot-style",0).getString("material","")))throw new AssertionError("v4 appearance choice changed");
    screenshot("v5-upgrade-from-v4");snapshotFile().delete();result.putString("stream","PASS: v4-to-v5 actual APK install update preserves native app/folder/widget IDs, placements, item state and chosen appearance.\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
