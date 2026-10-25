package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original, simple system-app symbols inside a consistent circular mask. */
final class AppIconArtwork extends Drawable {
  final String kind;
  final int paper, ink;
  final Paint p = new Paint(3);

  AppIconArtwork(String kind, boolean dark) {
    this.kind = kind;
    paper = dark ? 0xff202120 : 0xfff1f3ed;
    ink = dark ? 0xffeeeeea : 0xff151716;
  }

  static String kind(String name) {
    String n = name.toLowerCase(java.util.Locale.ROOT);
    if (n.contains("dialer") || n.contains("contacts")) return "phone";
    if (n.contains("messag") || n.contains("sms")) return "messages";
    if (n.contains("camera")) return "camera";
    if (n.contains("chrome") || n.contains("browser")) return "browser";
    if (n.contains("gallery") || n.contains("photos")) return "photo";
    if (n.contains("settings")) return "settings";
    return null;
  }

  @Override
  public void draw(Canvas c) {
    Rect b = getBounds();
    c.save();
    c.translate(b.left, b.top);
    c.scale(b.width() / 100f, b.height() / 100f);
    p.setStyle(Paint.Style.FILL);
    p.setColor(paper);
    c.drawCircle(50, 50, 48, p);
    p.setColor(ink);
    p.setStrokeWidth(4);
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setStrokeJoin(Paint.Join.ROUND);
    Path path = new Path();
    switch (kind) {
      case "phone":
        path.moveTo(34, 27);
        path.cubicTo(29, 27, 25, 32, 27, 41);
        path.cubicTo(31, 58, 43, 70, 60, 74);
        path.cubicTo(68, 76, 75, 70, 74, 65);
        path.lineTo(62, 55);
        path.quadTo(59, 53, 55, 59);
        path.cubicTo(47, 56, 42, 51, 40, 44);
        path.quadTo(47, 40, 43, 36);
        path.close();
        c.drawPath(path, p);
        break;
      case "messages":
        c.drawRoundRect(new RectF(26, 29, 74, 64), 15, 15, p);
        path.moveTo(36, 60);
        path.lineTo(36, 74);
        path.lineTo(51, 61);
        path.close();
        c.drawPath(path, p);
        break;
      case "camera":
        c.drawRoundRect(new RectF(24, 35, 76, 69), 4, 4, p);
        c.drawRoundRect(new RectF(35, 29, 49, 39), 2, 2, p);
        p.setColor(paper);
        c.drawCircle(50, 52, 11, p);
        p.setColor(ink);
        c.drawCircle(50, 52, 6, p);
        break;
      case "browser":
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        c.drawCircle(50, 50, 24, p);
        c.drawCircle(50, 50, 10, p);
        c.drawLine(50, 26, 70, 50, p);
        c.drawLine(74, 50, 41, 60, p);
        c.drawLine(38, 71, 40, 34, p);
        break;
      case "photo":
        p.setStyle(Paint.Style.STROKE);
        c.drawRect(30, 26, 70, 74, p);
        c.drawCircle(41, 39, 4, p);
        path.moveTo(32, 66);
        path.lineTo(49, 47);
        path.lineTo(68, 66);
        c.drawPath(path, p);
        break;
      default:
        p.setStyle(Paint.Style.STROKE);
        c.drawCircle(50, 50, 18, p);
        c.drawCircle(50, 50, 6, p);
        for (int n = 0; n < 8; n++) {
          double angle = n * Math.PI / 4;
          c.drawLine(
              50 + (float) Math.cos(angle) * 18,
              50 + (float) Math.sin(angle) * 18,
              50 + (float) Math.cos(angle) * 25,
              50 + (float) Math.sin(angle) * 25,
              p);
        }
    }
    p.setStyle(Paint.Style.FILL);
    c.restore();
  }

  @Override
  public void setAlpha(int alpha) {
    p.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter filter) {
    p.setColorFilter(filter);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
