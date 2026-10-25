package dev.dothome.test;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import org.json.*;

/** Device-level UI checks. Runs inside a fresh emulator, without app Internet access. */
public class UiSmoke extends Instrumentation {
 Activity home;Object ui;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 void call(Object object,String method,Class<?>[] types,Object...args){try{Method m=object.getClass().getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(object,args);}catch(Exception e){throw new RuntimeException(method,e);}}
 Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new RuntimeException(name,e);}}
 void onUi(Runnable work){runOnMainSync(work);waitForIdleSync();SystemClock.sleep(450);}
 void assertText(String expected){final boolean[] found={false};runOnMainSync(()->found[0]=contains(home.getWindow().getDecorView(),expected));if(!found[0])throw new AssertionError("Missing UI text: "+expected);}
 boolean contains(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),text))return true;return false;}
 void screenshot(String name)throws Exception{ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("screencap -p /sdcard/Download/"+name+".png");try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){in.readAllBytes();}}
 @Override public void onStart(){Bundle result=new Bundle();try{
  home=startActivitySync(new Intent().setClassName("dev.dothome","dev.dothome.HomeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();SystemClock.sleep(1800);ui=field(home,"ui");
  if(getTargetContext().checkSelfPermission("android.permission.INTERNET")!=PackageManager.PERMISSION_DENIED)throw new AssertionError("App has Internet permission");
  onUi(()->{View screen=(View)field(ui,"screen");if(screen.getWidth()<300||screen.getHeight()<600)throw new AssertionError("Workspace not laid out");});screenshot("home");
  onUi(()->call(ui,"drawer",new Class[]{java.util.function.Consumer.class},new Object[]{null}));assertText("Apps");screenshot("drawer");
  onUi(()->call(ui,"close",new Class[]{}));
  onUi(()->call(ui,"widgetPicker",new Class[]{}));assertText("Widgets");assertText("Clock");screenshot("widgets");
  onUi(()->call(ui,"stylePicker",new Class[]{String.class,JSONObject.class},"clock",null));assertText("Choose a style");assertText("ANALOGUE · SAMPLE PREVIEW");screenshot("styles");
  onUi(()->call(ui,"close",new Class[]{}));
  onUi(()->call(ui,"customise",new Class[]{}));assertText("Customisation");assertText("Appearance");screenshot("customisation");
  onUi(()->call(ui,"close",new Class[]{}));
  onUi(()->call(ui,"overview",new Class[]{}));assertText("Home screen");screenshot("overview");
  onUi(()->call(ui,"close",new Class[]{}));
  onUi(()->{try{JSONObject folder=new JSONObject().put("id","ui-test-folder").put("type","folder").put("page",0).put("w",1).put("h",1).put("name","Test folder").put("children",new JSONArray());call(ui,"addItem",new Class[]{JSONObject.class},folder);call(ui,"folder",new Class[]{JSONObject.class},folder);}catch(Exception e){throw new RuntimeException(e);}});assertText("Test folder");screenshot("folder");
  onUi(()->call(ui,"close",new Class[]{}));onUi(()->call(ui,"navigation",new Class[]{}));assertText("Gesture navigation");screenshot("navigation");
  result.putString("stream","PASS: home, integrated drawer, visual picker, style gallery, customisation, overview, folder, navigation and offline permission.\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
