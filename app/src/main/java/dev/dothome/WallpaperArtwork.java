package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original static opalescent gradients, rendered locally without image downloads. */
final class WallpaperArtwork extends Drawable {
  final String preset;
  final Paint p = new Paint(3);

  WallpaperArtwork(String preset) {
    this.preset = preset;
  }

  void glow(Canvas c, float x, float y, float radius, int color, float w, float h) {
    p.setShader(
        new RadialGradient(
            x,
            y,
            radius,
            new int[] {color, color & 0x00ffffff},
            new float[] {0, 1},
            Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, p);
  }

  @Override
  public void draw(Canvas c) {
    Rect b = getBounds();
    c.save();
    c.translate(b.left, b.top);
    float w = b.width(), h = b.height();
    boolean light = preset.equals("mist");
    c.drawColor(light ? 0xffc4ccc2 : 0xff10242b);
    if (light) {
      glow(c, -w * .1f, h * .03f, w * .9f, 0xcc9bd4b8, w, h);
      glow(c, w * .8f, h * .12f, w * .62f, 0x99b8a2d5, w, h);
      glow(c, w * .85f, h * .43f, w * .8f, 0xaab5ce91, w, h);
      glow(c, w * .06f, h * .48f, w * .85f, 0x99c1afd5, w, h);
      glow(c, w * .75f, h * .72f, w * .7f, 0x77d3bed8, w, h);
      glow(c, w * .1f, h * .93f, w * .8f, 0xbbafcba8, w, h);
    } else {
      glow(c, w * .05f, h * .25f, w * 1.15f, 0xbb355968, w, h);
      glow(c, w * .95f, h * .68f, w * .8f, 0x883e7273, w, h);
      glow(c, w * .4f, h * .9f, w * .7f, 0x99243c4d, w, h);
    }
    Path curve = new Path();
    curve.moveTo(w * 1.2f, h * .08f);
    curve.cubicTo(-w * .5f, h * .22f, w * 1.8f, h * .55f, -w * .2f, h * .83f);
    p.setShader(
        new LinearGradient(
            0,
            0,
            w,
            h,
            new int[] {0x00ffffff, light ? 0x33ffffff : 0x163cabb2, 0x00ffffff},
            null,
            Shader.TileMode.CLAMP));
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(w * .25f);
    c.drawPath(curve, p);
    p.setStrokeWidth(w * .02f);
    p.setShader(
        new LinearGradient(
            0,
            0,
            w,
            h,
            new int[] {0x00ffffff, light ? 0x44ffffff : 0x225ca7af, 0x00ffffff},
            null,
            Shader.TileMode.CLAMP));
    c.drawPath(curve, p);
    p.setShader(null);
    p.setStyle(Paint.Style.FILL);
    c.restore();
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter colorFilter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
