package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Shared offline colour and material tokens for launcher surfaces and widgets. */
final class LauncherTheme {
  static int mix(int a, int b, float fraction) {
    return Color.rgb(
        Math.round(Color.red(a) * (1 - fraction) + Color.red(b) * fraction),
        Math.round(Color.green(a) * (1 - fraction) + Color.green(b) * fraction),
        Math.round(Color.blue(a) * (1 - fraction) + Color.blue(b) * fraction));
  }

  static int wallpaperAccent(Drawable drawable) {
    if (drawable == null) return 0xff8ba7a0;
    Bitmap pixels = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
    Rect previous = new Rect(drawable.getBounds());
    drawable.setBounds(0, 0, 24, 24);
    drawable.draw(new Canvas(pixels));
    drawable.setBounds(previous);
    float[] hsv = new float[3];
    int winner = 0xff8ba7a0;
    float best = 0;
    for (int y = 0; y < 24; y++)
      for (int x = 0; x < 24; x++) {
        int color = pixels.getPixel(x, y);
        Color.colorToHSV(color, hsv);
        float score = hsv[1] * (1 - Math.abs(hsv[2] - .65f));
        if (Color.alpha(color) > 128 && score > best) {
          best = score;
          winner = color;
        }
      }
    pixels.recycle();
    return winner;
  }

  static int widgetSurface(HomeActivity a) {
    if (!a.settings.optString("material", "classic").equals("translucent"))
      return a.dark() ? 0xff202120 : 0xfff0f2eb;
    int tint = mix(a.dark() ? 0xff202120 : 0xfff0f2eb, a.accent, .16f);
    return (tint & 0xffffff) | 0xd9000000;
  }
}
