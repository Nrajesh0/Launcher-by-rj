package dev.dothome.test;
import android.app.*;
import android.os.*;
import org.json.*;
/** Synthetic fixture only, written inside the old APK's own sandbox before an update. */
public final class LegacySeed extends Instrumentation {
  @Override public void onCreate(Bundle args){super.onCreate(args);start();}
  @Override public void onStart(){Bundle result=new Bundle();try {
    JSONArray items=new JSONArray();
    items.put(new JSONObject().put("id","upgrade-clock").put("type","clock").put("page",0).put("x",0).put("y",0).put("w",2).put("h",2).put("style",3));
    items.put(new JSONObject().put("id","upgrade-note").put("type","notes").put("page",0).put("x",2).put("y",0).put("w",2).put("h",2).put("text","Synthetic upgrade note"));
    items.put(new JSONObject().put("id","upgrade-controls").put("type","controls").put("page",0).put("x",0).put("y",3).put("w",2).put("h",2));
    JSONObject data=new JSONObject().put("schema",2).put("settings",new JSONObject().put("columns",4).put("rows",7).put("pages",2).put("dark",true)).put("items",items).put("dock",new JSONArray());
    java.nio.file.Files.writeString(new java.io.File(getTargetContext().getFilesDir(),"workspace-v1.json").toPath(),data.toString());
    result.putString("stream","PASS: synthetic v3 layout seeded\n");finish(Activity.RESULT_OK,result);
  }catch(Exception error){result.putString("stream","FAIL: "+error);finish(Activity.RESULT_CANCELED,result);}}
}
