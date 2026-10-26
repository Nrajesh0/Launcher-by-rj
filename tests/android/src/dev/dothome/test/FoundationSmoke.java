package dev.dothome.test;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;

/** Exercises the actual replacement engine, without product Accessibility or shell role grants. */
public class FoundationSmoke extends UiSmoke {
  @Override void syncActivity() {
    try {
      Class<?> cls=Class.forName("app.lawnchair.LawnchairLauncher",true,getTargetContext().getClassLoader());
      Object companion=cls.getField("Companion").get(null);
      Activity current=(Activity)companion.getClass().getMethod("getInstance").invoke(companion);
      if(current!=null && !current.isDestroyed()) home=current;
    } catch(Exception error) { throw new RuntimeException(error); }
  }
  void launchHomeAsync() {
    getTargetContext().startActivity(new Intent().setClassName("dev.dothome","dev.dothome.HomeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    for(int n=0;n<150;n++) {
      try {syncActivity();if(home!=null && !home.isDestroyed())return;}catch(RuntimeException notReady){}
      SystemClock.sleep(100);
    }
    throw new AssertionError("Home Activity was not created after asynchronous launch");
  }
  Object invoke(Object target,String method) {
    try { return target.getClass().getMethod(method).invoke(target); }
    catch(Exception error) { throw new RuntimeException(method,error); }
  }
  boolean state(String key) {
    try {
      Class<?> cls=Class.forName("com.android.launcher3.LauncherState",true,getTargetContext().getClassLoader());
      return invoke(invoke(home,"getStateManager"),"getState")==cls.getField(key).get(null);
    } catch(Exception error) { throw new RuntimeException(error); }
  }
  void awaitState(String key) {
    for(int n=0;n<60;n++) { final boolean[] match={false};onUi(()->match[0]=state(key));if(match[0])return;SystemClock.sleep(80); }
    throw new AssertionError("Native launcher did not reach "+key);
  }
  void visible(String value) {
    for(int n=0;n<40;n++) {
      var node=findSystemText(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).getRootInActiveWindow(),value);
      if(node!=null && node.isVisibleToUser())return;
      SystemClock.sleep(100);
    }
    throw new AssertionError("Missing foreground text: "+value);
  }
  void longPressBlank() throws Exception {
    final View[] workspace={null};onUi(()->workspace[0]=(View)invoke(home,"getWorkspace"));
    int[] at=new int[2];onUi(()->workspace[0].getLocationOnScreen(at));
    float x=at[0]+workspace[0].getWidth()*.12f,y=at[1]+workspace[0].getHeight()*.46f;
    long start=SystemClock.uptimeMillis();
    MotionEvent down=MotionEvent.obtain(start,start,MotionEvent.ACTION_DOWN,x,y,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(down,true);down.recycle();
    SystemClock.sleep(900);
    MotionEvent up=MotionEvent.obtain(start,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(up,true);up.recycle();
    SystemClock.sleep(500);
  }
  Object api(Object target,String method,Class<?>[] types,Object... args) {
    try {return target.getClass().getMethod(method,types).invoke(target,args);}catch(Exception error){throw new RuntimeException(method,error);}
  }
  View nativeItem(int id) {
    return (View)api(invoke(home,"getWorkspace"),"getHomescreenIconByItemId",new Class<?>[]{int.class},id);
  }
  void batchMove() throws Exception {
    final int[] ids=new int[2];
    onUi(()->{
      try {
        Object[] apps=(Object[])invoke(invoke(invoke(home,"getAppsView"),"getAppsStore"),"getApps");
        if(apps.length<2)throw new AssertionError("Native app fixture is empty");
        Class<?> itemType=Class.forName("com.android.launcher3.model.data.ItemInfo",true,getTargetContext().getClassLoader());
        Object writer=invoke(home,"getModelWriter");
        for(int n=0;n<2;n++) {
          Object item=api(apps[n],"makeWorkspaceItem",new Class<?>[]{android.content.Context.class},getTargetContext());
          api(writer,"addItemToDatabase",new Class<?>[]{itemType,int.class,int.class,int.class,int.class},item,-100,0,2+n,3);
          ids[n]=itemType.getField("id").getInt(item);
        }
        invoke(invoke(home,"getModel"),"forceReload");
      }catch(Exception error){throw new RuntimeException(error);}
    });
    final View[] views=new View[2];
    for(int n=0;n<40;n++){onUi(()->{views[0]=nativeItem(ids[0]);views[1]=nativeItem(ids[1]);});if(views[0]!=null&&views[1]!=null)break;SystemClock.sleep(100);}
    if(views[0]==null||views[1]==null)throw new AssertionError("Native test apps were not bound");
    longPressBlank();visible("Arrange");if(!clickSystemText("Arrange"))throw new AssertionError("Arrange touch was not accepted");awaitState("EDIT_MODE");
    awaitState("EDIT_MODE");visible("Select items");
    for(int n=0;n<2;n++) {
      final View item=views[n];int[] at=new int[2];onUi(()->item.getLocationOnScreen(at));
      long downTime=SystemClock.uptimeMillis();
      for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
        MotionEvent touch=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,at[0]+item.getWidth()*.5f,at[1]+item.getHeight()*.5f,0);touch.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(touch,true);touch.recycle();SystemClock.sleep(50);
      }
      SystemClock.sleep(250);
    }
    visible("2 selected");screenshot("foundation-batch-selected");
    if(!clickSystemText("Move"))throw new AssertionError("Batch move did not respond");visible("Move selection");
    if(!clickSystemText("New page"))throw new AssertionError("Batch target did not respond");awaitState("NORMAL");
    final int[] screens={0,0};
    for(int n=0;n<40;n++) {
      onUi(()->{try {for(int item=0;item<2;item++){View icon=nativeItem(ids[item]);if(icon!=null)screens[item]=icon.getTag().getClass().getField("screenId").getInt(icon.getTag());}}catch(Exception error){throw new RuntimeException(error);}});
      if(screens[0]!=0&&screens[0]==screens[1])break;SystemClock.sleep(100);
    }
    if(screens[0]==0||screens[0]!=screens[1])throw new AssertionError("Batch did not durably move both native items to a new page");
    screenshot("foundation-batch-moved");
    groupDrag(ids,screens[0]);
    backupRoundTrip();
    folderRoundTrip(ids,screens[0]);
  }
  void groupDrag(int[] ids,int screen) throws Exception {
    onUi(()->{Object workspace=invoke(home,"getWorkspace");int index=(Integer)api(workspace,"getPageIndexForScreenId",new Class<?>[]{int.class},screen);api(workspace,"snapToPage",new Class<?>[]{int.class},index);});
    SystemClock.sleep(800);longPressBlank();visible("Arrange");clickSystemText("Arrange");awaitState("EDIT_MODE");
    final View[] icons=new View[2];onUi(()->{icons[0]=nativeItem(ids[0]);icons[1]=nativeItem(ids[1]);});
    tapView(icons[0]);tapView(icons[1]);visible("2 selected");
    final android.graphics.Rect start=new android.graphics.Rect(),target=new android.graphics.Rect();
    onUi(()->{
      icons[0].getGlobalVisibleRect(start);
      View page=(View)api(invoke(home,"getWorkspace"),"getScreenWithId",new Class<?>[]{int.class},screen);page.getGlobalVisibleRect(target);
    });
    // Free row on the same page. Keep both relative cells together.
    float x=start.exactCenterX(),y=start.exactCenterY(),tx=target.left+target.width()*.38f,ty=target.top+target.height()*.39f;
    long down=SystemClock.uptimeMillis();
    for(int n=0;n<=12;n++) {
      int action=n==0?MotionEvent.ACTION_DOWN:n==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
      MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x+(tx-x)*n/12f,y+(ty-y)*n/12f,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(event,true);event.recycle();SystemClock.sleep(n==0?400:32);
    }
    awaitState("NORMAL");
    final int[] cells={-1,-1,-1,-1};
    for(int n=0;n<50;n++) {
      onUi(()->{try{for(int j=0;j<2;j++){View view=nativeItem(ids[j]);if(view!=null){Object item=view.getTag();cells[j*2]=item.getClass().getField("cellX").getInt(item);cells[j*2+1]=item.getClass().getField("cellY").getInt(item);}}}catch(Exception error){throw new RuntimeException(error);}});
      if(cells[1]>0&&cells[1]==cells[3]&&cells[2]==cells[0]+1)break;SystemClock.sleep(100);
    }
    if(cells[1]<=0||cells[1]!=cells[3]||cells[2]!=cells[0]+1)throw new AssertionError("Group drag did not persist relative app placement");
    screenshot("foundation-group-dragged");
  }
  void backupRoundTrip() throws Exception {
    java.io.File document=new java.io.File(home.getFilesDir(),"synthetic-backup-check.json");
    Class<?> cls=Class.forName("dev.dothome.ui.DotBackup",true,getTargetContext().getClassLoader());
    cls.getMethod("export",Activity.class,android.net.Uri.class).invoke(null,home,android.net.Uri.fromFile(document));
    for(int n=0;n<60&&!document.isFile();n++)SystemClock.sleep(100);
    if(!document.isFile())throw new AssertionError("Local backup document was not written");
    SystemClock.sleep(250);
    onUi(()->{try{cls.getMethod("inspect",Activity.class,android.net.Uri.class).invoke(null,home,android.net.Uri.fromFile(document));}catch(Exception error){throw new RuntimeException(error);}});
    visible("Restore");screenshot("foundation-restore-confirmation");if(!clickSystemText("Restore"))throw new AssertionError("Restore confirmation did not respond");
    SystemClock.sleep(1500);homeKey();document.delete();
    final int[] art={0};onUi(()->art[0]=artworkCount((View)invoke(home,"getWorkspace")));
    if(art[0]<8)throw new AssertionError("Backup restore lost built-in widget artwork");
  }
  View folderOnPage(int screen) {
    android.view.ViewGroup page=(android.view.ViewGroup)api(invoke(home,"getWorkspace"),"getScreenWithId",new Class<?>[]{int.class},screen);
    if(page==null)return null;
    return folderChild(page);
  }
  View folderChild(View view) {
    if(view.getTag()!=null && view.getTag().getClass().getName().equals("com.android.launcher3.model.data.FolderInfo"))return view;
    if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++){View result=folderChild(group.getChildAt(n));if(result!=null)return result;}
    return null;
  }
  void folderRoundTrip(int[] ids,int screen) throws Exception {
    onUi(()->{Object workspace=invoke(home,"getWorkspace");int index=(Integer)api(workspace,"getPageIndexForScreenId",new Class<?>[]{int.class},screen);api(workspace,"snapToPage",new Class<?>[]{int.class},index);});
    SystemClock.sleep(800);longPressBlank();visible("Arrange");clickSystemText("Arrange");awaitState("EDIT_MODE");
    final View[] views=new View[2];onUi(()->{views[0]=nativeItem(ids[0]);views[1]=nativeItem(ids[1]);});tapView(views[0]);tapView(views[1]);visible("2 selected");
    if(!clickSystemText("Folder"))throw new AssertionError("Folder action did not respond");visible("Folder name");if(!clickSystemText("Create"))throw new AssertionError("Folder creation did not respond");awaitState("NORMAL");
    final View[] folder={null};for(int n=0;n<50;n++){onUi(()->folder[0]=folderOnPage(screen));if(folder[0]!=null)break;SystemClock.sleep(100);}
    if(folder[0]==null)throw new AssertionError("Native folder was not bound");
    longPressBlank();visible("Arrange");clickSystemText("Arrange");awaitState("EDIT_MODE");onUi(()->folder[0]=folderOnPage(screen));tapView(folder[0]);visible("1 selected");if(!clickSystemText("Style"))throw new AssertionError("Folder style did not respond");
    visible("Enlarge");if(!clickSystemText("Enlarge"))throw new AssertionError("Folder enlarge did not respond");awaitState("NORMAL");
    final boolean[] expanded={false};
    for(int n=0;n<50;n++) {onUi(()->{try{View view=folderOnPage(screen);if(view!=null){Object item=view.getTag();expanded[0]=item.getClass().getField("spanX").getInt(item)==2 && item.getClass().getField("spanY").getInt(item)==2 && ((java.util.Collection<?>)invoke(item,"getContents")).size()==2;}}catch(Exception error){throw new RuntimeException(error);}});if(expanded[0])break;SystemClock.sleep(100);}
    if(!expanded[0])throw new AssertionError("Expanded folder did not preserve both apps");screenshot("foundation-expanded-folder");
  }
  View firstTile(View view) {
    if(view.getClass().getName().equals("dev.dothome.widgets.TileArtwork"))return view;
    if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++){View result=firstTile(group.getChildAt(n));if(result!=null)return result;}
    return null;
  }
  void tapView(View view) throws Exception {
    android.graphics.Rect bounds=new android.graphics.Rect();onUi(()->view.getGlobalVisibleRect(bounds));
    long down=SystemClock.uptimeMillis();
    for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
      MotionEvent touch=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,bounds.exactCenterX(),bounds.exactCenterY(),0);touch.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).injectInputEvent(touch,true);touch.recycle();SystemClock.sleep(80);
    }
  }
  int artworkCount(View view) {
    int count=view.getClass().getName().equals("dev.dothome.widgets.TileArtwork")?1:0;
    if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++)count+=artworkCount(group.getChildAt(n));
    return count;
  }
  int widgetCount(View view) {
    int total=0;Object tag=view.getTag();
    if(tag!=null && tag.getClass().getName().endsWith("LauncherAppWidgetInfo"))total++;
    if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++)total+=widgetCount(group.getChildAt(n));
    return total;
  }
  String fixtureCounts() {
    final int[] widgets={0};onUi(()->widgets[0]=widgetCount((View)invoke(home,"getWorkspace")));
    var prefs=home.getSharedPreferences("com.android.launcher3.prefs",0);
    StringBuilder output=new StringBuilder("Fixture diagnostics: bound widgets="+widgets[0]+", themed="+prefs.getBoolean("themed_icons",false)+", grid="+prefs.getInt("pref_workspaceColumns",-1)+"x"+prefs.getInt("pref_workspaceRows",-1));
    int widgetRows=0;
    for(String name:home.databaseList()) {
      try(var db=android.database.sqlite.SQLiteDatabase.openDatabase(home.getDatabasePath(name).getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
          var rows=db.rawQuery("SELECT count(*) FROM favorites WHERE itemType IN (4,5)",null)) {if(rows.moveToFirst())widgetRows+=rows.getInt(0);}catch(Exception ignored){}
    }
    int active=-1;
    try {
      var db=(android.database.sqlite.SQLiteDatabase)invoke(invoke(invoke(home,"getModel"),"getModelDbController"),"getDb");
      try(var rows=db.rawQuery("SELECT count(*) FROM favorites WHERE itemType IN (4,5)",null)) {if(rows.moveToFirst())active=rows.getInt(0);}
    }catch(Exception ignored){}
    return output+", stored widget rows="+widgetRows+", active widget rows="+active+"\n";
  }
  void homeKey() throws Exception { shell("input keyevent KEYCODE_HOME");SystemClock.sleep(700);onUi(()->{});awaitState("NORMAL"); }
  @Override public void onStart() {
    Bundle result=new Bundle();
    try {
      getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
      launchHomeAsync();SystemClock.sleep(1200);
      chooseDefaultHome();homeKey();
      for(int n=0;n<50 && !home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false);n++)SystemClock.sleep(100);
      if(!home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false))throw new AssertionError("Native workspace migration/default transaction did not complete");
      SystemClock.sleep(2000);
      screenshot("foundation-home-light");
      android.util.Log.i("DotFoundation",fixtureCounts());
      final int[] placed={0};
      for(int n=0;n<30;n++) {onUi(()->placed[0]=widgetCount((View)invoke(home,"getWorkspace")));if(placed[0]>=8)break;SystemClock.sleep(100);}
      onUi(()->placed[0]=artworkCount((View)invoke(home,"getWorkspace")));
      if(placed[0]<8)throw new AssertionError("Default native widgets were not bound: "+placed[0]);
      final boolean[] dialer={false};
      onUi(()->{
        Object[] apps=(Object[])invoke(invoke(invoke(home,"getAppsView"),"getAppsStore"),"getApps");
        for(Object app:apps) {android.content.ComponentName component=(android.content.ComponentName)invoke(app,"getTargetComponent");if(component!=null && (component.getPackageName().equals("com.android.dialer") || component.getPackageName().equals("com.google.android.dialer")))dialer[0]=true;}
      });
      if(!dialer[0])throw new AssertionError("Android 16 fixture Dialer is missing from Apps; launcher package visibility is incomplete");
      View workspace=(View)invoke(home,"getWorkspace");
      swipe(workspace,.53f,.72f,0,-300,false);awaitState("ALL_APPS");screenshot("foundation-apps");
      View appsView=(View)invoke(home,"getAppsView");
      swipe(appsView,.18f,.45f,200,0,false);visible("Hidden icons");screenshot("foundation-hidden-icons");
      homeKey();longPressBlank();visible("Customisation");visible("Widgets");visible("Home settings");visible("Arrange");screenshot("foundation-home-menu");
      if(!clickSystemText("Customisation"))throw new AssertionError("Customisation action did not respond");
      visible("Theme & Style");screenshot("foundation-customisation");
      if(!clickSystemText("Theme & Style"))throw new AssertionError("Theme entry did not respond");
      visible("Classic");visible("Dynamic");visible("Dark theme");visible("Icon pack");screenshot("foundation-theme-style");
      if(!clickSystemText("Dark theme"))throw new AssertionError("Dark theme row did not respond");
      SystemClock.sleep(500);
      if(!home.getSharedPreferences("dot-style",0).getBoolean("dark",false))throw new AssertionError("Dark theme label did not toggle the setting");
      shell("input keyevent KEYCODE_BACK");visible("Wallpaper");screenshot("foundation-settings-back");
      homeKey();screenshot("foundation-home-dark");
      longPressBlank();visible("Arrange");if(!clickSystemText("Arrange"))throw new AssertionError("Arrange touch was not accepted");awaitState("EDIT_MODE");
      visible("Select items");visible("Done");screenshot("foundation-arrange");
      final View[] clock={null};
      onUi(()->{android.view.ViewGroup page=(android.view.ViewGroup)api(invoke(home,"getWorkspace"),"getScreenWithId",new Class<?>[]{int.class},0);clock[0]=firstTile(page);});
      if(clock[0]==null)throw new AssertionError("Clock artwork missing in arrangement mode");
      tapView(clock[0]);visible("1 selected");if(!clickSystemText("Style"))throw new AssertionError("Widget style action did not respond");
      visible("Clock face");visible("Minimal");screenshot("foundation-widget-style");
      if(!clickSystemText("Done"))throw new AssertionError("Widget configuration did not save");homeKey();
      longPressBlank();visible("Arrange");clickSystemText("Arrange");awaitState("EDIT_MODE");
      if(!clickSystemText("Done"))throw new AssertionError("Arrange did not finish");awaitState("NORMAL");
      longPressBlank();visible("Widgets");if(!clickSystemText("Widgets"))throw new AssertionError("Widget entry did not respond");
      visible("Dot Home");screenshot("foundation-widget-tray");
      homeKey();
      var info=home.getPackageManager().getPackageInfo("dev.dothome",android.content.pm.PackageManager.GET_SERVICES|android.content.pm.PackageManager.GET_PERMISSIONS);
      for(var service:info.services)if("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(service.permission))throw new AssertionError("Accessibility service is registered");
      for(String permission:info.requestedPermissions)if(permission.equals("android.permission.INTERNET")||permission.equals("android.permission.WRITE_SECURE_SETTINGS")||permission.equals("android.permission.SYSTEM_ALERT_WINDOW"))throw new AssertionError("Unexpected capability: "+permission);
      batchMove();
      shell("am start -a android.settings.SETTINGS");SystemClock.sleep(500);homeKey();screenshot("foundation-system-home");
      shell("input keyevent KEYCODE_APP_SWITCH");SystemClock.sleep(800);assertNoSystemError();screenshot("foundation-system-recents");homeKey();
      result.putString("stream","PASS: foundation Home chooser and workspace transaction, native swipe-up drawer, hidden-icons swipe, compact popup, Theme & Style, arrangement mode, widget tray, standard Home/Recents and offline/no-Accessibility package.\n");
      finish(Activity.RESULT_OK,result);
    } catch(Throwable failure) {
      String diagnostics="";
      try {shell("screencap -p /sdcard/Download/foundation-failure.png");diagnostics=systemTree(getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).getRootInActiveWindow(),0);}catch(Throwable ignored){}
      result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure)+diagnostics+fixtureCounts());finish(Activity.RESULT_CANCELED,result);
    }
  }
}
