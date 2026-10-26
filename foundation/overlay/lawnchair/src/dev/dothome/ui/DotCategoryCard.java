package dev.dothome.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.android.launcher3.Launcher;
import com.android.launcher3.model.data.*;
import java.util.*;

/** Category cards have direct app targets and a separate overflow target. */
public final class DotCategoryCard extends LinearLayout {
  private final FolderInfo folder;
  public DotCategoryCard(Context context,FolderInfo folder){
    super(context);this.folder=folder;setOrientation(VERTICAL);setPadding(DotUi.dp(context,7),DotUi.dp(context,6),DotUi.dp(context,7),DotUi.dp(context,12));
    LinearLayout card=DotUi.column(context);card.setPadding(DotUi.dp(context,10),DotUi.dp(context,10),DotUi.dp(context,10),DotUi.dp(context,10));card.setBackground(new DotGlassDrawable(card,24));
    GridLayout grid=new GridLayout(context);grid.setColumnCount(2);int count=folder.getContents().size();
    for(int n=0;n<4;n++){
      View cell;
      if(n<3&&n<count&&folder.getContents().get(n) instanceof ItemInfoWithIcon app)cell=appIcon(context,app);
      else if(n==3&&count>3){GridLayout mini=new GridLayout(context);mini.setColumnCount(2);for(int index=3;index<Math.min(7,count);index++)if(folder.getContents().get(index) instanceof ItemInfoWithIcon app){ImageView icon=new ImageView(context);icon.setImageDrawable(app.newIcon(context));GridLayout.LayoutParams small=new GridLayout.LayoutParams();small.width=DotUi.dp(context,25);small.height=DotUi.dp(context,25);small.columnSpec=GridLayout.spec((index-3)%2);small.rowSpec=GridLayout.spec((index-3)/2);mini.addView(icon,small);}mini.setPadding(DotUi.dp(context,5),DotUi.dp(context,5),0,0);mini.setContentDescription("More apps in "+folder.title);mini.setOnClickListener(v->open());cell=mini;}
      else cell=new View(context);
      GridLayout.LayoutParams params=new GridLayout.LayoutParams();params.width=0;params.height=DotUi.dp(context,64);params.columnSpec=GridLayout.spec(n%2,1f);params.rowSpec=GridLayout.spec(n/2);grid.addView(cell,params);
    }
    card.addView(grid,new LinearLayout.LayoutParams(-1,-2));card.setContentDescription((folder.title==null?"Category":folder.title)+", "+count+" apps");addView(card,new LinearLayout.LayoutParams(-1,-2));
    TextView title=DotUi.text(context,folder.title==null?"Apps":folder.title.toString(),12);title.setGravity(Gravity.CENTER);title.setPadding(0,DotUi.dp(context,8),0,0);title.setMaxLines(2);addView(title,new LinearLayout.LayoutParams(-1,-2));
    card.setOnClickListener(v->open());title.setOnClickListener(v->open());
  }
  private View appIcon(Context context,ItemInfoWithIcon app){
    ImageView icon=new ImageView(context);icon.setPadding(DotUi.dp(context,7),DotUi.dp(context,7),DotUi.dp(context,7),DotUi.dp(context,7));icon.setImageDrawable(app.newIcon(context));icon.setContentDescription(app.title);icon.setBackground(DotUi.clickable(context,android.graphics.Color.TRANSPARENT,100));icon.setFocusable(true);icon.setOnClickListener(v->launch(v,app));icon.setOnLongClickListener(v->{DotCategories.choose(context,app);return true;});return icon;
  }
  private void launch(View v,ItemInfoWithIcon app){android.content.Intent intent=app.getIntent();if(intent!=null)Launcher.getLauncher(getContext()).startActivitySafely(v,intent,app);}
  private void open(){
    Context c=getContext();Dialog dialog=new Dialog(c);LinearLayout body=DotUi.column(c);body.setPadding(DotUi.dp(c,18),DotUi.dp(c,18),DotUi.dp(c,18),DotUi.dp(c,18));body.setBackground(DotUi.shape(c,DotAppearance.get(c).background,28));TextView title=DotUi.text(c,folder.title==null?"Apps":folder.title.toString(),20);title.setPadding(0,0,0,DotUi.dp(c,16));body.addView(title);
    ScrollView scroll=new ScrollView(c);GridLayout grid=new GridLayout(c);grid.setColumnCount(3);int at=0;
    for(var value:folder.getContents())if(value instanceof ItemInfoWithIcon app){LinearLayout cell=DotUi.column(c);cell.setGravity(Gravity.CENTER);cell.addView(appIcon(c,app),new LinearLayout.LayoutParams(DotUi.dp(c,58),DotUi.dp(c,58)));TextView name=DotUi.text(c,app.title.toString(),11);name.setGravity(Gravity.CENTER);name.setMaxLines(2);cell.addView(name,new LinearLayout.LayoutParams(-1,DotUi.dp(c,40)));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.columnSpec=GridLayout.spec(at++%3,1f);grid.addView(cell,lp);}
    scroll.addView(grid);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body.addView(DotUi.button(c,"Done",dialog::dismiss));dialog.setContentView(body);dialog.show();dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setGravity(Gravity.BOTTOM);dialog.getWindow().setLayout(-1,(int)(c.getResources().getDisplayMetrics().heightPixels*.70f));DotUi.enter(body);
  }
}
