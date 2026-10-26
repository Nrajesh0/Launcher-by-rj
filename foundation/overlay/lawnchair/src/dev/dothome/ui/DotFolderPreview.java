package dev.dothome.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.model.data.*;

/** Enlarged folders expose individual apps while the label/overflow opens the folder. */
public final class DotFolderPreview {
  public static boolean covered(View view,FolderInfo info) {return !view.getContext().getSharedPreferences("dot-folders",0).getString("cover-"+info.id,"").isEmpty();}
  public static void choose(android.content.Context context,FolderInfo info,View view) {
    android.app.Dialog dialog=new android.app.Dialog(context);
    android.widget.LinearLayout body=DotUi.column(context);body.setPadding(DotUi.dp(context,18),DotUi.dp(context,18),DotUi.dp(context,18),DotUi.dp(context,18));body.setBackground(DotUi.shape(context,DotAppearance.get(context).background,28));body.addView(DotUi.text(context,"Folder style",19));
    android.widget.GridLayout grid=new android.widget.GridLayout(context);grid.setColumnCount(3);
    String[] names={"App previews","Dot grid","Flower","Orbit","Cross","Heart"};
    for(int n=0;n<names.length;n++){final String cover=n==0?"":names[n];android.widget.LinearLayout choice=DotUi.column(context);choice.setGravity(android.view.Gravity.CENTER);choice.setPadding(0,DotUi.dp(context,10),0,DotUi.dp(context,8));choice.setBackground(DotUi.clickable(context,DotAppearance.get(context).card,18));
      choice.addView(new View(context){final Paint paint=new Paint(3);@Override protected void onDraw(Canvas canvas){paint.setColor(DotAppearance.get(context).ink);if(cover.isEmpty()){for(int y=0;y<3;y++)for(int x=0;x<3;x++)canvas.drawCircle(getWidth()*(.3f+x*.2f),getHeight()*(.3f+y*.2f),DotUi.dp(context,3),paint);}else cover(canvas,cover,getWidth()/2f,getHeight()/2f,getWidth()*.30f,paint);}},new android.widget.LinearLayout.LayoutParams(DotUi.dp(context,66),DotUi.dp(context,66)));
      android.widget.TextView caption=DotUi.text(context,names[n],12);caption.setGravity(android.view.Gravity.CENTER);choice.addView(caption);choice.setOnClickListener(v->{context.getSharedPreferences("dot-folders",0).edit().putString("cover-"+info.id,cover).apply();view.invalidate();dialog.dismiss();});android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();lp.width=0;lp.columnSpec=android.widget.GridLayout.spec(n%3,1f);lp.setMargins(DotUi.dp(context,3),DotUi.dp(context,10),DotUi.dp(context,3),0);grid.addView(choice,lp);
    }
    body.addView(grid);
    body.addView(DotUi.button(context,"Emoji cover",()->{
      android.widget.EditText input=new android.widget.EditText(context);input.setHint("Choose an emoji");input.setTextColor(DotAppearance.get(context).ink);input.setSingleLine(true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(16)});
      android.widget.LinearLayout editor=DotUi.column(context);editor.setPadding(DotUi.dp(context,20),DotUi.dp(context,18),DotUi.dp(context,20),DotUi.dp(context,18));editor.setBackground(DotUi.shape(context,DotAppearance.get(context).background,24));editor.addView(input);android.app.Dialog emoji=new android.app.Dialog(context);editor.addView(DotUi.button(context,"Apply",()->{String value=input.getText().toString().trim();if(!value.isEmpty()){context.getSharedPreferences("dot-folders",0).edit().putString("cover-"+info.id,"emoji:"+value).apply();view.invalidate();}emoji.dismiss();dialog.dismiss();}));emoji.setContentView(editor);emoji.show();emoji.getWindow().setLayout(-1,-2);emoji.getWindow().setGravity(android.view.Gravity.BOTTOM);
    }));
    body.addView(DotUi.button(context,expanded(info)?"Shrink":"Enlarge",()->{dialog.dismiss();var launcher=Launcher.getLauncher(context);DotArrange.end(launcher);var shortcut=DotItemActions.SIZE.getShortcut(launcher,info,view);if(shortcut!=null)shortcut.onClick(view);}));
    body.addView(DotUi.button(context,"Done",dialog::dismiss));dialog.setContentView(body);dialog.show();dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);dialog.getWindow().setLayout(-1,-2);DotUi.enter(body);
  }
  private static void cover(Canvas canvas,String cover,float cx,float cy,float r,Paint paint) {
    if(cover.startsWith("emoji:")){paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(r*1.8f);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText(cover.substring(6),cx,cy+r*.65f,paint);return;}
    for(int y=-4;y<=4;y++)for(int x=-4;x<=4;x++){float distance=x*x+y*y;boolean dot=cover.equals("Dot grid")?distance<=17:cover.equals("Cross")?(Math.abs(x)<=1||Math.abs(y)<=1):cover.equals("Orbit")?distance>=8&&distance<=18:cover.equals("Heart")?((y<0&&Math.abs(x)>0&&Math.abs(x)<4)||(y>=0&&Math.abs(x)<=3-y)):(distance<=4||(Math.abs(x)<=2&&Math.abs(y)>=2&&Math.abs(y)<=3)||(Math.abs(y)<=2&&Math.abs(x)>=2&&Math.abs(x)<=3));if(dot)canvas.drawCircle(cx+x*r/4,cy+y*r/4,r*.075f,paint);}
  }

  public static boolean expanded(FolderInfo info) { return info.spanX>1 && info.spanY>1; }
  private static float label(View view) { return view instanceof com.android.launcher3.folder.FolderIcon icon && !icon.getTextVisible()?0:view.getResources().getDisplayMetrics().density*24; }
  public static void draw(Canvas canvas,View view,FolderInfo info) {
    boolean dark=view.getContext().getSharedPreferences("dot-style",0).getBoolean("dark",false);
    float density=view.getResources().getDisplayMetrics().density;
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(DotAppearance.get(view.getContext()).surface);
    float size=Math.min(view.getWidth(),view.getHeight()-label(view));
    float left=(view.getWidth()-size)/2;
    canvas.drawRoundRect(new RectF(left,0,left+size,size),density*20,density*20,paint);
    String cover=view.getContext().getSharedPreferences("dot-folders",0).getString("cover-"+info.id,"");
    if(!cover.isEmpty()) {
      paint.setColor(DotAppearance.get(view.getContext()).ink);
      cover(canvas,cover,left+size/2,size/2,size*.30f,paint);
    } else {
    var entries=info.getContents();
    float cell=size/3;
    for(int n=0;n<Math.min(9,entries.size());n++) {
      float x=left+(n%3+.5f)*cell,y=(n/3+.5f)*cell,r=cell*.36f;
      if(n==8 && entries.size()>9) {
        paint.setColor(dark?0xffebede6:0xff242622);paint.setTextSize(density*15);paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("+"+(entries.size()-8),x,y+density*5,paint);
      } else if(entries.get(n) instanceof ItemInfoWithIcon entry) {
        Drawable icon=entry.newIcon(view.getContext());icon.setBounds((int)(x-r),(int)(y-r),(int)(x+r),(int)(y+r));icon.draw(canvas);
      }
    }
    }
    if(label(view)==0)return;
    paint.setColor(DotAppearance.get(view.getContext()).ink);paint.setTextSize(density*12);paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(view.getResources().getFont(com.android.launcher3.R.font.geist_regular));
    String title=info.title==null?"Folder":info.title.toString();
    while(title.length()>1 && paint.measureText(title)>view.getWidth()-density*8) title=title.substring(0,title.length()-1);
    canvas.drawText(title,view.getWidth()/2f,view.getHeight()-density*5,paint);
  }
  public static boolean launch(View view,FolderInfo info,float x,float y) {
    if(!expanded(info) || covered(view,info)) return false;
    float size=Math.min(view.getWidth(),view.getHeight()-label(view)), left=(view.getWidth()-size)/2;
    if(x<left || x>=left+size || y<0 || y>=size) return false;
    int index=(int)(y/(size/3))*3+(int)((x-left)/(size/3));
    var entries=info.getContents();
    if(index>=entries.size() || (index==8 && entries.size()>9)) return false;
    if(entries.get(index) instanceof WorkspaceItemInfo app) {
      Launcher.getLauncher(view.getContext()).startActivitySafely(view,app.getIntent(),app);return true;
    }
    return false;
  }
}
