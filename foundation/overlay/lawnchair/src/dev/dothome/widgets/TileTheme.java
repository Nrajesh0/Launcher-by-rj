package dev.dothome.widgets;
import android.graphics.Color;
final class TileTheme {
  static int widgetSurface(TileContext context) {
    if(context.settings.optString("material","classic").equals("classic")) return context.dark()?0xff202120:0xfff7f7f2;
    int base=context.dark()?0xff262e2c:0xfff2f5eb, accent=context.accent;
    return Color.argb(174,(Color.red(base)*84+Color.red(accent)*16)/100,(Color.green(base)*84+Color.green(accent)*16)/100,(Color.blue(base)*84+Color.blue(accent)*16)/100);
  }
}
