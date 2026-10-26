package dev.dothome.test;
import android.app.*;
import android.content.*;
import android.os.*;
/** The update uses one disposable CI certificate for both APKs; production key stays local. */
public final class FoundationUpgrade extends FoundationSmoke {
  @Override public void onStart(){Bundle result=new Bundle();try {
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
    launchHomeAsync();SystemClock.sleep(1200);chooseDefaultHome();homeKey();
    for(int n=0;n<80&&!home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false);n++)SystemClock.sleep(100);
    if(!home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false))throw new AssertionError("Upgrade import did not finish");
    if(!new java.io.File(home.getFilesDir(),"pre-foundation-layout.json").isFile())throw new AssertionError("Legacy recovery copy is missing");
    if(!home.getSharedPreferences("dot-style",0).getBoolean("dark",false))throw new AssertionError("Legacy dark theme was lost");
    boolean note=false;for(Object value:home.getSharedPreferences("dot-tiles",0).getAll().values())if(String.valueOf(value).contains("Synthetic upgrade note"))note=true;
    if(!note)throw new AssertionError("Independent note state was lost");
    SystemClock.sleep(1000);final int[] count={0};onUi(()->count[0]=widgetCount((android.view.View)invoke(home,"getWorkspace")));
    if(count[0]!=3)throw new AssertionError("Expected three migrated native widgets, found "+count[0]);
    screenshot("foundation-upgrade-home");result.putString("stream","PASS: v3-to-v4 install update, three native widgets including legacy controls, note state, dark appearance and recovery copy\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
