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
    String[] names={"App previews","Dot grid","Flower","Orbit","Cross"};
    new android.app.AlertDialog.Builder(context).setTitle("Folder cover").setItems(names,(dialog,index)->{context.getSharedPreferences("dot-folders",0).edit().putString("cover-"+info.id,index==0?"":names[index]).apply();view.invalidate();}).setNegativeButton("Cancel",null).show();
  }
  public static boolean expanded(FolderInfo info) { return info.spanX>1 && info.spanY>1; }
  private static float label(View view) { return view.getResources().getDisplayMetrics().density*24; }
  public static void draw(Canvas canvas,View view,FolderInfo info) {
    boolean dark=view.getContext().getSharedPreferences("dot-style",0).getBoolean("dark",false);
    float density=view.getResources().getDisplayMetrics().density;
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(dark?0xe62a2c29:0xd9f6f7f1);
    float size=Math.min(view.getWidth(),view.getHeight()-label(view));
    float left=(view.getWidth()-size)/2;
    canvas.drawRoundRect(new RectF(left,0,left+size,size),density*20,density*20,paint);
    String cover=view.getContext().getSharedPreferences("dot-folders",0).getString("cover-"+info.id,"");
    if(!cover.isEmpty()) {
      paint.setColor(dark?0xfff0f1eb:0xff242622);
      float cx=left+size/2,cy=size/2,r=size*.30f;
      for(int y=-4;y<=4;y++)for(int x=-4;x<=4;x++) {
        float distance=x*x+y*y;boolean dot=cover.equals("Dot grid")?distance<=17:cover.equals("Cross")?(Math.abs(x)<=1||Math.abs(y)<=1):cover.equals("Orbit")?distance>=8&&distance<=18:(distance<=4 || (Math.abs(x)<=2&&Math.abs(y)>=2&&Math.abs(y)<=3) || (Math.abs(y)<=2&&Math.abs(x)>=2&&Math.abs(x)<=3));
        if(dot)canvas.drawCircle(cx+x*r/4,cy+y*r/4,size*.018f,paint);
      }
      return;
    }
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
    paint.setColor(dark?0xfff0f1eb:0xff242622);paint.setTextSize(density*12);paint.setTextAlign(Paint.Align.CENTER);
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
