package dev.dothome.widgets;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import dev.dothome.ui.*;
import org.json.JSONObject;
import java.util.*;

/** Actual local history and guided sessions; permissions are an empty-state action. */
public final class TileDetails extends Activity {
  private long id;
  private JSONObject item;
  private String type;
  private LinearLayout body;
  private boolean resumed;
  private int generation,lastPhase=-1;
  private final android.window.OnBackAnimationCallback back=new android.window.OnBackAnimationCallback(){
    public void onBackStarted(android.window.BackEvent e){}
    public void onBackProgressed(android.window.BackEvent e){if(!DotAppearance.get(TileDetails.this).reducedMotion)getWindow().getDecorView().setTranslationX(DotUi.dp(TileDetails.this,22)*e.getProgress());}
    public void onBackCancelled(){getWindow().getDecorView().animate().translationX(0).setDuration(150).start();}
    public void onBackInvoked(){finish();}
  };
  @Override public void onCreate(Bundle state){super.onCreate(state);id=getIntent().getLongExtra("itemId",-1);try{item=new JSONObject(getIntent().getStringExtra("initial"));}catch(Exception invalid){item=new JSONObject();}type=item.optString("type","away");getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,back);}
  @Override protected void onResume(){super.onResume();resumed=true;show();}
  @Override protected void onPause(){resumed=false;super.onPause();}
  @Override public void onDestroy(){generation++;getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back);super.onDestroy();}
  private int d(float value){return DotUi.dp(this,value);}
  private void put(String key,Object value){try{item.put(key,value);}catch(Exception ignored){}}
  private void save(){TileContext.save(this,id,item);}
  private void row(String title,String value){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(d(18),d(16),d(18),d(16));line.setBackground(DotUi.shape(this,DotAppearance.get(this).card,18));TextView a=DotUi.text(this,title,14);line.addView(a,new LinearLayout.LayoutParams(0,-2,1));line.addView(DotUi.text(this,value,14));body.addView(line,new LinearLayout.LayoutParams(-1,-2));}
  private void action(String label,Runnable run){body.addView(DotUi.button(this,label,run),new LinearLayout.LayoutParams(-1,-2));}
  private boolean usageAllowed(){return getSystemService(AppOpsManager.class).unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;}
  private void show(){
    String title=type.equals("breathing")?"Breathing break":type.equals("away")?"Away time":"Screen time";
    LinearLayout root=DotUi.page(this,title,this::finish);ScrollView scroll=new ScrollView(this);scroll.setClipToPadding(false);body=DotUi.column(this);body.setPadding(0,d(14),0,d(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));DotUi.enter(body);
    if(type.equals("breathing")){breathing();return;}
    if(!usageAllowed()){
      TextView explanation=DotUi.text(this,type.equals("away")?"See your screen-off breaks and weekly patterns.":"See your daily app usage and the time you spend on your phone.",17);explanation.setPadding(d(12),d(22),d(12),d(28));body.addView(explanation);
      action("Allow usage history",()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));return;
    }
    TextView loading=DotUi.text(this,"Loading local history…",14);body.addView(loading);final int current=++generation;
    TileContext.WORK.execute(()->{
      long now=System.currentTimeMillis();AwayTimeStats history=AwayTimeStats.read(this,now);
      Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
      ArrayList<UsageStats> apps=new ArrayList<>(getSystemService(UsageStatsManager.class).queryAndAggregateUsageStats(start.getTimeInMillis(),now).values());apps.removeIf(a->a.getTotalTimeInForeground()<=0);apps.sort((a,b)->Long.compare(b.getTotalTimeInForeground(),a.getTotalTimeInForeground()));
      runOnUiThread(()->{if(isDestroyed()||current!=generation)return;body.removeAllViews();if(type.equals("away"))away(history);else usage(apps);});
    });
  }
  private void away(AwayTimeStats history){
    long total=0;for(long day:history.daily)total+=day;
    TextView amount=DotUi.text(this,history.available?AwayTimeStats.duration(total):"No history yet",46);amount.setTypeface(TileTypography.display(this));amount.setPadding(d(14),d(12),0,d(4));body.addView(amount);
    TextView caption=DotUi.text(this,"Weekly screen-off time",14);caption.setTextColor(DotAppearance.get(this).secondary);caption.setPadding(d(14),0,0,d(24));body.addView(caption);
    body.addView(new HistoryChart(history.daily),new LinearLayout.LayoutParams(-1,d(240)));
    row("Longest break",AwayTimeStats.duration(history.longest));row("Today",AwayTimeStats.duration(history.daily[6]));row("Daily average",AwayTimeStats.duration(total/7));
    TextView note=DotUi.text(this,"Calculated from the screen-off events available on this device. Screen-off time is not proof that the phone was put down.",12);note.setTextColor(DotAppearance.get(this).secondary);note.setPadding(d(14),d(20),d(14),d(12));body.addView(note);
  }
  private void usage(ArrayList<UsageStats> apps){
    long total=0;for(UsageStats app:apps)total+=app.getTotalTimeInForeground();TextView amount=DotUi.text(this,AwayTimeStats.duration(total),46);amount.setTypeface(TileTypography.display(this));amount.setPadding(d(14),d(14),0,d(24));body.addView(amount);
    if(apps.isEmpty())row("Today","No usage recorded");
    for(UsageStats app:apps.subList(0,Math.min(30,apps.size()))){String name;try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(app.getPackageName(),0)).toString();}catch(Exception unavailable){name=app.getPackageName();}row(name,AwayTimeStats.duration(app.getTotalTimeInForeground()));}
    TextView note=DotUi.text(this,"App foreground totals reported by Android may overlap during multitasking.",12);note.setPadding(d(14),d(20),d(14),d(12));body.addView(note);
  }
  private void breathing(){
    int mode=Math.max(0,Math.min(2,item.optInt("breathMode")));LinearLayout modes=new LinearLayout(this);
    for(int n=0;n<3;n++){final int selected=n;TextView choice=DotUi.button(this,BreathingSession.NAMES[n],()->{pause();put("breathMode",selected);put("breathElapsed",0);save();show();});if(n==mode)choice.setBackground(DotUi.shape(this,DotAppearance.get(this).surface,28));modes.addView(choice,new LinearLayout.LayoutParams(0,-2,1));}body.addView(modes);
    body.addView(new BreathCanvas(),new LinearLayout.LayoutParams(-1,d(360)));
    action(item.optLong("breathStarted")>0?"Pause":"Start",()->{if(item.optLong("breathStarted")>0)pause();else put("breathStarted",System.currentTimeMillis());save();show();});
    action("Reset",()->{put("breathStarted",0);put("breathElapsed",0);save();show();});
    action(item.optBoolean("breathHaptics",true)?"Haptic guidance · On":"Haptic guidance · Off",()->{put("breathHaptics",!item.optBoolean("breathHaptics",true));save();show();});
  }
  private void pause(){long started=item.optLong("breathStarted");if(started>0)put("breathElapsed",item.optLong("breathElapsed")+Math.max(0,System.currentTimeMillis()-started));put("breathStarted",0);}
  private final class BreathCanvas extends View {
    final Paint p=new Paint(3);BreathCanvas(){super(TileDetails.this);setContentDescription("Breathing guide");}
    @Override protected void onDraw(Canvas c){var a=DotAppearance.get(getContext());BreathingSession session=new BreathingSession(item,System.currentTimeMillis());float cx=getWidth()/2f,cy=getHeight()*.43f,r=Math.min(getWidth(),getHeight())*(.20f+.12f*(a.reducedMotion?.5f:session.expansion));p.setColor(a.surface);c.drawCircle(cx,cy,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(.8f));p.setColor(a.ink);for(int n=1;n<10;n++){RectF oval=new RectF(cx-r*n/10,cy-r,cx+r*n/10,cy+r);c.drawOval(oval,p);}p.setStyle(Paint.Style.FILL);p.setTypeface(TileTypography.regular(getContext()));p.setTextSize(d(22));p.setTextAlign(Paint.Align.CENTER);c.drawText(item.optLong("breathStarted")>0?session.label:"Ready when you are",cx,getHeight()*.88f,p);
      if(resumed&&item.optLong("breathStarted")>0){if(lastPhase!=session.phase){lastPhase=session.phase;if(item.optBoolean("breathHaptics",true)&&!a.reducedMotion){Vibrator vibrator=getSystemService(Vibrator.class);if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(VibrationEffect.createOneShot(32,VibrationEffect.DEFAULT_AMPLITUDE));}}if(a.reducedMotion)postInvalidateDelayed(1000);else postInvalidateOnAnimation();}
    }
  }
  private final class HistoryChart extends View {
    final long[] values;final Paint p=new Paint(3);HistoryChart(long[] values){super(TileDetails.this);this.values=values;setContentDescription("Seven days of screen-off time");}
    @Override protected void onDraw(Canvas c){var a=DotAppearance.get(getContext());float pad=d(14),cell=(getWidth()-2*pad)/7;long max=1;for(long v:values)max=Math.max(max,v);Calendar day=Calendar.getInstance();day.add(Calendar.DAY_OF_YEAR,-6);p.setTypeface(TileTypography.regular(getContext()));p.setTextSize(d(11));p.setTextAlign(Paint.Align.CENTER);for(int n=0;n<7;n++){float x=pad+cell*(n+.5f),height=(getHeight()-d(55))*values[n]/max;p.setColor(n==6?a.accent:DotAppearance.alpha(a.accent,140));c.drawRoundRect(new RectF(x-cell*.22f,getHeight()-d(32)-Math.max(d(2),height),x+cell*.22f,getHeight()-d(32)),d(8),d(8),p);p.setColor(a.secondary);c.drawText(new java.text.SimpleDateFormat("EE",Locale.getDefault()).format(day.getTime()),x,getHeight()-d(8),p);day.add(Calendar.DAY_OF_YEAR,1);}}
  }
}
