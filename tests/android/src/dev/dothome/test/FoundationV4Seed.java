package dev.dothome.test;
import android.app.*;
import android.os.*;
/** Synthetic fixture only. No user data or private emulator state is published. */
public final class FoundationV4Seed extends FoundationV4Upgrade {
  @Override public void onStart(){Bundle result=new Bundle();try{
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);launchHomeAsync();SystemClock.sleep(1200);chooseDefaultHome();homeKey();
    for(int n=0;n<60&&!home.getSharedPreferences("dot-style",0).getBoolean("legacyImported",false);n++)SystemClock.sleep(100);SystemClock.sleep(1500);
    batchMove();homeKey();home.getSharedPreferences("dot-style",0).edit().putBoolean("dark",true).putString("material","translucent").commit();
    java.nio.file.Files.writeString(snapshotFile().toPath(),nativeSnapshot().toString());
    result.putString("stream","PASS: synthetic v4 native fixture with app group/folder, default widgets and appearance saved.\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
