package dev.dothome.widgets;

import android.app.Dialog;
import android.content.ComponentName;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.android.launcher3.*;
import com.android.launcher3.widget.*;
import com.android.launcher3.widget.custom.CustomWidgetManager;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import dev.dothome.ui.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Family-first catalogue; placement still runs through the native widget add flow. */
public final class DotWidgetTray {
  private static WeakReference<Dialog> opened=new WeakReference<>(null);
  public static void close(){Dialog dialog=opened.get();if(dialog!=null)dialog.dismiss();opened.clear();}
  public static void show(Launcher launcher){
    close();Dialog dialog=new Dialog(launcher);opened=new WeakReference<>(dialog);var a=DotAppearance.get(launcher);
    LinearLayout root=DotUi.column(launcher);root.setPadding(DotUi.dp(launcher,18),DotUi.dp(launcher,14),DotUi.dp(launcher,18),DotUi.dp(launcher,16));root.setBackground(DotUi.shape(launcher,a.background,28));
    LinearLayout heading=new LinearLayout(launcher);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=DotUi.text(launcher,"Widgets",23);title.setAccessibilityHeading(true);heading.addView(title,new LinearLayout.LayoutParams(0,DotUi.dp(launcher,48),1));heading.addView(DotUi.button(launcher,"Done",dialog::dismiss));root.addView(heading);
    LinearLayout tabs=new LinearLayout(launcher);tabs.addView(DotUi.button(launcher,"Dot Home",()->{}),new LinearLayout.LayoutParams(0,-2,1));tabs.addView(DotUi.button(launcher,"Installed apps",()->{dialog.dismiss();WidgetsFullSheet.show(launcher,true);}),new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);
    EditText search=new EditText(launcher);search.setSingleLine(true);search.setTextColor(a.ink);search.setHintTextColor(a.secondary);search.setHint("Search widgets");search.setTextSize(14);search.setTypeface(launcher.getResources().getFont(R.font.geist_regular));search.setBackground(DotUi.shape(launcher,a.surface,24));search.setPadding(DotUi.dp(launcher,16),0,DotUi.dp(launcher,16),0);LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,DotUi.dp(launcher,48));searchParams.topMargin=DotUi.dp(launcher,12);root.addView(search,searchParams);
    ScrollView scroll=new ScrollView(launcher);LinearLayout list=DotUi.column(launcher);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    LinkedHashMap<String,List<LauncherAppWidgetProviderInfo>> families=new LinkedHashMap<>();
    String[] providers=launcher.getResources().getStringArray(R.array.custom_widget_providers);
    for(String name:providers){var info=CustomWidgetManager.INSTANCE.get(launcher).getWidgetProvider(new ComponentName(launcher.getPackageName(),"#custom-widget-"+name));if(info==null)continue;try{DotTile tile=(DotTile)Class.forName(name).getConstructor(android.content.Context.class).newInstance(launcher);String kind=tile.getKind();String family=switch(kind){case "clock"->"Clocks";case "photo"->"Photos & Collector";case "battery"->"Battery";case "breathing"->"Breathing break";case "away"->"Away time";case "usage"->"Screen time";case "weather"->"Weather · Local";case "torch","bluetooth","sound","dnd","controls"->"Quick controls";default->info.label;};families.computeIfAbsent(family,k->new ArrayList<>()).add(info);}catch(Exception unavailable){android.util.Log.w("DotWidgets","Widget unavailable: "+name);}}
    ArrayList<View> groups=new ArrayList<>();
    for(var family:families.entrySet()){
      LinearLayout group=DotUi.column(launcher);group.setTag(family.getKey().toLowerCase(Locale.ROOT));TextView label=DotUi.text(launcher,family.getKey(),16);label.setPadding(DotUi.dp(launcher,6),DotUi.dp(launcher,24),0,DotUi.dp(launcher,12));group.addView(label);
      HorizontalScrollView strip=new HorizontalScrollView(launcher);strip.setHorizontalScrollBarEnabled(false);LinearLayout variants=new LinearLayout(launcher);variants.setGravity(Gravity.CENTER_VERTICAL);
      for(var info:family.getValue()){
        LinearLayout variant=DotUi.column(launcher);variant.setGravity(Gravity.CENTER);variant.setPadding(DotUi.dp(launcher,10),DotUi.dp(launcher,12),DotUi.dp(launcher,10),DotUi.dp(launcher,10));variant.setBackground(DotUi.clickable(launcher,a.card,20));
        ImageView image=new ImageView(launcher);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(TilePreviews.render(launcher,info,DotUi.dp(launcher,180),null));variant.addView(image,new LinearLayout.LayoutParams(DotUi.dp(launcher,160),DotUi.dp(launcher,140)));
        TextView caption=DotUi.text(launcher,info.label+" · "+info.spanX+" × "+info.spanY,12);caption.setGravity(Gravity.CENTER);caption.setMaxLines(2);variant.addView(caption,new LinearLayout.LayoutParams(-1,DotUi.dp(launcher,40)));
        variant.setContentDescription(info.label+", "+info.spanX+" by "+info.spanY+", add to Home");variant.setFocusable(true);variant.setOnClickListener(v->{if(add(launcher,info))dialog.dismiss();});LinearLayout.LayoutParams variantParams=new LinearLayout.LayoutParams(-2,-2);variantParams.rightMargin=DotUi.dp(launcher,10);variants.addView(variant,variantParams);
      }
      strip.addView(variants);group.addView(strip);list.addView(group);groups.add(group);
    }
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){String q=s.toString().trim().toLowerCase(Locale.ROOT);for(View g:groups)g.setVisibility(g.getTag().toString().contains(q)?View.VISIBLE:View.GONE);}public void afterTextChanged(android.text.Editable e){}});
    dialog.setContentView(root);dialog.show();dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setGravity(Gravity.BOTTOM);dialog.getWindow().setLayout(-1,(int)(launcher.getResources().getDisplayMetrics().heightPixels*.87f));DotUi.enter(root);
  }
  private static boolean add(Launcher launcher,LauncherAppWidgetProviderInfo info){
    if(DotArrange.locked(launcher)){Toast.makeText(launcher,"Home layout is frozen",Toast.LENGTH_SHORT).show();return false;}
    Workspace<?> workspace=launcher.getWorkspace();int current=workspace.getCurrentPage();int[] cell=new int[2];
    for(int n=0;n<workspace.getPageCount();n++){int index=(current+n)%workspace.getPageCount();int screen=workspace.getScreenIdForPageIndex(index);if(screen<0)continue;CellLayout page=workspace.getScreenWithId(screen);if(page!=null&&page.findCellForSpan(cell,info.spanX,info.spanY)){launcher.addPendingItem(new PendingAddWidgetInfo(info,LauncherSettings.Favorites.CONTAINER_WIDGETS_TRAY),LauncherSettings.Favorites.CONTAINER_DESKTOP,screen,cell,info.spanX,info.spanY);workspace.snapToPage(index);return true;}}
    Toast.makeText(launcher,"Make room for this widget, then try again",Toast.LENGTH_SHORT).show();return false;
  }
}
