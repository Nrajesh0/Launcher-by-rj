package dev.dothome.test;
import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** v5 checks use synthetic local data and the actual Activity/widget renderers. */
public final class ExperienceSmoke extends FoundationSmoke {
  Class<?> cls(String name)throws Exception{return Class.forName(name,true,getTargetContext().getClassLoader());}
  Map<Integer,String> placement(){
    Map<Integer,String> result=new TreeMap<>();SQLiteDatabase db=(SQLiteDatabase)invoke(invoke(invoke(home,"getModel"),"getModelDbController"),"getDb");
    try(var cursor=db.rawQuery("SELECT _id,container,screen,cellX,cellY,spanX,spanY FROM favorites ORDER BY _id",null)){
      while(cursor.moveToNext()){StringBuilder value=new StringBuilder();for(int n=1;n<7;n++)value.append(cursor.getInt(n)).append(':');result.put(cursor.getInt(0),value.toString());}
    }return result;
  }
  void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  void settings(String route)throws Exception{
    getTargetContext().startActivity(new Intent().setClassName("dev.dothome","app.lawnchair.ui.preferences.PreferenceActivity").putExtra("dotRoute",route).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(550);
  }
  int views(View view,String name){int count=view.getClass().getName().equals(name)?1:0;if(view instanceof ViewGroup group)for(int n=0;n<group.getChildCount();n++)count+=views(group.getChildAt(n),name);return count;}
  void themes()throws Exception{
    Map<Integer,String> original=placement();
    for(String material:new String[]{"Classic","Lucent"})for(boolean dark:new boolean[]{false,true}){
      settings("appearance");visible(material);check(clickSystemText(material),"Material selection failed");
      if(home.getSharedPreferences("dot-style",0).getBoolean("dark",false)!=dark){visible("Dark theme");check(clickSystemText("Dark theme"),"Dark theme selection failed");}
      SystemClock.sleep(600);screenshot("v5-theme-"+material.toLowerCase()+"-"+(dark?"dark":"light"));homeKey();SystemClock.sleep(500);
      check(original.equals(placement()),"Theme selection changed the layout");screenshot("v5-home-"+material.toLowerCase()+"-"+(dark?"dark":"light"));
    }
    settings("grid");visible("Home screen layout");screenshot("v5-grid-choices");homeKey();check(original.equals(placement()),"Opening grid settings changed layout");
  }
  void hiddenCancellation()throws Exception{
    View workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");
    View apps=(View)invoke(home,"getAppsView");int[] position=new int[2];onUi(()->apps.getLocationOnScreen(position));
    float x=position[0]+apps.getWidth()*.22f,y=position[1]+apps.getHeight()*.45f;long down=SystemClock.uptimeMillis();
    for(int n=0;n<8;n++){MotionEvent e=MotionEvent.obtain(down,down+n*20,n==0?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_MOVE,x+n*10,y,0);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(e,true);e.recycle();}
    MotionEvent cancel=MotionEvent.obtain(down,down+180,MotionEvent.ACTION_CANCEL,x+70,y,0);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(cancel,true);cancel.recycle();SystemClock.sleep(350);
    final boolean[] open={true};onUi(()->{try{open[0]=(Boolean)cls("dev.dothome.ui.DotHiddenPanel").getMethod("isOpen",cls("app.lawnchair.LawnchairLauncher")).invoke(null,home);}catch(Exception error){throw new RuntimeException(error);}});
    check(!open[0],"Cancelled hidden-page gesture stayed open");awaitState("ALL_APPS");
    swipe(apps,.18f,.45f,200,0,false);visible("Hidden icons");shell("input keyevent KEYCODE_BACK");SystemClock.sleep(350);awaitState("ALL_APPS");homeKey();
  }
  void categories()throws Exception{
    settings("drawer");visible("Smart categories");clickSystemText("Smart categories");SystemClock.sleep(500);homeKey();
    View workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");SystemClock.sleep(800);
    final int[] cards={0};onUi(()->cards[0]=views((View)invoke(home,"getAppsView"),"dev.dothome.ui.DotCategoryCard"));check(cards[0]>0,"Category mode did not bind category cards");screenshot("v5-categories");
    Object[] apps=(Object[])invoke(invoke(invoke(home,"getAppsView"),"getAppsStore"),"getApps");Object app=apps[0];String key=invoke(app,"toComponentKey").toString();
    onUi(()->{try{cls("dev.dothome.ui.DotCategories").getMethod("choose",Context.class,cls("com.android.launcher3.model.data.ItemInfoWithIcon")).invoke(null,home,app);}catch(Exception error){throw new RuntimeException(error);}});
    visible("Communication");screenshot("v5-category-editor");clickSystemText("Communication");SystemClock.sleep(400);
    check("Communication".equals(home.getSharedPreferences("dot-categories",0).getString(key,"")),"Category correction not persisted");homeKey();
    settings("drawer");clickSystemText("Alphabetical");homeKey();
  }
  void presetUndo()throws Exception{
    Map<Integer,String> original=placement();Class<?> launcher=cls("com.android.launcher3.Launcher");
    onUi(()->{try{cls("dev.dothome.ui.DotPresets").getMethod("add",launcher,boolean.class).invoke(null,home,true);}catch(Exception error){throw new RuntimeException(error);}});
    for(int n=0;n<40&&placement().size()==original.size();n++)SystemClock.sleep(100);
    Map<Integer,String> expanded=placement();check(expanded.size()==original.size()+10,"Preset did not add exactly one ten-widget page");for(var entry:original.entrySet())check(entry.getValue().equals(expanded.get(entry.getKey())),"Preset moved an existing item");
    visible("Undo");screenshot("v5-added-preset");clickSystemText("Undo");for(int n=0;n<40&&!original.equals(placement());n++)SystemClock.sleep(100);check(original.equals(placement()),"Preset undo did not restore original rows");homeKey();
  }
  Activity editor(long id,JSONObject item)throws Exception{
    ActivityMonitor monitor=addMonitor("dev.dothome.widgets.TileConfiguration",null,false);
    getTargetContext().startActivity(new Intent().setClassName("dev.dothome","dev.dothome.widgets.TileConfiguration").putExtra("itemId",id).putExtra("initial",item.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    Activity result=waitForMonitorWithTimeout(monitor,5000);removeMonitor(monitor);check(result!=null,"Widget editor did not open");SystemClock.sleep(300);return result;
  }
  EditText draft(Activity activity,String field)throws Exception{var f=activity.getClass().getDeclaredField(field);f.setAccessible(true);return (EditText)f.get(activity);}
  Object staticCall(String name,String method,Class<?>[] types,Object... args)throws Exception{var m=cls(name).getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(null,args);}
  void widgetData()throws Exception{
    long noteId=900001,weatherId=900002;var prefs=home.getSharedPreferences("dot-tiles",0);
    Activity notes=editor(noteId,new JSONObject().put("type","notes"));onUi(()->{try{draft(notes,"draftText").setText("Synthetic multiline\nnote persists with Done");}catch(Exception error){throw new RuntimeException(error);}});clickSystemText("Done");SystemClock.sleep(250);
    check(new JSONObject(prefs.getString("item-"+noteId,"{}")).getString("text").contains("multiline\n"),"Done lost the note draft");
    Activity weather=editor(weatherId,new JSONObject().put("type","weather"));onUi(()->{try{draft(weather,"draftCity").setText("Synthetic town");draft(weather,"draftTemperature").setText("19");}catch(Exception error){throw new RuntimeException(error);}});
    clickSystemText("Cloudy");SystemClock.sleep(200);check("Synthetic town".equals(draft(weather,"draftCity").getText().toString()),"Condition selection lost city draft");screenshot("v5-weather-editor");clickSystemText("Done");SystemClock.sleep(250);
    JSONObject saved=new JSONObject(prefs.getString("item-"+weatherId,"{}"));check(saved.getString("location").equals("Synthetic town")&&saved.getString("temperature").equals("19")&&saved.getString("condition").equals("Cloudy")&&saved.getLong("weatherUpdated")>0,"Weather data did not persist coherently");
    java.io.File source=new java.io.File(home.getCacheDir(),"synthetic-photo.png");Bitmap image=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(image);c.drawColor(Color.WHITE);Paint p=new Paint();p.setColor(Color.BLUE);c.drawRect(30,30,90,90,p);try(var out=new java.io.FileOutputStream(source)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
    android.net.Uri imported=(android.net.Uri)staticCall("dev.dothome.widgets.PhotoCollection","importImage",new Class<?>[]{Context.class,android.net.Uri.class},home,android.net.Uri.fromFile(source));
    android.net.Uri cutout=(android.net.Uri)staticCall("dev.dothome.widgets.PhotoCollection","removePlainBackground",new Class<?>[]{Context.class,android.net.Uri.class,int.class},home,imported,35);Bitmap result=BitmapFactory.decodeFile(cutout.getPath());check(Color.alpha(result.getPixel(0,0))==0&&Color.alpha(result.getPixel(60,60))==255,"Plain-background cutout erased subject or retained corner");result.recycle();
    Bitmap effectSource=BitmapFactory.decodeFile(imported.getPath());
    for(String effect:new String[]{"glass","atmosphere"}){Bitmap effected=(Bitmap)staticCall("dev.dothome.ui.LocalWallpaperEffects","apply",new Class<?>[]{Bitmap.class,String.class},effectSource,effect);check(effected.getWidth()==effectSource.getWidth()&&effected.getHeight()==effectSource.getHeight()&&!effected.sameAs(effectSource),"Local wallpaper effect did not alter the image: "+effect);check(!effectSource.isRecycled(),"Effect recycled its caller-owned source");effected.recycle();}effectSource.recycle();
    JSONObject photo=new JSONObject().put("type","photo").put("uri",imported.toString()).put("images",new JSONArray().put(imported.toString()).put(cutout.toString())).put("imageIndex",0);
    check((Boolean)staticCall("dev.dothome.widgets.PhotoCollection","cycle",new Class<?>[]{JSONObject.class,int.class},photo,1),"Collection did not advance");check(photo.getString("uri").equals(cutout.toString()),"Collection displayed wrong image");
    Activity wallpaper=editor(900003,new JSONObject().put("type","photo").put("wallpaper",true).put("uri",imported.toString()));
    View preview=null;for(int n=0;n<30;n++){var f=wallpaper.getClass().getDeclaredField("photo");f.setAccessible(true);preview=(View)f.get(wallpaper);if(preview!=null){var loaded=preview.getClass().getDeclaredField("image");loaded.setAccessible(true);if(loaded.get(preview)!=null)break;}SystemClock.sleep(100);}
    final View rendered=preview;check(rendered!=null,"Wallpaper preview missing");final int[] changed={0};onUi(()->{Bitmap capture=Bitmap.createBitmap(rendered.getWidth(),rendered.getHeight(),Bitmap.Config.ARGB_8888);rendered.draw(new Canvas(capture));for(int y=0;y<capture.getHeight();y+=8)for(int x=0;x<capture.getWidth();x+=8)if(Color.alpha(capture.getPixel(x,y))>0)changed[0]++;capture.recycle();});check(changed[0]>100,"Wallpaper preview clipped the whole image");screenshot("v5-local-wallpaper-editor");clickSystemText("Cancel");homeKey();
    // Backup image path traversal must be rejected before any writes.
    try{staticCall("dev.dothome.ui.DotBackupMedia","validate",new Class<?>[]{JSONObject.class},new JSONObject().put("../escape.jpg","AA=="));throw new AssertionError("Unsafe backup media name accepted");}catch(java.lang.reflect.InvocationTargetException expected){check(expected.getCause() instanceof java.io.IOException,"Unexpected backup validation failure");}
    prefs.edit().remove("item-"+noteId).remove("item-"+weatherId).commit();new java.io.File(imported.getPath()).delete();new java.io.File(cutout.getPath()).delete();source.delete();
  }
  void textAndDeniedStates()throws Exception{
    try{
      shell("settings put system font_scale 1.3");SystemClock.sleep(700);settings("appearance");visible("Theme & Style");screenshot("v5-large-text-theme");homeKey();
      ActivityMonitor monitor=addMonitor("dev.dothome.widgets.TileDetails",null,false);
      getTargetContext().startActivity(new Intent().setClassName("dev.dothome","dev.dothome.widgets.TileDetails").putExtra("itemId",900004L).putExtra("initial",new JSONObject().put("type","away").toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      Activity details=waitForMonitorWithTimeout(monitor,5000);removeMonitor(monitor);check(details!=null,"History detail screen did not open");visible("Allow usage history");screenshot("v5-denied-history");
      onUi(()->details.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL));SystemClock.sleep(200);screenshot("v5-rtl-owned-screen");shell("input keyevent KEYCODE_BACK");homeKey();
      for(String permission:new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.READ_CALENDAR,android.Manifest.permission.ACTIVITY_RECOGNITION})check(home.checkSelfPermission(permission)!=android.content.pm.PackageManager.PERMISSION_GRANTED,"Optional permission unexpectedly granted: "+permission);
    }finally{shell("settings put system font_scale 1.0");SystemClock.sleep(500);}
  }
  @Override public void onStart(){Bundle result=new Bundle();try{
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);launchHomeAsync();SystemClock.sleep(800);homeKey();themes();hiddenCancellation();categories();presetUndo();widgetData();textAndDeniedStates();
    result.putString("stream","PASS: v5 Classic/Lucent light/dark preserve layout; grid preview; hidden cancellation/back; category correction; preset/undo; note and weather drafts; owned photos/cutout/cycle; wallpaper preview; backup path rejection.\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){try{screenshot("v5-failure");}catch(Throwable ignored){}result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
