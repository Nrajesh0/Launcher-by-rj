package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original local gradients; no remote images or reference-photo assets. */
final class WallpaperArtwork extends Drawable {
  final String preset;
  final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

  WallpaperArtwork(String preset) {
    this.preset = preset;
  }

  @Override
  public void draw(Canvas canvas) {
    Rect b = getBounds();
    float w = b.width(), h = b.height();
    boolean mist = preset.equals("mist");
    paint.setShader(
        new LinearGradient(
            0,
            0,
            w,
            h,
            mist
                ? new int[] {0xffaebdc2, 0xffc4ccbe, 0xffb8b3c7}
                : new int[] {0xff152832, 0xff1d4d59, 0xff0a1621},
            null,
            Shader.TileMode.CLAMP));
    canvas.drawRect(b, paint);
    paint.setShader(
        new RadialGradient(
            w * .1f,
            h * .35f,
            w * .95f,
            mist ? new int[] {0x88dce8df, 0x00dce8df} : new int[] {0x885c9ba0, 0x005c9ba0},
            null,
            Shader.TileMode.CLAMP));
    canvas.drawRect(b, paint);
    paint.setShader(null);
    paint.setColor(mist ? 0x16435f69 : 0x163b91a6);
    canvas.drawCircle(w * .9f, h * .72f, w * .8f, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1);
    paint.setColor(mist ? 0x2284939a : 0x335b9eab);
    canvas.drawCircle(w * .9f, h * .72f, w * .8f, paint);
    paint.setStyle(Paint.Style.FILL);
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter filter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
