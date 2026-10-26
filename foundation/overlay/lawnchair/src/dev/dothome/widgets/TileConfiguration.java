package dev.dothome.widgets;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.Calendar;
import org.json.JSONObject;

/** Per-instance widget editing, with genuine local photo selection and crop controls. */
public final class TileConfiguration extends Activity {
  long id;
  JSONObject item;
  LinearLayout body;
  TileContext adapter;
  PhotoView photo;
  int ink, paper;
  int d(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
  TextView label(String value,int size) { TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(ink);view.setTypeface(TileTypography.regular(this));view.setGravity(Gravity.CENTER_VERTICAL);return view; }
  TextView action(String value,Runnable run) { TextView view=label(value,15);view.setPadding(d(16),0,d(16),0);view.setOnClickListener(v->run.run());body.addView(view,new LinearLayout.LayoutParams(-1,d(54)));return view; }
  void save() { if(!item.optBoolean("wallpaper"))TileContext.save(this,id,item); }
  void finishEdit() {
    if(!item.optBoolean("wallpaper")){save();finish();return;}
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
        getSharedPreferences("dot-style",0).edit().putBoolean("wallpaperApplied",true).putString("wallpaper","local").apply();ok=true;
      }catch(Exception ignored){}finally{bitmap.recycle();}
      final boolean applied=ok;runOnUiThread(()->{Toast.makeText(this,applied?"Wallpaper applied":"Wallpaper unavailable",Toast.LENGTH_SHORT).show();if(applied)finish();});
    });
  }
  void put(String key,Object value) { try { item.put(key,value); } catch(Exception ignored) {} }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    id=getIntent().getLongExtra("itemId",-1);
    try { item=new JSONObject(getIntent().getStringExtra("initial")); } catch(Exception ignored) { item=new JSONObject(); }
    if(state!=null) try { item=new JSONObject(state.getString("item")); } catch(Exception ignored) {}
    getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    getWindow().setDimAmount(.22f);
    adapter=new TileContext(this);
    show();
  }
  @Override public void onSaveInstanceState(Bundle out) { out.putString("item",item.toString());super.onSaveInstanceState(out); }
  void show() {
    ink=adapter.dark()?0xfff2f2ed:0xff181918;paper=adapter.dark()?0xff171817:0xfff7f7f3;
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(d(22),d(16),d(22),d(22));body.setBackgroundColor(paper);
    android.graphics.drawable.GradientDrawable sheet=new android.graphics.drawable.GradientDrawable();sheet.setColor(paper);sheet.setCornerRadii(new float[]{d(30),d(30),d(30),d(30),0,0,0,0});body.setBackground(sheet);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);
    getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.78f));
    body.setOnApplyWindowInsetsListener((view,insets)->{ var bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(d(22),d(12),d(22),bars.bottom+d(18));return insets; });
    String type=item.optString("type");
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
    header.addView(label(item.optBoolean("wallpaper")?"Wallpaper":type.equals("photo")?"Photos":"Widget style",19),new LinearLayout.LayoutParams(0,d(50),1));
    TextView done=label("Done",15);done.setOnClickListener(v->{if(photo!=null){put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);}finishEdit();});header.addView(done,new LinearLayout.LayoutParams(-2,d(50)));body.addView(header);
    if(type.equals("photo") && !item.optString("uri").isEmpty()) {
      photo=new PhotoView(adapter,item,true);
      var metrics=getResources().getDisplayMetrics();int previewHeight=item.optBoolean("wallpaper")?(int)(metrics.heightPixels*.48f):d(300);
      LinearLayout.LayoutParams preview=new LinearLayout.LayoutParams(item.optBoolean("wallpaper")?Math.round(previewHeight*metrics.widthPixels/(float)metrics.heightPixels):-1,previewHeight);preview.gravity=Gravity.CENTER_HORIZONTAL;
      body.addView(photo,preview);
      body.addView(label("Drag to position · pinch to zoom",12));
      action("Apply crop",()->{put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);finishEdit();});
    } else body.addView(new TileArtwork(adapter,item,true),new LinearLayout.LayoutParams(-1,d(210)));
    if(type.equals("photo") && !item.optBoolean("wallpaper")) {
      action("Choose a photo",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),31));
      LinearLayout shapes=new LinearLayout(this);
      String[] names={"Rectangle","Circle","Sticker"};
      for(int n=0;n<names.length;n++) {final int variant=n;TextView choice=label(names[n],13);choice.setGravity(Gravity.CENTER);shapes.addView(choice,new LinearLayout.LayoutParams(0,d(48),1));choice.setOnClickListener(v->{put("style",variant);photo=null;show();});}
      body.addView(shapes);
    } else if(type.equals("notes")) {
      EditText input=new EditText(this);input.setText(item.optString("text"));input.setTextColor(ink);input.setTypeface(TileTypography.regular(this));input.setHint("Write a note");body.addView(input,new LinearLayout.LayoutParams(-1,d(120)));
      action("Save note",()->{put("text",input.getText().toString());save();finish();});
    } else if(type.equals("timer")) {
      action(item.optLong("timerStart")>0?"Pause":"Start",()->{
        long started=item.optLong("timerStart");if(started>0) { put("elapsed",item.optLong("elapsed")+Math.max(0,System.currentTimeMillis()-started));item.remove("timerStart"); } else put("timerStart",System.currentTimeMillis());save();finish();
      });
      action("Reset",()->{put("elapsed",0);item.remove("timerStart");save();finish();});
      action("Set a ringing timer in Clock",()->{startActivity(new Intent(android.provider.AlarmClock.ACTION_SET_TIMER));finish();});
    } else if(type.equals("countdown")) {
      EditText name=new EditText(this);name.setText(item.optString("name","A day to remember"));name.setTextColor(ink);name.setTypeface(TileTypography.regular(this));body.addView(name,new LinearLayout.LayoutParams(-1,d(54)));
      action("Choose a date",()->{
        Calendar day=Calendar.getInstance();new DatePickerDialog(this,(picker,y,m,d)->{day.set(y,m,d,0,0,0);day.set(Calendar.MILLISECOND,0);put("deadline",day.getTimeInMillis());put("name",name.getText().toString());save();finish();},day.get(Calendar.YEAR),day.get(Calendar.MONTH),day.get(Calendar.DAY_OF_MONTH)).show();
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
    if(request==31 && result==RESULT_OK && data!=null && data.getData()!=null) {
      try { getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(SecurityException ignored) {}
      put("uri",data.getData().toString());photo=null;show();
    }
  }
}
