package dev.dothome.ui;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.view.*;
import android.widget.*;
import com.android.launcher3.*;
import com.android.launcher3.model.ModelDbController;
import java.util.*;

/** One-step guarded layout undo. A later layout edit invalidates the saved operation. */
public final class DotUndo {
  private static List<ContentValues> before;
  private static String expected;
  private static View notice;
  public static List<ContentValues> capture(ModelDbController db){List<ContentValues> rows=new ArrayList<>();try(var cursor=db.getDb().query("favorites",null,null,null,null,null,"_id")){while(cursor.moveToNext()){if(rows.size()>=1000)return null;ContentValues row=new ContentValues();DatabaseUtils.cursorRowToContentValues(cursor,row);rows.add(row);}}return rows;}
  private static String signature(List<ContentValues> rows){if(rows==null)return "";try{var digest=java.security.MessageDigest.getInstance("SHA-256");for(ContentValues row:rows){List<String> keys=new ArrayList<>(row.keySet());Collections.sort(keys);for(String key:keys){digest.update(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));Object value=row.get(key);digest.update(value instanceof byte[] bytes?bytes:String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);}}return android.util.Base64.encodeToString(digest.digest(),android.util.Base64.NO_WRAP);}catch(Exception unavailable){return "";}}
  public static void remember(List<ContentValues> snapshot,ModelDbController db){if(snapshot==null)return;before=snapshot;expected=signature(capture(db));}
  public static void show(Launcher launcher){
    if(before==null)return;if(notice!=null&&notice.getParent() instanceof ViewGroup parent)parent.removeView(notice);
    LinearLayout bar=new LinearLayout(launcher);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(DotUi.dp(launcher,18),0,DotUi.dp(launcher,8),0);bar.setBackground(DotUi.shape(launcher,DotAppearance.get(launcher).card,28));bar.addView(DotUi.text(launcher,"Home layout updated",13),new LinearLayout.LayoutParams(0,-1,1));bar.addView(DotUi.button(launcher,"Undo",()->undo(launcher)));
    var params=new com.android.launcher3.views.BaseDragLayer.LayoutParams(-1,DotUi.dp(launcher,54));params.gravity=Gravity.BOTTOM;params.leftMargin=DotUi.dp(launcher,18);params.rightMargin=DotUi.dp(launcher,18);params.bottomMargin=launcher.getDeviceProfile().getInsets().bottom+DotUi.dp(launcher,88);launcher.getDragLayer().addView(bar,params);notice=bar;DotUi.enter(bar);bar.postDelayed(()->{if(bar.getParent() instanceof ViewGroup parent)parent.removeView(bar);if(notice==bar)notice=null;},7000);
  }
  private static void undo(Launcher launcher){
    var app=LauncherAppState.getInstance(launcher);app.getModel().enqueueModelUpdateTask((task,model,apps)->{
      try{var db=app.getModel().getModelDbController();if(before==null||expected.isEmpty()||!signature(capture(db)).equals(expected)){launcher.runOnUiThread(()->Toast.makeText(launcher,"Home changed again; previous layout kept",Toast.LENGTH_SHORT).show());return;}
        List<ContentValues> restore=before;try(var tx=db.newTransaction()){db.delete("favorites",null,null);for(ContentValues row:restore)if(db.insert("favorites",row)<0)throw new IllegalStateException();tx.commit();}before=null;expected=null;app.getModel().forceReload();launcher.runOnUiThread(()->{if(notice!=null&&notice.getParent() instanceof ViewGroup parent)parent.removeView(notice);notice=null;});
      }catch(Exception unavailable){launcher.runOnUiThread(()->Toast.makeText(launcher,"Undo unavailable; current Home kept",Toast.LENGTH_SHORT).show());}
    });
  }
}
