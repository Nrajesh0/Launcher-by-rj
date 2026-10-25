package dev.dothome.test;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;

/** Exercises the actual replacement engine, without product Accessibility or shell role grants. */
public final class FoundationSmoke extends UiSmoke {
  @Override void syncActivity() {
    try {
      Class<?> cls=Class.forName("app.lawnchair.LawnchairLauncher",true,getTargetContext().getClassLoader());
      Object companion=cls.getField("Companion").get(null);
      Activity current=(Activity)companion.getClass().getMethod("getInstance").invoke(companion);
      if(current!=null && !current.isDestroyed()) home=current;
    } catch(Exception error) { throw new RuntimeException(error); }
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
      var node=findSystemText(getUiAutomation().getRootInActiveWindow(),value);
      if(node!=null && node.isVisibleToUser())return;
      SystemClock.sleep(100);
    }
    throw new AssertionError("Missing foreground text: "+value);
  }
  void longPressBlank() throws Exception {
    final View[] workspace={null};onUi(()->workspace[0]=(View)invoke(home,"getWorkspace"));
    int[] at=new int[2];onUi(()->workspace[0].getLocationOnScreen(at));
    float x=at[0]+workspace[0].getWidth()*.79f,y=at[1]+workspace[0].getHeight()*.46f;
    long start=SystemClock.uptimeMillis();
    MotionEvent down=MotionEvent.obtain(start,start,MotionEvent.ACTION_DOWN,x,y,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(down,true);down.recycle();
    SystemClock.sleep(900);
    MotionEvent up=MotionEvent.obtain(start,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);getUiAutomation().injectInputEvent(up,true);up.recycle();
    SystemClock.sleep(500);
  }
  void homeKey() throws Exception { shell("input keyevent KEYCODE_HOME");SystemClock.sleep(700);onUi(()->{});awaitState("NORMAL"); }
  @Override public void onStart() {
    Bundle result=new Bundle();
    try {
      getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
      home=startActivitySync(new Intent().setClassName("dev.dothome","dev.dothome.HomeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      waitForIdleSync();SystemClock.sleep(1200);
      chooseDefaultHome();homeKey();
      for(int n=0;n<50 && !home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false);n++)SystemClock.sleep(100);
      if(!home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false))throw new AssertionError("Native workspace migration/default transaction did not complete");
      screenshot("foundation-home-light");
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
      SystemClock.sleep(500);homeKey();screenshot("foundation-home-dark");
      longPressBlank();if(!clickSystemText("Arrange"))throw new AssertionError("Arrange entry did not respond");
      visible("Select apps");visible("Done");screenshot("foundation-arrange");
      if(!clickSystemText("Done"))throw new AssertionError("Arrange did not finish");awaitState("NORMAL");
      longPressBlank();if(!clickSystemText("Widgets"))throw new AssertionError("Widget entry did not respond");
      visible("Dot Home");screenshot("foundation-widget-tray");
      homeKey();
      var info=home.getPackageManager().getPackageInfo("dev.dothome",android.content.pm.PackageManager.GET_SERVICES|android.content.pm.PackageManager.GET_PERMISSIONS);
      for(var service:info.services)if("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(service.permission))throw new AssertionError("Accessibility service is registered");
      for(String permission:info.requestedPermissions)if(permission.equals("android.permission.INTERNET")||permission.equals("android.permission.WRITE_SECURE_SETTINGS")||permission.equals("android.permission.SYSTEM_ALERT_WINDOW"))throw new AssertionError("Unexpected capability: "+permission);
      shell("am start -a android.settings.SETTINGS");SystemClock.sleep(500);homeKey();screenshot("foundation-system-home");
      shell("input keyevent KEYCODE_APP_SWITCH");SystemClock.sleep(800);assertNoSystemError();screenshot("foundation-system-recents");homeKey();
      result.putString("stream","PASS: foundation Home chooser and workspace transaction, native swipe-up drawer, hidden-icons swipe, compact popup, Theme & Style, arrangement mode, widget tray, standard Home/Recents and offline/no-Accessibility package.\n");
      finish(Activity.RESULT_OK,result);
    } catch(Throwable failure) {
      String diagnostics="";
      try {shell("screencap -p /sdcard/Download/foundation-failure.png");diagnostics=systemTree(getUiAutomation().getRootInActiveWindow(),0);}catch(Throwable ignored){}
      result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure)+diagnostics);finish(Activity.RESULT_CANCELED,result);
    }
  }
}
