package dev.dothome.ui;
import android.content.Context;
import android.graphics.Color;
/** Shared launcher-owned surface and ink colours for tiles and dock search. */
public final class DotSurface {
  public static int ink(Context context) {return context.getSharedPreferences("dot-style",0).getBoolean("dark",false)?0xfff0f0e9:0xff151813;}
  public static int paper(Context context) {
    var prefs=context.getSharedPreferences("dot-style",0);boolean dark=prefs.getBoolean("dark",false);String material=prefs.getString("material","classic");
    if(material.equals("classic"))return dark?0xff202120:0xfff7f7f2;
    boolean glass=material.equals("translucent");int base=dark?0xff262e2c:0xfff2f5eb,accent=prefs.getInt("accent",0xff98b9a5),amount=glass?16:28;
    return Color.argb(glass?174:255,(Color.red(base)*(100-amount)+Color.red(accent)*amount)/100,(Color.green(base)*(100-amount)+Color.green(accent)*amount)/100,(Color.blue(base)*(100-amount)+Color.blue(accent)*amount)/100);
  }
}
