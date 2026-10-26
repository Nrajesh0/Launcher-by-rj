package dev.dothome.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;
import com.android.launcher3.R;

/** Shared accessible controls for launcher-owned screens and sheets. */
public final class DotUi {
  public static int dp(Context c,float n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
  public static GradientDrawable shape(Context c,int colour,float radius){GradientDrawable d=new GradientDrawable();d.setColor(colour);d.setCornerRadius(dp(c,radius));return d;}
  public static android.graphics.drawable.Drawable clickable(Context c,int colour,float radius){DotAppearance a=DotAppearance.get(c);return new RippleDrawable(ColorStateList.valueOf(DotAppearance.alpha(a.ink,26)),shape(c,colour,radius),shape(c,Color.WHITE,radius));}
  public static TextView text(Context c,String value,float size){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(DotAppearance.get(c).ink);t.setTypeface(c.getResources().getFont(R.font.geist_regular));t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(false);return t;}
  public static TextView button(Context c,String value,Runnable action){DotAppearance a=DotAppearance.get(c);TextView t=text(c,value,14);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,48));t.setPadding(dp(c,18),dp(c,12),dp(c,18),dp(c,12));t.setBackground(clickable(c,a.card,28));t.setFocusable(true);t.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);action.run();});return t;}
  public static void enter(View view){if(DotAppearance.get(view.getContext()).reducedMotion)return;view.setAlpha(0);view.setTranslationY(dp(view.getContext(),8));view.animate().alpha(1).translationY(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
  public static void bars(Activity activity){DotAppearance a=DotAppearance.get(activity);activity.getWindow().setDecorFitsSystemWindows(false);WindowInsetsController control=activity.getWindow().getDecorView().getWindowInsetsController();if(control!=null)control.setSystemBarsAppearance(a.dark?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
  public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
  public static LinearLayout page(Activity a,String title,Runnable back){
    DotAppearance appearance=DotAppearance.get(a);LinearLayout root=column(a);root.setBackgroundColor(appearance.background);
    root.setOnApplyWindowInsetsListener((v,insets)->{var b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(a,20),b.top,dp(a,20),b.bottom);return insets;});
    LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);
    TextView close=button(a,"‹",back);close.setTextSize(30);close.setContentDescription("Back");header.addView(close,new LinearLayout.LayoutParams(dp(a,48),dp(a,56)));
    TextView heading=text(a,title,17);heading.setGravity(Gravity.CENTER);heading.setAccessibilityHeading(true);header.addView(heading,new LinearLayout.LayoutParams(0,dp(a,56),1));header.addView(new View(a),new LinearLayout.LayoutParams(dp(a,48),1));root.addView(header);
    a.setContentView(root);bars(a);return root;
  }
}
