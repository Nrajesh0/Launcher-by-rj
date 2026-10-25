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
    // Fine optical rings and soft colour fields, rather than solid ribbon bands.
    p.setShader(null);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(Math.max(.5f, w / 1300f));
    p.setColor(light ? 0x15ffffff : 0x1248bec3);
    for (int n = 0; n < 11; n++) {
      float radius = w * (.36f + n * .075f);
      c.drawCircle(w * 1.03f, h * .58f, radius, p);
    }
    p.setStyle(Paint.Style.FILL);
    p.setColor(light ? 0x0affffff : 0x08d7ffff);
    float[] grain = new float[2048];
    int noise = 19473;
    for (int n = 0; n < grain.length; n++) {
      noise = noise * 1664525 + 1013904223;
      grain[n] = ((noise >>> 8) & 65535) / 65535f * (n % 2 == 0 ? w : h);
    }
    p.setStrokeWidth(Math.max(.5f, w / 900f));
    c.drawPoints(grain, p);
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
