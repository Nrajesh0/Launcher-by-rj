package dev.dothome.widgets;
import android.graphics.Color;
final class TileTheme {
  static int widgetSurface(TileContext context) {
    if(context.settings.optString("material","classic").equals("classic")) return context.dark()?0xff202120:0xfff7f7f2;
    boolean glass=context.settings.optString("material").equals("translucent");
    int base=context.dark()?0xff262e2c:0xfff2f5eb, accent=context.accent, amount=glass?16:28;
    return Color.argb(glass?174:255,(Color.red(base)*(100-amount)+Color.red(accent)*amount)/100,(Color.green(base)*(100-amount)+Color.green(accent)*amount)/100,(Color.blue(base)*(100-amount)+Color.blue(accent)*amount)/100);
  }
}
