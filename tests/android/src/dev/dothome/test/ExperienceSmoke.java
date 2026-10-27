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
  @Override boolean clickSystemText(String value){
    for(int n=0;n<30;n++){if(super.clickSystemText(value)){SystemClock.sleep(200);return true;}SystemClock.sleep(80);}return false;
  }
  Class<?> cls(String name)throws Exception{return Class.forName(name,true,getTargetContext().getClassLoader());}
  Map<Integer,String> placement(){
    Map<Integer,String> result=new TreeMap<>();SQLiteDatabase db=(SQLiteDatabase)invoke(invoke(invoke(home,"getModel"),"getModelDbController"),"getDb");
    try(var cursor=db.rawQuery("SELECT _id,container,screen,cellX,cellY,spanX,spanY FROM favorites ORDER BY _id",null)){
      while(cursor.moveToNext()){StringBuilder value=new StringBuilder();for(int n=1;n<7;n++)value.append(cursor.getInt(n)).append(':');result.put(cursor.getInt(0),value.toString());}
    }return result;
  }
  void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
  void settings(String route)throws Exception{
    onUi(()->home.startActivity(new Intent().setClassName("dev.dothome","app.lawnchair.ui.preferences.PreferenceActivity").putExtra("dotRoute",route)));SystemClock.sleep(550);
  }
  int views(View view,String name){int count=view.getClass().getName().equals(name)?1:0;if(view instanceof ViewGroup group)for(int n=0;n<group.getChildCount();n++)count+=views(group.getChildAt(n),name);return count;}
  void themes()throws Exception{
    Map<Integer,String> original=placement();
    for(String material:new String[]{"Classic","Lucent"})for(boolean dark:new boolean[]{false,true}){
      settings("appearance");visible(material);check(clickSystemText(material),"Material selection failed");
      if(home.getSharedPreferences("dot-style",0).getBoolean("dark",false)!=dark){visible("Dark theme");check(clickSystemText("Dark theme"),"Dark theme selection failed");}
      SystemClock.sleep(600);onUi(()->{syncActivity();check(home!=null&&!home.isDestroyed(),"Home recreation invalidated the visible preview");});screenshot("v5-theme-"+material.toLowerCase()+"-"+(dark?"dark":"light"));homeKey();SystemClock.sleep(500);
      check(original.equals(placement()),"Theme selection changed the layout");screenshot("v5-home-"+material.toLowerCase()+"-"+(dark?"dark":"light"));
    }
    settings("grid");visible("Home screen layout");screenshot("v5-grid-choices");homeKey();check(original.equals(placement()),"Opening grid settings changed layout");
  }
  void hiddenCancellation()throws Exception{
    View workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");
    View apps=(View)invoke(home,"getAppsView");int[] position=new int[2];onUi(()->apps.getLocationOnScreen(position));
    float x=position[0]+apps.getWidth()*.22f,y=position[1]+apps.getHeight()*.45f;long down=SystemClock.uptimeMillis();
    for(int n=0;n<8;n++){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),n==0?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_MOVE,x+n*10,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(e,true),"Touch injection failed");e.recycle();SystemClock.sleep(16);}
    final int[] partial={0};onUi(()->partial[0]=views(home.getWindow().getDecorView(),"dev.dothome.ui.DotHiddenPanel$Panel"));check(partial[0]==1,"Partial hidden gesture never entered its page");
    MotionEvent cancel=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,x+70,y,0);cancel.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(cancel,true),"Cancel injection failed");cancel.recycle();SystemClock.sleep(350);
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
    onUi(()->home.startActivity(new Intent().setClassName("dev.dothome","dev.dothome.widgets.TileConfiguration").putExtra("itemId",id).putExtra("initial",item.toString())));
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
  void beginExternalWidget(android.appwidget.AppWidgetProviderInfo provider)throws Exception{
    onUi(()->{try{
      Class<?> infoType=cls("com.android.launcher3.widget.LauncherAppWidgetProviderInfo");Object info=infoType.getMethod("fromProviderInfo",Context.class,android.appwidget.AppWidgetProviderInfo.class).invoke(null,home,provider);
      int w=infoType.getField("spanX").getInt(info),h=infoType.getField("spanY").getInt(info);Object workspace=invoke(home,"getWorkspace");int pages=(Integer)invoke(workspace,"getPageCount");
      for(int n=0;n<pages;n++){int screen=(Integer)api(workspace,"getScreenIdForPageIndex",new Class<?>[]{int.class},n);if(screen<0)continue;Object page=api(workspace,"getScreenWithId",new Class<?>[]{int.class},screen);int[] cell=new int[2];if((Boolean)api(page,"findCellForSpan",new Class<?>[]{int[].class,int.class,int.class},cell,w,h)){
        api(workspace,"snapToPage",new Class<?>[]{int.class},n);Object pending=cls("com.android.launcher3.widget.PendingAddWidgetInfo").getConstructor(infoType,int.class).newInstance(info,-105);
        api(home,"addPendingItem",new Class<?>[]{cls("com.android.launcher3.PendingAddItemInfo"),int.class,int.class,int[].class,int.class,int.class},pending,-100,screen,cell,w,h);return;
      }}throw new AssertionError("No room for external widget fixture");
    }catch(Exception error){throw new RuntimeException(error);}});
  }
  void externalWidget()throws Exception{
    onUi(()->{try{cls("dev.dothome.widgets.DotWidgetTray").getMethod("show",cls("com.android.launcher3.Launcher")).invoke(null,home);}catch(Exception error){throw new RuntimeException(error);}});visible("Installed apps");clickSystemText("Installed apps");SystemClock.sleep(500);
    final int[] sheets={0};onUi(()->sheets[0]=views(home.getWindow().getDecorView(),"com.android.launcher3.widget.picker.WidgetsFullSheet"));check(sheets[0]>0,"Installed-app entry did not open the native picker");screenshot("v5-installed-widget-picker");homeKey();
    android.appwidget.AppWidgetManager manager=android.appwidget.AppWidgetManager.getInstance(home);android.appwidget.AppWidgetProviderInfo provider=null;for(var candidate:manager.getInstalledProviders())if(candidate.provider.getClassName().equals("dev.dothome.test.WidgetProvider"))provider=candidate;check(provider!=null,"External widget fixture missing");
    Map<Integer,String> before=placement();beginExternalWidget(provider);configureFixture(true);
    for(int n=0;n<40&&placement().size()==before.size();n++)SystemClock.sleep(100);check(placement().size()==before.size()+1,"Configured external widget not durably placed");
    int row=-1,widget=-1;SQLiteDatabase db=(SQLiteDatabase)invoke(invoke(invoke(home,"getModel"),"getModelDbController"),"getDb");try(var cursor=db.rawQuery("SELECT _id,appWidgetId FROM favorites WHERE appWidgetProvider LIKE '%WidgetProvider%'",null)){check(cursor.moveToFirst(),"External widget row missing");row=cursor.getInt(0);widget=cursor.getInt(1);}check(manager.getAppWidgetInfo(widget)!=null,"External widget binding missing");visible("Provider widget configured");screenshot("v5-external-widget-configured");
    Map<Integer,String> placed=placement();beginExternalWidget(provider);configureFixture(false);for(int n=0;n<40&&!placed.equals(placement());n++)SystemClock.sleep(100);check(placed.equals(placement()),"Cancelled provider configuration changed the layout: before="+placed+" after="+placement());
    final int remove=row;onUi(()->{View view=nativeItem(remove);check(view!=null,"External widget view missing");try{api(home,"removeItem",new Class<?>[]{View.class,cls("com.android.launcher3.model.data.ItemInfo"),boolean.class,String.class},view,view.getTag(),true,"Synthetic widget test cleanup");}catch(Exception error){throw new RuntimeException(error);}});
    for(int n=0;n<40&&!before.equals(placement());n++)SystemClock.sleep(100);check(before.equals(placement()),"External widget removal did not restore the layout");check(manager.getAppWidgetInfo(widget)==null,"Removed external widget binding leaked");homeKey();
  }
  void calculatorScope()throws Exception{
    var prefs=home.getSharedPreferences("com.android.launcher3.prefs",0);boolean original=prefs.getBoolean("pref_searchResultCalculator",false);
    try{
      settings("search");visible("Calculator");if(!prefs.getBoolean("pref_searchResultCalculator",false))clickSystemText("Calculator");homeKey();
      View workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");
      final EditText[] input={null};onUi(()->{int id=home.getResources().getIdentifier("input","id","dev.dothome");input[0]=((View)invoke(home,"getAppsView")).findViewById(id);check(input[0]!=null,"Drawer search input missing");input[0].requestFocus();input[0].setText("6*7");});
      visible("42");screenshot("v5-offline-calculator");homeKey();settings("search");visible("Calculator");clickSystemText("Calculator");homeKey();
      workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");onUi(()->{int id=home.getResources().getIdentifier("input","id","dev.dothome");EditText field=((View)invoke(home,"getAppsView")).findViewById(id);field.requestFocus();field.setText("6*7");});SystemClock.sleep(900);
      check(findSystemText(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).getRootInActiveWindow(),"42")==null,"Disabled calculator scope still returned a calculation");homeKey();
    }finally{prefs.edit().putBoolean("pref_searchResultCalculator",original).commit();}
  }
  void selectedDataScopes()throws Exception{
    settings("search");visible("Contacts");check(clickSystemText("Contacts"),"Contacts scope did not request optional access");
    check(clickSystemText("Don't allow")||clickSystemText("Don’t allow")||clickSystemText("Deny"),"Contact access refusal was not available");SystemClock.sleep(300);
    check(home.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&!home.getSharedPreferences("com.android.launcher3.prefs",0).getBoolean("pref_searchResultPeople",false),"Refused contact access left its scope enabled");homeKey();
    java.io.File directory=new java.io.File(home.getCacheDir(),"Overview");directory.mkdirs();java.io.File selected=new java.io.File(directory,"rj-selected-fixture.txt"),unselected=new java.io.File(directory,"rj-unselected-fixture.txt");try(var out=new java.io.FileOutputStream(selected)){out.write("Synthetic selected document".getBytes(java.nio.charset.StandardCharsets.UTF_8));}try(var out=new java.io.FileOutputStream(unselected)){out.write("This must not be indexed".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    android.net.Uri uri=(android.net.Uri)cls("androidx.core.content.FileProvider").getMethod("getUriForFile",Context.class,String.class,java.io.File.class).invoke(null,home,"dev.dothome.overview.fileprovider",selected);
    staticCall("dev.dothome.ui.DotDocuments","add",new Class<?>[]{Context.class,java.util.List.class},home,java.util.List.of(uri));
    settings("search");visible("Selected documents");if(!home.getSharedPreferences("dot-style",0).getBoolean("searchDocuments",false))clickSystemText("Selected documents");homeKey();
    var found=(java.util.List<?>)staticCall("dev.dothome.ui.DotDocuments","search",new Class<?>[]{Context.class,String.class},home,"rj-selected");check(found.size()==1,"Selected document metadata was not searchable");check(((java.util.List<?>)staticCall("dev.dothome.ui.DotDocuments","search",new Class<?>[]{Context.class,String.class},home,"rj-unselected")).isEmpty(),"Document search scanned an unselected file");
    View workspace=(View)invoke(home,"getWorkspace");swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");onUi(()->{int id=home.getResources().getIdentifier("input","id","dev.dothome");EditText field=((View)invoke(home,"getAppsView")).findViewById(id);field.requestFocus();field.setText("rj-selected");});visible("rj-selected-fixture.txt");screenshot("v5-selected-document-search");homeKey();
    staticCall("dev.dothome.ui.DotDocuments","retain",new Class<?>[]{Context.class,boolean[].class},home,new boolean[]{false});check(((java.util.List<?>)staticCall("dev.dothome.ui.DotDocuments","search",new Class<?>[]{Context.class,String.class},home,"rj-selected")).isEmpty(),"Removed document stayed in search");home.getSharedPreferences("dot-style",0).edit().putBoolean("searchDocuments",false).commit();selected.delete();unselected.delete();
  }
  void textAndDeniedStates()throws Exception{
    try{
      shell("settings put system font_scale 1.3");SystemClock.sleep(700);settings("appearance");visible("Theme & Style");screenshot("v5-large-text-theme");homeKey();
      ActivityMonitor monitor=addMonitor("dev.dothome.widgets.TileDetails",null,false);
      String initial=new JSONObject().put("type","away").toString();onUi(()->home.startActivity(new Intent().setClassName("dev.dothome","dev.dothome.widgets.TileDetails").putExtra("itemId",900004L).putExtra("initial",initial)));
      Activity details=waitForMonitorWithTimeout(monitor,5000);removeMonitor(monitor);check(details!=null,"History detail screen did not open");visible("Allow usage history");screenshot("v5-denied-history");
      onUi(()->details.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL));SystemClock.sleep(200);screenshot("v5-rtl-owned-screen");shell("input keyevent KEYCODE_BACK");homeKey();
      for(String permission:new String[]{android.Manifest.permission.CAMERA,android.Manifest.permission.READ_CALENDAR,android.Manifest.permission.ACTIVITY_RECOGNITION,android.Manifest.permission.READ_CONTACTS})check(home.checkSelfPermission(permission)!=android.content.pm.PackageManager.PERMISSION_GRANTED,"Optional permission unexpectedly granted: "+permission);
    }finally{shell("settings put system font_scale 1.0");SystemClock.sleep(500);}
  }
  @Override public void onStart(){Bundle result=new Bundle();try{
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);launchHomeAsync();SystemClock.sleep(800);homeKey();themes();hiddenCancellation();categories();presetUndo();widgetData();externalWidget();calculatorScope();selectedDataScopes();textAndDeniedStates();
    result.putString("stream","PASS: v5 Classic/Lucent light/dark preserve layout; grid preview; hidden cancellation/back; category correction; preset/undo; note and weather drafts; owned photos/cutout/cycle; wallpaper preview; backup path rejection.\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){try{screenshot("v5-failure");}catch(Throwable ignored){}String diagnostics="";try{diagnostics=systemTree(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).getRootInActiveWindow(),0);}catch(Throwable ignored){}result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error)+diagnostics);finish(Activity.RESULT_CANCELED,result);}}
}
