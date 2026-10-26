package dev.dothome.widgets;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.Calendar;
import dev.dothome.ui.*;
import org.json.JSONObject;

/** Per-instance widget editing, with genuine local photo selection and crop controls. */
public final class TileConfiguration extends Activity {
  long id;
  JSONObject item;
  LinearLayout body;
  TileContext adapter;
  PhotoView photo;
  int ink, paper;
  EditText draftText,draftCity,draftTemperature,draftGoal;
  void captureDraft(){
    if(draftText!=null)put(item.optString("type").equals("countdown")?"name":"text",draftText.getText().toString());
    if(draftCity!=null)put("location",draftCity.getText().toString());
    if(draftTemperature!=null)put("temperature",draftTemperature.getText().toString());
    if(draftGoal!=null)try{put("stepGoal",Math.max(100,Math.min(100000,Integer.parseInt(draftGoal.getText().toString()))));}catch(NumberFormatException ignored){}
  }
  final android.window.OnBackAnimationCallback back=new android.window.OnBackAnimationCallback(){public void onBackStarted(android.window.BackEvent event){}public void onBackProgressed(android.window.BackEvent event){if(!DotAppearance.get(TileConfiguration.this).reducedMotion)getWindow().getDecorView().setTranslationX(d(22)*event.getProgress());}public void onBackCancelled(){getWindow().getDecorView().animate().translationX(0).setDuration(150).start();}public void onBackInvoked(){finish();}};
  @Override public void onDestroy(){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back);PhotoCollection.prune(this);super.onDestroy();}
  int d(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
  TextView label(String value,int size) { TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(ink);view.setTypeface(TileTypography.regular(this));view.setIncludeFontPadding(false);view.setGravity(Gravity.CENTER_VERTICAL);return view; }
  TextView action(String value,Runnable run) { TextView view=DotUi.button(this,value,run);view.setGravity(Gravity.CENTER_VERTICAL);body.addView(view,new LinearLayout.LayoutParams(-1,-2));return view; }
  void save() { if(!item.optBoolean("wallpaper"))TileContext.save(this,id,item); }
  void finishEdit() {
    captureDraft();
    if(!item.optBoolean("wallpaper")){if(item.optString("type").equals("weather")&&!item.optString("temperature").isEmpty())put("weatherUpdated",System.currentTimeMillis());save();finish();return;}
    if(photo==null || photo.image==null){Toast.makeText(this,"Wait for the photo to load",Toast.LENGTH_SHORT).show();return;}
    int w=1080,h=(int)(1080f*getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().widthPixels);
    android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
    android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.scale(w/(float)photo.getWidth(),h/(float)photo.getHeight());photo.draw(canvas);
    adapter.worker.execute(()->{
      boolean ok=false;
      try {
        android.app.WallpaperManager.getInstance(this).setBitmap(bitmap,null,true,android.app.WallpaperManager.FLAG_SYSTEM);
        java.io.File temp=new java.io.File(getFilesDir(),"wallpaper-new.jpg");try(var output=new java.io.FileOutputStream(temp)){bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,92,output);}
        java.nio.file.Files.move(temp.toPath(),new java.io.File(getFilesDir(),"wallpaper-local.jpg").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        getSharedPreferences("dot-style",0).edit().putBoolean("wallpaperApplied",true).putString("wallpaper","local").putBoolean("externalWallpaper",false).putInt("ownedWallpaperId",android.app.WallpaperManager.getInstance(this).getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM)).apply();DotAppearance.refreshWallpaper(this);ok=true;
      }catch(Exception ignored){}finally{bitmap.recycle();}
      final boolean applied=ok;runOnUiThread(()->{Toast.makeText(this,applied?"Wallpaper applied":"Wallpaper unavailable",Toast.LENGTH_SHORT).show();if(applied){if(item.optBoolean("motion"))startActivity(new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new android.content.ComponentName(this,dev.dothome.ui.DotAtmosphere.class)));finish();}});
    });
  }
  void put(String key,Object value) { try { item.put(key,value); } catch(Exception ignored) {} }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,back);
    id=getIntent().getLongExtra("itemId",-1);
    try { item=new JSONObject(getIntent().getStringExtra("initial")); } catch(Exception ignored) { item=new JSONObject(); }
    if(state!=null) try { item=new JSONObject(state.getString("item")); } catch(Exception ignored) {}
    getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    getWindow().setDimAmount(.22f);
    adapter=new TileContext(this);
    show();
  }
  @Override public void onSaveInstanceState(Bundle out) { captureDraft();out.putString("item",item.toString());super.onSaveInstanceState(out); }
  void show() {
    captureDraft();draftText=null;draftCity=null;draftTemperature=null;draftGoal=null;
    adapter.sync();var appearance=DotAppearance.get(this);ink=appearance.ink;paper=appearance.background;
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(d(22),d(16),d(22),d(22));body.setBackgroundColor(paper);
    android.graphics.drawable.GradientDrawable sheet=new android.graphics.drawable.GradientDrawable();sheet.setColor(paper);sheet.setCornerRadii(new float[]{d(30),d(30),d(30),d(30),0,0,0,0});body.setBackground(sheet);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);
    getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*(item.optBoolean("wallpaper")?.94f:item.optString("type").equals("clock")?.67f:.82f)));
    body.setOnApplyWindowInsetsListener((view,insets)->{ var bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(d(22),d(12),d(22),bars.bottom+d(18));return insets; });
    String type=item.optString("type");
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
    header.addView(label(item.optBoolean("wallpaper")?"Wallpaper":type.equals("photo")?"Photo collection":type.equals("clock")?"Clock style":type.equals("weather")?"Weather":"Widget style",19),new LinearLayout.LayoutParams(0,d(50),1));
    TextView done=label("Done",15);done.setOnClickListener(v->{if(photo!=null){put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);}finishEdit();});header.addView(done,new LinearLayout.LayoutParams(-2,d(50)));body.addView(header);
    if(type.equals("photo") && !item.optString("uri").isEmpty()) {
      photo=new PhotoView(adapter,item,true);
      var metrics=getResources().getDisplayMetrics();int previewHeight=item.optBoolean("wallpaper")?(int)(metrics.heightPixels*.48f):d(300);
      LinearLayout.LayoutParams preview=new LinearLayout.LayoutParams(item.optBoolean("wallpaper")?Math.round(previewHeight*metrics.widthPixels/(float)metrics.heightPixels):-1,previewHeight);preview.gravity=Gravity.CENTER_HORIZONTAL;
      body.addView(photo,preview);
      body.addView(label("Drag to position · pinch to zoom",12));
      action("Apply crop",()->{put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);finishEdit();});
    } else body.addView(new TileArtwork(adapter,item,true),new LinearLayout.LayoutParams(-1,d(type.equals("clock")?155:190)));
    if(item.optBoolean("wallpaper")) {
      LinearLayout effects=new LinearLayout(this);String[] keys={"none","glass","atmosphere"},names={"Original","Glass","Atmosphere"};
      for(int n=0;n<keys.length;n++){final String key=keys[n];TextView choice=DotUi.button(this,names[n],()->{if(photo!=null){put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);}put("wallpaperEffect",key);photo=null;show();});if(item.optString("wallpaperEffect","none").equals(key))choice.setBackground(DotUi.shape(this,appearance.surface,24));effects.addView(choice,new LinearLayout.LayoutParams(0,-2,1));}body.addView(effects);
      action("Use Atmosphere motion",()->{put("motion",true);finishEdit();});
    }
    if(type.equals("photo") && !item.optBoolean("wallpaper")) {
      action("Add photos",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),31));
      org.json.JSONArray images=item.optJSONArray("images");
      int count=images==null?(item.optString("uri").isEmpty()?0:1):images.length();
      if(count>0) {
        body.addView(label("Swipe along the bottom of a Home photo to change it.",12));
        body.addView(label("Photo "+(item.optInt("imageIndex")+1)+" of "+count,12));
        action("Next photo",()->{PhotoCollection.cycle(item,1);photo=null;show();});
        action("Move photo earlier",()->{PhotoCollection.move(item,-1);photo=null;show();});
        action("Move photo later",()->{PhotoCollection.move(item,1);photo=null;show();});
        action("Remove this photo",()->{PhotoCollection.remove(item);photo=null;show();});
        action(item.optBoolean("rotateOnUnlock")?"Change on unlock · On":"Change on unlock · Off",()->{put("rotateOnUnlock",!item.optBoolean("rotateOnUnlock"));show();});
        if(item.optInt("style")==2){
          action(item.optBoolean("stickerOutline",true)?"White outline · On":"White outline · Off",()->{put("stickerOutline",!item.optBoolean("stickerOutline",true));photo=null;show();});
          action("Remove plain background",()->{
            android.widget.Toast.makeText(this,"Removing edge-connected background…",android.widget.Toast.LENGTH_SHORT).show();
            adapter.worker.execute(()->{try {android.net.Uri result=PhotoCollection.removePlainBackground(this,android.net.Uri.parse(PhotoCollection.uri(item)),35);runOnUiThread(()->{if(isDestroyed())return;org.json.JSONArray list=item.optJSONArray("images");if(list==null)list=new org.json.JSONArray().put(result.toString());else try{list.put(item.optInt("imageIndex"),result.toString());}catch(Exception ignored){}put("images",list);put("uri",result.toString());photo=null;show();});}catch(Exception unavailable){runOnUiThread(()->android.widget.Toast.makeText(this,"Photo unavailable",android.widget.Toast.LENGTH_SHORT).show());}});
          });
          body.addView(label("Background removal works best with a plain edge background.",12));
        }
      }
      LinearLayout shapes=new LinearLayout(this);
      String[] names={"Rectangle","Circle","Sticker"};
      for(int n=0;n<names.length;n++) {final int variant=n;TextView choice=label(names[n],13);choice.setGravity(Gravity.CENTER);shapes.addView(choice,new LinearLayout.LayoutParams(0,d(48),1));choice.setOnClickListener(v->{put("style",variant);photo=null;show();});}
      body.addView(shapes);
    } else if(type.equals("weather")) {
      EditText city=new EditText(this);draftCity=city;city.setText(item.optString("location"));city.setHint("Place");city.setTextColor(ink);body.addView(city);
      EditText temperature=new EditText(this);draftTemperature=temperature;temperature.setText(item.optString("temperature"));temperature.setHint("Temperature");temperature.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);temperature.setTextColor(ink);body.addView(temperature);
      String[] conditions={"Sunny","Cloudy","Rain","Snow","Clear night"};
      for(String condition:conditions)action(condition+(item.optString("condition").equals(condition)?" · Selected":""),()->{put("condition",condition);show();});
      action("Save local weather",()->{put("location",city.getText().toString());put("temperature",temperature.getText().toString());put("weatherUpdated",System.currentTimeMillis());finishEdit();});
      body.addView(label("Entered manually. This app does not fetch weather.",12));
    } else if(type.equals("steps")) {
      body.addView(label("Measured steps today",17));
      EditText goal=new EditText(this);draftGoal=goal;goal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);goal.setText(String.valueOf(item.optInt("stepGoal",6000)));goal.setTextColor(ink);goal.setHint("Step goal");body.addView(goal);
      TextView note=label("Counts from the first sensor reading today. The next reading includes steps taken while Home is hidden on the same boot. Steps before first use or across a reboot cannot be reconstructed. No background service runs.",13);note.setPadding(0,d(14),0,d(14));body.addView(note);
      if(getSystemService(android.hardware.SensorManager.class).getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER)==null)body.addView(label("Step sensor unavailable on this device",14));
      else if(checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)action("Allow activity recognition",()->requestPermissions(new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},23));
    } else if(type.equals("compass")) {
      TextView note=label("Move the phone in a figure eight if the direction looks wrong. Keep it away from magnets and metal. This shows magnetic heading; no location access is used.",14);body.addView(note);
    } else if(type.equals("notes")) {
      EditText input=new EditText(this);draftText=input;input.setText(item.optString("text"));input.setTextColor(ink);input.setTypeface(TileTypography.regular(this));input.setHint("Write a note");body.addView(input,new LinearLayout.LayoutParams(-1,d(120)));
      action("Save note",()->{put("text",input.getText().toString());save();finish();});
    } else if(type.equals("timer")) {
      action(item.optLong("timerStart")>0?"Pause":"Start",()->{
        long started=item.optLong("timerStart");if(started>0) { put("elapsed",item.optLong("elapsed")+Math.max(0,System.currentTimeMillis()-started));item.remove("timerStart"); } else put("timerStart",System.currentTimeMillis());save();finish();
      });
      action("Reset",()->{put("elapsed",0);item.remove("timerStart");save();finish();});
      action("Set a ringing timer in Clock",()->{startActivity(new Intent(android.provider.AlarmClock.ACTION_SET_TIMER));finish();});
    } else if(type.equals("countdown")) {
      EditText name=new EditText(this);draftText=name;name.setText(item.optString("name","A day to remember"));name.setTextColor(ink);name.setTypeface(TileTypography.regular(this));body.addView(name,new LinearLayout.LayoutParams(-1,d(54)));
      action("Choose a date",()->{
        Calendar day=Calendar.getInstance();if(item.optLong("deadline")>0)day.setTimeInMillis(item.optLong("deadline"));new DatePickerDialog(this,(picker,y,m,d)->{day.set(y,m,d,0,0,0);day.set(Calendar.MILLISECOND,0);put("deadline",day.getTimeInMillis());put("name",name.getText().toString());save();finish();},day.get(Calendar.YEAR),day.get(Calendar.MONTH),day.get(Calendar.DAY_OF_MONTH)).show();
      });
    }
    if(type.equals("clock")) {
      body.addView(label("Clock face",14));
      HorizontalScrollView faces=new HorizontalScrollView(this);faces.setHorizontalScrollBarEnabled(false);
      LinearLayout choices=new LinearLayout(this);String[] names={"Dot matrix","Minimal","Serif","Dial","Digital"};
      for(int n=0;n<5;n++) {
        final int face=n;JSONObject sample;
        try {sample=new JSONObject(item.toString());sample.put("style",n);}catch(Exception error){sample=new JSONObject();}
        LinearLayout option=new LinearLayout(this);option.setOrientation(1);option.setPadding(d(6),d(6),d(6),d(6));
        android.graphics.drawable.GradientDrawable outline=new android.graphics.drawable.GradientDrawable();outline.setColor(Color.TRANSPARENT);outline.setCornerRadius(d(18));if(item.optInt("style")==n)outline.setStroke(d(2),ink);option.setBackground(outline);
        option.addView(new TileArtwork(adapter,sample,true),new LinearLayout.LayoutParams(d(112),d(104)));
        TextView caption=label(names[n],12);caption.setGravity(Gravity.CENTER);option.addView(caption,new LinearLayout.LayoutParams(-1,d(28)));
        option.setOnClickListener(v->{put("style",face);show();});choices.addView(option);
      }
      faces.addView(choices);body.addView(faces);
    }
    if(!type.equals("photo")) {
    LinearLayout tones=new LinearLayout(this);int[] colors={paper,0xffeeeede,0xff181a1c,0xffb7ca9c,0xffcc292d};
    for(int n=0;n<colors.length;n++) {final int tone=n;View swatch=new View(this);android.graphics.drawable.GradientDrawable chip=new android.graphics.drawable.GradientDrawable();chip.setColor(colors[n]);chip.setShape(1);chip.setStroke(d(item.optInt("tone")==n?2:1),ink);swatch.setBackground(chip);swatch.setContentDescription(new String[]{"Default","Ivory","Charcoal","Sage","Red"}[n]+" widget colour");swatch.setOnClickListener(v->{put("tone",tone);show();});tones.addView(swatch,new LinearLayout.LayoutParams(d(36),d(36)){{setMargins(d(8),d(14),d(8),d(14));}});}
    body.addView(tones);
    }
    if(!type.equals("photo") && !type.equals("clock")) action(item.optInt("style")==1?"Use light surface":"Invert surface",()->{put("style",item.optInt("style")==1?0:1);show();});
    action("Cancel",this::finish);
  }
  @Override public void onActivityResult(int request,int result,Intent data) {
    super.onActivityResult(request,result,data);
    if(request==31 && result==RESULT_OK && data!=null) {
      java.util.ArrayList<android.net.Uri> selected=new java.util.ArrayList<>();
      if(data.getClipData()!=null)for(int n=0;n<Math.min(PhotoCollection.MAX_IMAGES,data.getClipData().getItemCount());n++)selected.add(data.getClipData().getItemAt(n).getUri());
      else if(data.getData()!=null)selected.add(data.getData());
      org.json.JSONArray current=item.optJSONArray("images");
      if(current==null){current=new org.json.JSONArray();if(!item.optString("uri").isEmpty())current.put(item.optString("uri"));}
      final org.json.JSONArray images=current;int room=PhotoCollection.MAX_IMAGES-images.length();if(selected.size()>room)selected=new java.util.ArrayList<>(selected.subList(0,Math.max(0,room)));
      final java.util.ArrayList<android.net.Uri> sources=selected;
      adapter.worker.execute(()->{
        for(android.net.Uri source:sources)try{images.put(PhotoCollection.importImage(this,source).toString());}catch(Exception unavailable){}
        runOnUiThread(()->{if(isDestroyed())return;put("images",images);if(images.length()>0){put("imageIndex",images.length()-1);put("uri",images.optString(images.length()-1));}photo=null;show();});
      });
    }
  }
}
