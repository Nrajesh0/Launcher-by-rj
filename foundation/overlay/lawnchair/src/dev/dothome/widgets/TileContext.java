package dev.dothome.widgets;

import android.Manifest;
import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.CalendarContract;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Local data adapter for native custom widgets; no polling service or network client. */
final class TileContext extends ContextWrapper {
  static final ExecutorService WORK = Executors.newFixedThreadPool(2);
  static volatile long refreshed;
  static volatile AwayTimeStats history = AwayTimeStats.unavailable();
  static volatile String nextEvent = "Connect calendar", totalUsage = "Enable history";
  final ExecutorService worker = WORK;
  final JSONObject settings = new JSONObject();
  boolean foreground;
  int card=0xffeeeeea, bg=0xfff4f4ef;
  int page, battery, accent = 0xff98b9a5, muted = 0xff6a6b64;
  boolean charging, torch, dnd;
  int ringer=2;
  float heading;
  int sensorAccuracy=-1,steps=-1;
  boolean compassAvailable=true;
  AwayTimeStats away = AwayTimeStats.unavailable();
  String agenda = "Connect calendar", usage = "Enable history";

  TileContext(Context context) { super(context); sync(); }

  boolean dark() { return getSharedPreferences("dot-style", 0).getBoolean("dark", false); }
  int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
  Drawable scaledImage(Uri uri) throws Exception {
    return ImageDecoder.decodeDrawable(ImageDecoder.createSource(getContentResolver(), uri), (decoder, info, source) -> {
      decoder.setTargetSampleSize(Math.max(1, Math.max(info.getSize().getWidth(), info.getSize().getHeight()) / 1600));
      decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
    });
  }
  void sync() {
    var prefs = getSharedPreferences("dot-style", 0);
    try {
      settings.put("material", prefs.getString("material", "classic"));
      settings.put("reduceMotion", prefs.getBoolean("reduceMotion", false));
    } catch (Exception ignored) {}
    var appearance=dev.dothome.ui.DotAppearance.get(this);
    accent=appearance.accent;muted=appearance.secondary;card=appearance.card;bg=appearance.background;
    try {settings.put("reduceMotion",appearance.reducedMotion);}catch(Exception ignored){}
    Intent state = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if(state != null) {
      battery = state.getIntExtra(BatteryManager.EXTRA_LEVEL,0)*100/Math.max(1,state.getIntExtra(BatteryManager.EXTRA_SCALE,100));
      int status = state.getIntExtra(BatteryManager.EXTRA_STATUS,0);
      charging = status==BatteryManager.BATTERY_STATUS_CHARGING || status==BatteryManager.BATTERY_STATUS_FULL;
    }
    away=history; agenda=nextEvent; usage=totalUsage;
    dnd=getSystemService(NotificationManager.class).getCurrentInterruptionFilter()!=NotificationManager.INTERRUPTION_FILTER_ALL;
    ringer=getSystemService(android.media.AudioManager.class).getRingerMode();
    var stepPrefs=getSharedPreferences("dot-steps",0);if(java.time.LocalDate.now().toString().equals(stepPrefs.getString("day","")))steps=stepPrefs.getInt("count",-1);
    long now=SystemClock.elapsedRealtime();
    if(now-refreshed<30000) return;
    refreshed=now;
    Context app=getApplicationContext();
    WORK.execute(() -> {
      if(app.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED) {
        Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();
        long time=System.currentTimeMillis();
        ContentUris.appendId(uri,time); ContentUris.appendId(uri,time+7*86400000L);
        try(Cursor cursor=app.getContentResolver().query(uri.build(),new String[]{CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN},null,null,CalendarContract.Instances.BEGIN+" ASC")) {
          nextEvent=cursor!=null && cursor.moveToFirst() ? new java.text.SimpleDateFormat("EEE HH:mm",Locale.getDefault()).format(new Date(cursor.getLong(1)))+"\n"+cursor.getString(0) : "No upcoming events";
        } catch(Exception unavailable) { nextEvent="Calendar unavailable"; }
      } else nextEvent="Connect calendar";
      try {
        AppOpsManager ops=app.getSystemService(AppOpsManager.class);
        if(ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), app.getPackageName())==AppOpsManager.MODE_ALLOWED) {
          long end=System.currentTimeMillis(); Calendar day=Calendar.getInstance(); day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
          long total=0; for(UsageStats entry:app.getSystemService(UsageStatsManager.class).queryAndAggregateUsageStats(day.getTimeInMillis(),end).values()) total+=entry.getTotalTimeInForeground();
          totalUsage=AwayTimeStats.duration(total); history=AwayTimeStats.read(app,end);
        } else { history=AwayTimeStats.unavailable(); totalUsage="Enable history"; }
      } catch(Exception unavailable) { history=AwayTimeStats.unavailable(); totalUsage="History unavailable"; }
    });
  }

  static JSONObject load(Context context, long id, String type, int variant) {
    try {
      String saved=context.getSharedPreferences("dot-tiles",0).getString("item-"+id,null);
      if(saved!=null) return new JSONObject(saved);
      return new JSONObject().put("type",type).put("style",variant).put("w",2).put("h",2).put("page",0);
    } catch(Exception invalid) { return new JSONObject(); }
  }
  static void save(Context context,long id,JSONObject item) { context.getSharedPreferences("dot-tiles",0).edit().putString("item-"+id,item.toString()).apply(); }
}
