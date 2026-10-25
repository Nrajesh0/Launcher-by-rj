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
  void save() { TileContext.save(this,id,item); }
  void put(String key,Object value) { try { item.put(key,value); } catch(Exception ignored) {} }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    id=getIntent().getLongExtra("itemId",-1);
    try { item=new JSONObject(getIntent().getStringExtra("initial")); } catch(Exception ignored) { item=new JSONObject(); }
    if(state!=null) try { item=new JSONObject(state.getString("item")); } catch(Exception ignored) {}
    adapter=new TileContext(this);
    show();
  }
  @Override public void onSaveInstanceState(Bundle out) { out.putString("item",item.toString());super.onSaveInstanceState(out); }
  void show() {
    ink=adapter.dark()?0xfff2f2ed:0xff181918;paper=adapter.dark()?0xff171817:0xfff7f7f3;
    body=new LinearLayout(this);body.setOrientation(1);body.setPadding(d(22),d(16),d(22),d(22));body.setBackgroundColor(paper);
    setContentView(body);
    body.setOnApplyWindowInsetsListener((view,insets)->{ var bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(d(22),bars.top+d(16),d(22),bars.bottom+d(18));return insets; });
    String type=item.optString("type");
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
    header.addView(label(type.equals("photo")?"Photos":"Widget style",19),new LinearLayout.LayoutParams(0,d(50),1));
    TextView done=label("Done",15);done.setOnClickListener(v->{save();finish();});header.addView(done,new LinearLayout.LayoutParams(-2,d(50)));body.addView(header);
    if(type.equals("photo") && !item.optString("uri").isEmpty()) {
      photo=new PhotoView(adapter,item,true);body.addView(photo,new LinearLayout.LayoutParams(-1,0,1));
      body.addView(label("Drag to position · pinch to zoom",12));
      action("Apply crop",()->{put("cropX",photo.cropX);put("cropY",photo.cropY);put("cropScale",photo.scale);save();finish();});
    } else body.addView(new TileArtwork(adapter,item,true),new LinearLayout.LayoutParams(-1,d(210)));
    if(type.equals("photo")) {
      action("Choose a photo",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),31));
      LinearLayout shapes=new LinearLayout(this);
      String[] names={"Rectangle","Circle","Sticker"};
      for(int n=0;n<names.length;n++) {final int variant=n;TextView choice=label(names[n],13);choice.setGravity(Gravity.CENTER);shapes.addView(choice,new LinearLayout.LayoutParams(0,d(48),1));choice.setOnClickListener(v->{put("style",variant);save();show();});}
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
    if(!type.equals("photo")) action(item.optInt("style")==1?"Use light surface":"Invert surface",()->{put("style",item.optInt("style")==1?0:1);save();show();});
    action("Cancel",this::finish);
  }
  @Override public void onActivityResult(int request,int result,Intent data) {
    super.onActivityResult(request,result,data);
    if(request==31 && result==RESULT_OK && data!=null && data.getData()!=null) {
      try { getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(SecurityException ignored) {}
      put("uri",data.getData().toString());save();show();
    }
  }
}
