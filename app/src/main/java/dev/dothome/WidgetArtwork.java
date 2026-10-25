package dev.dothome;

import android.graphics.*;
import android.view.*;
import java.text.*;
import java.util.*;
import org.json.*;

/** Shared preview/live artwork, with separate renderers for each local widget family. */
final class WidgetArtwork extends View {
  final HomeActivity a;
  final JSONObject item;
  final boolean preview;
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  final RectF r = new RectF();

  WidgetArtwork(HomeActivity a, JSONObject x, boolean preview) {
    super(a);
    this.a = a;
    item = x;
    this.preview = preview;
    setLayerType(View.LAYER_TYPE_HARDWARE, null);
    setContentDescription(x.optString("type") + " widget");
  }

  int ink, paper, secondary;
  float w, h, u;

  void text(Canvas c, String value, float x, float y, float size, int color, boolean mono) {
    p.setColor(color);
    p.setStyle(Paint.Style.FILL);
    p.setTypeface(mono ? Typeface.create("monospace", 0) : LauncherTypography.medium(a));
    p.setTextSize(size);
    c.drawText(value, x, y, p);
  }

  String fit(String value, float size, float max) {
    p.setTextSize(size);
    if (p.measureText(value) <= max) return value;
    while (value.length() > 1 && p.measureText(value + "…") > max)
      value = value.substring(0, value.length() - 1);
    return value + "…";
  }

