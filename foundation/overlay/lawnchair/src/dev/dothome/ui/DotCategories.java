package dev.dothome.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.Gravity;
import android.widget.*;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.model.data.*;
import java.util.*;

/** Local classification and user corrections; no remote model, package lookup or inference API. */
public final class DotCategories {
  private static final String[] ORDER={"Communication","Social","Work","Media","Games","Travel","Shopping","Tools","Other"};
  public static Map<String,List<AppInfo>> apply(Context context,List<AppInfo> apps,Map<String,List<AppInfo>> upstream){
    Map<String,String> fallback=new HashMap<>();upstream.forEach((name,items)->items.forEach(app->fallback.put(app.toComponentKey().toString(),name)));
    var corrections=context.getSharedPreferences("dot-categories",0);Map<String,List<AppInfo>> grouped=new LinkedHashMap<>();for(String name:ORDER)grouped.put(name,new ArrayList<>());
    for(AppInfo app:apps){String key=app.toComponentKey().toString(),packageName=app.getTargetPackage();String category=corrections.getString(key,"");
      if(category.isEmpty()){
        String name=packageName==null?"":packageName.toLowerCase(Locale.ROOT);
        if(name.contains("dialer")||name.contains("messag")||name.contains("contacts")||name.contains("whatsapp")||name.contains("telegram"))category="Communication";
        else if(name.contains("gmail")||name.contains("calendar")||name.contains("docs")||name.contains("office")||name.contains("keep"))category="Work";
        else if(name.contains("maps")||name.contains("transit")||name.contains("uber"))category="Travel";
        else if(name.contains("camera")||name.contains("photos")||name.contains("gallery")||name.contains("youtube")||name.contains("music")||name.contains("spotify"))category="Media";
        else try {int type=context.getPackageManager().getApplicationInfo(packageName,0).category;category=switch(type){case ApplicationInfo.CATEGORY_GAME->"Games";case ApplicationInfo.CATEGORY_AUDIO,ApplicationInfo.CATEGORY_VIDEO,ApplicationInfo.CATEGORY_IMAGE->"Media";case ApplicationInfo.CATEGORY_SOCIAL->"Social";case ApplicationInfo.CATEGORY_MAPS->"Travel";case ApplicationInfo.CATEGORY_PRODUCTIVITY->"Work";default->fallback.getOrDefault(key,"Other");};}catch(Exception unavailable){category=fallback.getOrDefault(key,"Other");}
        if(category.equals("System Apps")||category.equals("Google Apps"))category="Tools";
      }
      grouped.computeIfAbsent(category,k->new ArrayList<>()).add(app);
    }
    grouped.values().removeIf(List::isEmpty);java.text.Collator collator=java.text.Collator.getInstance();grouped.values().forEach(items->items.sort((a,b)->collator.compare(a.title.toString(),b.title.toString())));return grouped;
  }
  public static void choose(Context context,ItemInfoWithIcon app){
    if(!(app instanceof AppInfo info))return;Dialog dialog=new Dialog(context);LinearLayout body=DotUi.column(context);body.setPadding(DotUi.dp(context,20),DotUi.dp(context,16),DotUi.dp(context,20),DotUi.dp(context,18));body.setBackground(DotUi.shape(context,DotAppearance.get(context).background,26));body.addView(DotUi.text(context,"Category · "+app.title,17));ScrollView scroll=new ScrollView(context);LinearLayout choices=DotUi.column(context);String key=info.toComponentKey().toString();
    for(String category:ORDER)choices.addView(DotUi.button(context,category,()->{context.getSharedPreferences("dot-categories",0).edit().putString(key,category).apply();LauncherAppState.getInstance(context).getModel().forceReload();dialog.dismiss();}));
    choices.addView(DotUi.button(context,"Automatic",()->{context.getSharedPreferences("dot-categories",0).edit().remove(key).apply();LauncherAppState.getInstance(context).getModel().forceReload();dialog.dismiss();}));scroll.addView(choices);body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(body);dialog.show();dialog.getWindow().setGravity(Gravity.BOTTOM);dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(-1,(int)(context.getResources().getDisplayMetrics().heightPixels*.75f));
  }
}
