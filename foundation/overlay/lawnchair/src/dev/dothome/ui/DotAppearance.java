package dev.dothome.ui;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.provider.Settings;

/** Resolved, local-only design tokens. All launcher-owned renderers use this model. */
public final class DotAppearance {
  private static SharedPreferences observed;
  private static volatile DotAppearance cached;
  private static final SharedPreferences.OnSharedPreferenceChangeListener changed=(preferences,key)->cached=null;
  public final boolean dark, glass, classic, reducedMotion;
  public final int accent, ink, secondary, background, card, surface, outline, onAccent;
  public final int iconBackground, iconForeground, activeControl;
  public final String material;

  private DotAppearance(Context context) {
    SharedPreferences p=context.getSharedPreferences("dot-style",0);
    dark=p.getBoolean("dark",false);
    material=p.getString("material","classic");
    classic=material.equals("classic");glass=material.equals("translucent");
    reducedMotion=p.getBoolean("reduceMotion",false) ||
      Settings.Global.getFloat(context.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f;
    int seed=p.getBoolean("manualAccent",false)?p.getInt("accent",0xff6d9a80):p.getInt("wallpaperAccent",0xff7fa88d);
    accent=classic&&!p.getBoolean("manualAccent",false)?0xffd71920:tone(seed,dark?.78f:.38f,.64f);
    onAccent=luminance(accent)>.47?0xff111512:0xfffafbf7;
    ink=dark?0xfff2f3ed:0xff171b18;
    secondary=classic?(dark?0xffadafa7:0xff60685e):tone(seed,dark?.72f:.35f,.13f);
    background=classic?(dark?0xff111211:0xfff5f6f1):tone(seed,dark?.065f:.96f,.09f);
    card=classic?(dark?0xff242623:0xfffdfef9):tone(seed,dark?.14f:.985f,.10f);
    int solid=classic?(dark?0xff20221f:0xfff6f7f1):tone(seed,dark?.18f:.91f,glass?.17f:.27f);
    surface=glass?alpha(solid,dark?192:181):solid;
    outline=alpha(ink,dark?42:28);
    iconBackground=classic?(dark?0xff20221f:0xfff7f8f3):tone(seed,dark?.16f:.94f,.17f);
    iconForeground=classic?ink:tone(seed,dark?.90f:.15f,.29f);
    activeControl=accent;
  }
  public static DotAppearance get(Context context){
    DotAppearance value=cached;if(value!=null)return value;
    synchronized(DotAppearance.class){if(observed==null){observed=context.getApplicationContext().getSharedPreferences("dot-style",0);observed.registerOnSharedPreferenceChangeListener(changed);}if(cached==null)cached=new DotAppearance(context);return cached;}
  }
  public static void invalidate(){cached=null;}
  public static int alpha(int colour,int alpha){return (colour&0x00ffffff)|(Math.max(0,Math.min(255,alpha))<<24);}
  public static int mix(int a,int b,float amount){return Color.rgb(Math.round(Color.red(a)*(1-amount)+Color.red(b)*amount),Math.round(Color.green(a)*(1-amount)+Color.green(b)*amount),Math.round(Color.blue(a)*(1-amount)+Color.blue(b)*amount));}
  public static int tone(int seed,float lightness,float saturation){
    float[] hsv=new float[3];Color.colorToHSV(seed,hsv);
    // HSL lightness keeps light/dark palettes comparable across wallpaper hues.
    float value=lightness+saturation*Math.min(lightness,1-lightness);
    hsv[1]=value==0?0:2*(1-lightness/value);hsv[2]=value;return Color.HSVToColor(hsv);
  }
  public static double luminance(int colour){double sum=0;double[] weights={.2126,.7152,.0722};int[] rgb={Color.red(colour),Color.green(colour),Color.blue(colour)};for(int n=0;n<3;n++){double c=rgb[n]/255.0;sum+=weights[n]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));}return sum;}
  /** Public colour metadata, not wallpaper pixels or access to another app's screen. */
  public static boolean refreshWallpaper(Context context){
    try{
      WallpaperManager wm=WallpaperManager.getInstance(context);
      SharedPreferences p=context.getSharedPreferences("dot-style",0);int id=wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM);
      WallpaperColors colours=wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
      int seed=p.getInt("wallpaperAccent",0xff7fa88d);
      if(colours!=null){seed=colours.getPrimaryColor().toArgb();
        if(colours.getSecondaryColor()!=null){int second=colours.getSecondaryColor().toArgb();float[] a=new float[3],b=new float[3];Color.colorToHSV(seed,a);Color.colorToHSV(second,b);if(b[1]>a[1])seed=second;}
      }
      boolean ownLive=wm.getWallpaperInfo()!=null&&wm.getWallpaperInfo().getPackageName().equals(context.getPackageName());
      boolean changedId=p.contains("observedWallpaperId")&&p.getInt("observedWallpaperId",id)!=id;
      boolean external=(p.getBoolean("wallpaperApplied",false)||changedId)&&p.getInt("ownedWallpaperId",-2)!=id&&!ownLive;
      if(p.getInt("wallpaperAccent",0)==seed&&p.getInt("observedWallpaperId",-1)==id&&p.getBoolean("externalWallpaper",false)==external)return false;
      var edit=p.edit().putInt("wallpaperAccent",seed).putInt("observedWallpaperId",id).putBoolean("externalWallpaper",external);
      if(external)edit.putBoolean("wallpaperApplied",true);
      edit.apply();return true;
    }catch(RuntimeException unavailable){return false;}
  }
}