  @Override
  protected void onDraw(Canvas c) {
    w = getWidth();
    h = getHeight();
    u = Math.min(w, h);
    int style = item.optInt("style");
    String type = item.optString("type");
    boolean inverted = style == 1 && !type.equals("clock") && !type.equals("battery");
    paper = LauncherTheme.widgetSurface(a);
    ink = a.dark() ? 0xfff0f0e9 : 0xff151813;
    secondary = a.dark() ? 0xffb8bbb0 : 0xff66705f;
    if (inverted) {
      paper = a.dark() ? 0xfff0f2eb : 0xff202120;
      ink = a.dark() ? 0xff151813 : 0xfff0f0e9;
      secondary = ink;
    }
    int tone = item.optInt("tone");
    if (tone > 0) {
      paper = new int[] {a.card, 0xffeeeede, 0xff181a1c, 0xffb7ca9c, 0xffcc292d}[Math.min(4, tone)];
      ink = tone == 2 || tone == 4 ? 0xfff8f6ee : 0xff171b18;
      secondary = ink;
    }
    int opacity = Math.max(0, Math.min(100, item.optInt("opacity", 100)));
    paper = (paper & 0x00ffffff) | ((int) (Color.alpha(paper) * opacity / 100f) << 24);
    boolean control =
        Arrays.asList("torch", "bluetooth", "sound", "dnd").contains(type)
            || (type.equals("photo") && item.optInt("w", 1) == 1 && item.optInt("h", 1) == 1);
    if (type.equals("torch") && !preview && a.torch) {
      paper = 0xff8eb65b;
      ink = 0xfff8f7ee;
    }
    p.setStyle(Paint.Style.FILL);
    p.setColor(paper);
    r.set(0, 0, w, h);
    if (control || (type.equals("clock") && (style == 1 || style == 3)))
      c.drawCircle(w / 2, h / 2, u * (control ? .44f : .46f), p);
    else if (!(type.equals("photo") && style == 2))
      c.drawRoundRect(
          r,
          type.equals("battery") && h < w * .65f ? h / 2 : a.dp(19),
          type.equals("battery") && h < w * .65f ? h / 2 : a.dp(19),
          p);
    float pad = Math.min(a.dp(18), w * .12f), size = Math.min(a.dp(15), u * .16f);
    Date now = preview ? new GregorianCalendar(2026, 9, 25, 10, 8).getTime() : new Date();
    switch (type) {
      case "clock":
        if (style == 1 || style == 3) {
          float radius = u * .45f, centerX = w / 2, centerY = h / 2;
          Calendar time = Calendar.getInstance();
          time.setTime(now);
          if (style == 3) {
            p.setColor(ink);
            p.setStyle(Paint.Style.STROKE);
            for (int n = 0; n < 60; n++) {
              double angle = n * Math.PI / 30;
              float dx = (float) Math.sin(angle), dy = (float) -Math.cos(angle);
              p.setStrokeWidth(n % 5 == 0 ? a.dp(1.5f) : a.dp(.6f));
              c.drawLine(
                  centerX + dx * radius * (n % 5 == 0 ? .85f : .92f),
                  centerY + dy * radius * (n % 5 == 0 ? .85f : .92f),
                  centerX + dx * radius,
                  centerY + dy * radius,
                  p);
            }
          }
          p.setStyle(Paint.Style.STROKE);
          hand(
              c,
              centerX,
              centerY,
              radius * .43f,
              (time.get(Calendar.HOUR) % 12 + time.get(Calendar.MINUTE) / 60f) * 30,
              ink,
              style == 1 ? u * .075f : a.dp(2.5f));
          hand(
              c,
              centerX,
              centerY,
              radius * .72f,
              time.get(Calendar.MINUTE) * 6,
              ink,
              style == 1 ? u * .018f : a.dp(1.5f));
          if (style == 3)
            hand(
                c,
                centerX,
                centerY,
                radius * .76f,
                time.get(Calendar.SECOND) * 6,
                a.accent,
                a.dp(.8f));
          p.setStyle(Paint.Style.FILL);
          p.setColor(style == 1 ? 0xff708957 : a.accent);
          c.drawCircle(centerX, style == 1 ? centerY + radius * .83f : centerY, a.dp(2), p);
        } else {
          String value =
              new SimpleDateFormat(
                      android.text.format.DateFormat.is24HourFormat(a) ? "HH:mm" : "hh:mm",
                      Locale.US)
                  .format(now);
          if (style == 0) dots(c, value, pad, h * .22f, w - pad * 2, h * .5f, ink);
          else if (style == 4) segments(c, value, pad, h * .23f, w - pad * 2, h * .48f, ink);
          else serif(c, value, pad, h * .70f, Math.min(w * .24f, h * .60f), ink);
          text(
              c,
              new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now).toUpperCase(),
              pad,
              h - pad,
              Math.min(size, w * .055f),
              secondary,
              true);
        }
        break;
      case "date":
        text(
            c,
            new SimpleDateFormat("EEE", Locale.getDefault()).format(now).toUpperCase(),
            pad,
            pad + size,
            size,
            secondary,
            true);
        if (style == 2)
          serif(
              c,
              new SimpleDateFormat("dd", Locale.getDefault()).format(now),
              pad,
              h - pad,
              Math.min(h * .64f, w * .5f),
              ink);
        else
          text(
              c,
              new SimpleDateFormat("dd", Locale.getDefault()).format(now),
              pad,
              h - pad,
              Math.min(h * .55f, w * .47f),
              ink,
              false);
        text(
            c,
            new SimpleDateFormat("MMM", Locale.getDefault()).format(now).toUpperCase(),
            w * .58f,
            h - pad,
            size,
            ink,
            true);
        break;
      case "timer":
        long elapsed =
            item.optLong("elapsed")
                + (item.optLong("timerStart") > 0
                    ? Math.max(0, System.currentTimeMillis() - item.optLong("timerStart"))
                    : 0);
        long minutes = elapsed / 60000;
        String duration = (minutes >= 60 ? minutes / 60 + "h " : "") + minutes % 60 + "m";
        serif(c, duration, pad, h * .48f, Math.min(w * .14f, h * .32f), ink);
        text(
            c,
            item.optLong("timerStart") > 0 ? "Running" : "Timer",
            pad,
            h * .78f,
            Math.min(a.dp(10), h * .15f),
            secondary,
            false);
        p.setColor(0xff819665);
        p.setStrokeWidth(a.dp(1));
        p.setStyle(Paint.Style.STROKE);
        float sunX = w - pad * 1.3f, sunY = h * .35f;
        c.drawCircle(sunX, sunY, a.dp(3), p);
        for (int n = 0; n < 8; n++) {
          double angle = n * Math.PI / 4;
          c.drawLine(
              sunX + (float) Math.cos(angle) * a.dp(5),
              sunY + (float) Math.sin(angle) * a.dp(5),
              sunX + (float) Math.cos(angle) * a.dp(7),
              sunY + (float) Math.sin(angle) * a.dp(7),
              p);
        }
        p.setStyle(Paint.Style.FILL);
        break;
      case "battery":
        if (style == 1) {
          text(
              c,
              (preview ? 76 : a.battery) + "%",
              pad,
              h * .61f,
              Math.min(w * .34f, h * .48f),
              ink,
              false);
          text(
              c,
              !preview && a.charging ? "Charging" : "Battery",
              pad,
              h - pad,
              Math.min(a.dp(12), w * .1f),
              secondary,
              false);
          break;
        }
        if (style == 2) {
          float percentValue = preview ? 76 : a.battery;
          int columns = 18, rows = 4;
          float step = Math.min((w - pad * 2) / columns, (h - pad * 2) / rows);
          float left = (w - step * columns) / 2, top = (h - step * rows) / 2;
          for (int row = 0; row < rows; row++)
            for (int col = 0; col < columns; col++) {
              boolean full = col * 100f / columns < percentValue;
              p.setColor(
                  full
                      ? (percentValue <= 20 ? a.accent : ink)
                      : (a.dark() ? 0xff55564e : 0xffb5bdad));
              c.drawCircle(left + (col + .5f) * step, top + (row + .5f) * step, step * .34f, p);
            }
          break;
        }
        float percent = preview ? 76 : a.battery;
        float cx = w * .3f, cy = h * .52f, rr = Math.min(w * .2f, h * .29f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(a.dp(5));
        p.setColor(inverted ? 0x44333333 : 0x44555555);
        r.set(cx - rr, cy - rr, cx + rr, cy + rr);
        c.drawArc(r, 0, 360, false, p);
        p.setColor(ink);
        c.drawArc(r, -90, percent * 3.6f, false, p);
        p.setStyle(Paint.Style.FILL);
        text(
            c,
            Math.round(percent) + "%",
            w * .53f,
            h * .61f,
            Math.min(w * .19f, h * .3f),
            ink,
            false);
        if (w > a.dp(130) && h > a.dp(120))
          text(
              c,
              a.charging ? "CHARGING" : "BATTERY",
              pad,
              h - pad,
              Math.min(size, a.dp(10)),
              secondary,
              true);
        break;
      case "photo":
        if (style == 2) {
          drawControl(c, "photo", w / 2, h * .43f, u * .18f, ink);
          text(
              c,
              preview ? "YOUR STICKER" : "Choose a photo",
              pad,
              h - pad,
              Math.min(a.dp(12), w * .085f),
              ink,
              false);
          break;
        }
        if (control) {
          drawControl(c, "photo", w / 2, h / 2, u * .24f, ink);
          break;
        }
        p.setColor(inverted ? a.card : 0xff77736d);
        r.set(pad, pad, w - pad, h - pad);
        c.drawRoundRect(r, style == 1 ? u / 2 : a.dp(18), style == 1 ? u / 2 : a.dp(18), p);
        p.setColor(paper);
        Path landscape = new Path();
        landscape.moveTo(pad, h - pad);
        landscape.lineTo(w * .42f, h * .37f);
        landscape.lineTo(w * .64f, h * .65f);
        landscape.lineTo(w * .78f, h * .46f);
        landscape.lineTo(w - pad, h - pad);
        landscape.close();
        c.drawPath(landscape, p);
        p.setColor(ink);
        c.drawCircle(w * .7f, h * .3f, u * .06f, p);
        text(
            c,
            preview ? "PHOTO PREVIEW" : "Choose a photo",
            pad,
            h - pad,
            Math.min(size, a.dp(11)),
            ink,
            true);
        break;
      case "media":
        p.setColor(inverted ? a.card : a.bg);
        r.set(pad, pad, pad + h * .46f, pad + h * .46f);
        c.drawRoundRect(r, a.dp(12), a.dp(12), p);
        text(c, "♫", pad + h * .13f, pad + h * .34f, h * .3f, ink, false);
        text(c, "Now playing", pad + h * .55f, pad + size, Math.min(size, w * .07f), ink, false);
        text(
            c,
            "Your music",
            pad + h * .55f,
            pad + size * 2.5f,
            Math.min(size, w * .06f),
            secondary,
            false);
        text(c, "‹       Ⅱ       ›", pad, h - pad, Math.min(w * .095f, h * .2f), ink, false);
        break;
      case "away":
        text(c, "Away time", pad, pad + size * .4f, Math.min(size, a.dp(12)), secondary, false);
        AwayTimeStats stats = a.away;
        String longest =
            preview
                ? "3h 19m"
                : stats.available ? AwayTimeStats.duration(stats.longest) : "No history";
        text(c, longest, pad, h * .47f, Math.min(w * .18f, h * .23f), ink, false);
        text(
            c,
            "Screen-off breaks · 7 days",
            pad,
            h * .62f,
            Math.min(a.dp(10), w * .06f),
            secondary,
            false);
        long max = 1;
        for (long total : stats.daily) max = Math.max(max, total);
        for (int n = 0; n < 7; n++) {
          float ratio =
              preview
                  ? new float[] {.4f, .65f, .5f, .8f, .35f, .9f, .6f}[n]
                  : stats.available ? stats.daily[n] / (float) max : 0;
          float bw = (w - pad * 2) / 10;
          p.setColor(ink);
          p.setAlpha(stats.available || preview ? 180 : 50);
          float left = pad + n * (w - pad * 2) / 7;
          c.drawRoundRect(
              new RectF(left, h - pad - h * .2f * ratio - 2, left + bw, h - pad),
              bw / 2,
              bw / 2,
              p);
        }
        p.setAlpha(255);
        break;
      case "breathing":
        float progress =
            preview || a.settings.optBoolean("reduceMotion", false)
                ? .5f
                : (float) ((Math.sin(System.currentTimeMillis() / 2600.0) + 1) / 2);
        text(c, "Focus", pad, pad + size * .45f, Math.min(size, a.dp(10)), secondary, false);
        p.setColor(ink);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(a.dp(.8f));
        float radius = u * (.33f + progress * .07f);
        for (int n = 0; n < 9; n++) {
          float factor = .15f + n * .1f;
          r.set(
              w / 2 - radius * factor,
              h * .52f - radius,
              w / 2 + radius * factor,
              h * .52f + radius);
          c.drawOval(r, p);
        }
        p.setStyle(Paint.Style.FILL);
        break;
      case "torch":
      case "bluetooth":
      case "sound":
      case "dnd":
        drawControl(c, type, w / 2, h / 2, u * .24f, ink);
        break;
      case "compass":
        float degrees = preview ? 315 : a.heading;
        float rad = u * .29f;
        p.setColor(ink);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(a.dp(1));
        c.drawCircle(w / 2, h * .45f, rad, p);
        p.setStyle(Paint.Style.FILL);
        c.save();
        c.rotate(-degrees, w / 2, h * .45f);
        p.setColor(a.accent);
        Path arrow = new Path();
        arrow.moveTo(w / 2, h * .45f - rad * .8f);
        arrow.lineTo(w / 2 - rad * .18f, h * .45f);
        arrow.lineTo(w / 2 + rad * .18f, h * .45f);
        arrow.close();
        c.drawPath(arrow, p);
        c.restore();
        text(c, Math.round(degrees) + "°", pad, h - pad, Math.min(size * 1.5f, w * .2f), ink, true);
        break;
      default:
        String label =
            type.equals("agenda")
                ? "QUICK LOOK"
                : type.equals("usage") ? "SCREEN TIME" : type.toUpperCase();
        String value =
            type.equals("notes")
                ? item.optString(
                    "text", preview ? "Make room for what matters." : "Tap to write a note")
                : type.equals("agenda")
                    ? (preview ? "10:30 · Your next event" : a.agenda)
                    : type.equals("usage")
                        ? (preview ? "1h 24m" : a.usage)
                        : type.equals("countdown")
                            ? (item.optLong("deadline") == 0
                                ? "Set a date"
                                : Math.max(
                                        0,
                                        (item.optLong("deadline")
                                                - System.currentTimeMillis()
                                                + 86399999)
                                            / 86400000)
                                    + " days")
                            : "Tap to configure";
        text(c, label, pad, pad + size * .7f, Math.min(size, a.dp(11)), secondary, true);
        float valueSize = Math.min(a.dp(22), Math.min(w * .12f, h * .27f));
        text(c, fit(value, valueSize, w - pad * 2), pad, h * .66f, valueSize, ink, false);
        if (type.equals("countdown") && h > a.dp(100))
          text(
              c,
              item.optString("name", "A day to remember"),
              pad,
              h - pad,
              size * .8f,
              secondary,
              false);
    }
    if (!preview
        && a.foreground
        && isShown()
        && item.optInt("page", a.page) == a.page
        && getGlobalVisibleRect(new Rect())) {
      int delay =
          type.equals("breathing") && !a.settings.optBoolean("reduceMotion", false)
              ? 40
              : type.equals("clock") || (type.equals("timer") && item.optLong("timerStart") > 0)
                  ? 1000
                  : type.equals("compass") ? 120 : 0;
      if (delay > 0) postInvalidateDelayed(delay);
    }
  }

  void serif(Canvas c, String value, float x, float y, float size, int color) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(color);
    p.setTypeface(Typeface.create("serif", Typeface.NORMAL));
    p.setTextSize(size);
    c.drawText(value, x, y, p);
  }

  void drawControl(Canvas c, String type, float x, float y, float radius, int color) {
    c.save();
    c.translate(x, y);
    p.setColor(color);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(Math.max(a.dp(1.5f), radius * .1f));
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setStrokeJoin(Paint.Join.ROUND);
    Path path = new Path();
    if (type.equals("photo")) {
      r.set(-radius * .6f, -radius * .72f, radius * .6f, radius * .72f);
      c.drawRect(r, p);
      c.drawCircle(-radius * .2f, -radius * .25f, radius * .1f, p);
      path.moveTo(-radius * .5f, radius * .5f);
      path.lineTo(0, 0);
      path.lineTo(radius * .5f, radius * .5f);
      c.drawPath(path, p);
    } else if (type.equals("torch")) {
      path.moveTo(-radius * .43f, -radius * .72f);
      path.lineTo(radius * .43f, -radius * .72f);
      path.lineTo(radius * .25f, -radius * .2f);
      path.lineTo(radius * .22f, radius * .72f);
      path.lineTo(-radius * .22f, radius * .72f);
      path.lineTo(-radius * .25f, -radius * .2f);
      path.close();
      c.drawPath(path, p);
      c.drawLine(-radius * .35f, -radius * .48f, radius * .35f, -radius * .48f, p);
      c.drawCircle(0, radius * .1f, radius * .07f, p);
    } else if (type.equals("bluetooth")) {
      path.moveTo(0, -radius * .8f);
      path.lineTo(radius * .5f, -radius * .4f);
      path.lineTo(-radius * .5f, radius * .4f);
      path.moveTo(-radius * .5f, -radius * .4f);
      path.lineTo(radius * .5f, radius * .4f);
      path.lineTo(0, radius * .8f);
      path.close();
      c.drawPath(path, p);
    } else if (type.equals("dnd")) {
      c.drawCircle(0, 0, radius * .68f, p);
      c.drawLine(-radius * .35f, 0, radius * .35f, 0, p);
    } else {
      path.moveTo(-radius * .65f, -radius * .2f);
      path.lineTo(-radius * .3f, -radius * .2f);
      path.lineTo(radius * .13f, -radius * .65f);
      path.lineTo(radius * .13f, radius * .65f);
      path.lineTo(-radius * .3f, radius * .2f);
      path.lineTo(-radius * .65f, radius * .2f);
      path.close();
      c.drawPath(path, p);
      r.set(radius * .05f, -radius * .5f, radius * .8f, radius * .5f);
      c.drawArc(r, -60, 120, false, p);
    }
    p.setStyle(Paint.Style.FILL);
    c.restore();
  }

  void segments(
      Canvas c, String value, float left, float top, float width, float height, int color) {
    int[] mask = {0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f};
    float cell = Math.min(width / 4.7f, height * .57f),
        digitHeight = Math.min(height, cell * 1.7f),
        stroke = cell * .16f;
    float total = cell * 4.7f;
    left += (width - total) / 2;
    float[][] lines = {
      {.18f, 0, .82f, 0},
      {1, .12f, 1, .42f},
      {1, .58f, 1, .88f},
      {.18f, 1, .82f, 1},
      {0, .58f, 0, .88f},
      {0, .12f, 0, .42f},
      {.18f, .5f, .82f, .5f}
    };
    p.setColor(color);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(stroke);
    p.setStrokeCap(Paint.Cap.ROUND);
    for (char ch : value.toCharArray()) {
      if (ch == ':') {
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(left + cell * .1f, top + digitHeight * .32f, stroke * .55f, p);
        c.drawCircle(left + cell * .1f, top + digitHeight * .7f, stroke * .55f, p);
        p.setStyle(Paint.Style.STROKE);
        left += cell * .3f;
        continue;
      }
      for (int n = 0; n < 7; n++)
        if ((mask[ch - '0'] & (1 << n)) != 0) {
          float[] line = lines[n];
          c.drawLine(
              left + line[0] * cell * .8f,
              top + line[1] * digitHeight,
              left + line[2] * cell * .8f,
              top + line[3] * digitHeight,
              p);
        }
      left += cell * 1.1f;
    }
    p.setStyle(Paint.Style.FILL);
  }

  void hand(Canvas c, float x, float y, float length, float angle, int color, float stroke) {
    p.setColor(color);
    p.setStrokeWidth(stroke);
    p.setStrokeCap(Paint.Cap.ROUND);
    c.drawLine(
        x,
        y,
        x + (float) Math.sin(Math.toRadians(angle)) * length,
        y - (float) Math.cos(Math.toRadians(angle)) * length,
        p);
  }

  void dots(Canvas c, String value, float x, float y, float width, float height, int color) {
    String[] patterns = {
      "111101101101111",
      "010110010010111",
      "111001111100111",
      "111001111001111",
      "101101111001001",
      "111100111001111",
      "111100111101111",
      "111001001001001",
      "111101111101111",
      "111101111001111"
    };
    float unit = Math.min(width / 18f, height / 5);
    for (char ch : value.toCharArray()) {
      int columns = ch == ':' ? 1 : 3;
      for (int row = 0; row < 5; row++)
        for (int col = 0; col < columns; col++) {
          boolean on =
              ch == ':' ? (row == 1 || row == 3) : patterns[ch - '0'].charAt(row * 3 + col) == '1';
          p.setStyle(Paint.Style.FILL);
          p.setColor(on ? (ch == ':' ? a.accent : color) : 0x22555555);
          c.drawCircle(x + (col + .5f) * unit, y + (row + .5f) * unit, unit * .29f, p);
        }
      x += (columns + 1) * unit;
    }
  }
}
