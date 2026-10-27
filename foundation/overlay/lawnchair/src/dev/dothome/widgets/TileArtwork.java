package dev.dothome.widgets;

import android.graphics.*;
import android.view.*;
import java.text.*;
import java.util.*;
import org.json.*;

/** Shared preview/live artwork, with separate renderers for each local widget family. */
final class TileArtwork extends View {
  final TileContext a;
  final JSONObject item;
  final boolean preview;
  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  final RectF r = new RectF();

  TileArtwork(TileContext a, JSONObject x, boolean preview) {
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
    p.setTypeface(mono ? TileTypography.mono(a) : TileTypography.regular(a));
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
    paper = TileTheme.widgetSurface(a);
    var appearance=dev.dothome.ui.DotAppearance.get(a);
    ink = appearance.ink;
    secondary = appearance.secondary;
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
    if (!preview && ((type.equals("torch") && a.torch)||(type.equals("dnd") && a.dnd)||(type.equals("sound") && a.ringer!=android.media.AudioManager.RINGER_MODE_NORMAL))) {
      paper = appearance.activeControl;
      ink = appearance.onAccent;
    }
    p.setStyle(Paint.Style.FILL);
    p.setColor(paper);
    r.set(0, 0, w, h);
    if(a.settings.optString("material").equals("translucent") && !type.equals("photo")) {
      c.save();Path mask=new Path();
      if(control || (type.equals("clock") && (style==1 || style==3)))mask.addCircle(w/2,h/2,u*(control?.44f:.46f),Path.Direction.CW);
      else mask.addRoundRect(r,a.dp(19),a.dp(19),Path.Direction.CW);
      c.clipPath(mask);dev.dothome.ui.DotBackdrop.glass(this,c);c.restore();
    }
    if (control || (type.equals("clock") && (style == 1 || style == 3)))
      c.drawCircle(w / 2, h / 2, u * (control ? .44f : .46f), p);
    else if (!type.equals("controls") && !(type.equals("photo") && style == 2))
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
          p.setColor(a.accent);
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
            w * .62f,
            pad + size,
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
      case "media": {
        TileMedia media=preview?new TileMedia(null,"Now playing","Artist",null,false):TileMedia.read(a);
        p.setColor(inverted ? a.card : a.bg);
        r.set(pad,pad,pad+h*.40f,pad+h*.40f);
        c.drawRoundRect(r,a.dp(10),a.dp(10),p);
        if(media.artwork!=null && !media.artwork.isRecycled()) {
          c.save();Path clip=new Path();clip.addRoundRect(r,a.dp(10),a.dp(10),Path.Direction.CW);c.clipPath(clip);
          c.drawBitmap(media.artwork,null,r,p);c.restore();
        } else text(c,"♫",pad+h*.12f,pad+h*.30f,h*.26f,ink,false);
        float left=pad+h*.49f;
        text(c,fit(media.title,size,w-left-pad),left,pad+size, size,ink,false);
        text(c,fit(media.artist,size*.78f,w-left-pad),left,pad+size*2.45f,size*.78f,secondary,false);
        if(preview) text(c,"‹       Ⅱ       ›",pad,h-pad,Math.min(w*.095f,h*.2f),ink,false);
        break;
      }
      case "away": {
        AwayTimeStats stats=a.away;
        String value=preview?"3h 19m":stats.available?AwayTimeStats.duration(stats.longest):"Set up";
        if(h<w*.7f || item.optInt("w")==1){
          serif(c,value,pad,h*.52f,Math.min(w*.20f,h*.40f),ink);
          text(c,"Away",pad,h*.80f,Math.min(a.dp(11),h*.20f),secondary,false);
        }else{
          float barY=h*.14f,barH=h*.16f,barW=(w-pad*2)/7f;
          long max=1;for(long v:stats.daily)max=Math.max(max,v);
          for(int n=0;n<7;n++){float ratio=preview?new float[]{.4f,.7f,.5f,.8f,.4f,.9f,.6f}[n]:stats.available?stats.daily[n]/(float)max:0;p.setColor(dev.dothome.ui.DotAppearance.alpha(a.accent,60+(int)(195*ratio)));c.drawRoundRect(new RectF(pad+n*barW,barY,pad+(n+1)*barW-a.dp(3),barY+barH),a.dp(2),a.dp(2),p);}
          text(c,"Away",pad,h*.67f,Math.min(a.dp(11),h*.14f),secondary,false);
          serif(c,value,pad,h*.87f,Math.min(w*.20f,h*.20f),ink);
        }break;
      }
      case "breathing":
        BreathingSession session=new BreathingSession(item,System.currentTimeMillis());
        float progress=preview || appearance.reducedMotion || item.optLong("breathStarted")==0?.5f:session.expansion;
        text(c, BreathingSession.NAMES[session.mode], pad, pad + size * .45f, Math.min(size, a.dp(10)), secondary, false);
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
      case "controls":
        for(int n=0;n<4;n++){float controlX=w*(n%2==0?.25f:.75f),controlY=h*(n<2?.25f:.75f),rad=Math.min(w,h)*.21f;p.setColor(paper);c.drawCircle(controlX,controlY,rad,p);drawControl(c,new String[]{"bluetooth","sound","torch","dnd"}[n],controlX,controlY,rad*.55f,ink);}break;
      case "torch":
      case "bluetooth":
      case "sound":
      case "dnd":
        drawControl(c, type, w / 2, h / 2, u * .24f, ink);
        break;
      case "weather": {
        String temperature=item.optString("temperature");
        String condition=item.optString("condition","Sunny");
        if(temperature.isEmpty()) {
          float size=Math.min(a.dp(16),h*.26f);
          text(c,"Weather",pad,h*.36f,Math.min(a.dp(11),h*.17f),secondary,false);
          p.setTypeface(TileTypography.regular(a));
          text(c,fit("Set up",size,w-pad*2),pad,h*.68f,size,ink,false);
        } else if(h<w*.7f){
          String value=temperature+"°";float size=Math.min(w*.23f,h*.55f);
          p.setTypeface(TileTypography.display(a));p.setTextSize(size);float measured=p.measureText(value);
          if(measured>w*.60f-pad)size*=Math.max(.3f,(w*.60f-pad)/measured);
          serif(c,value,pad,h*.65f,size,ink);weather(c,condition,w*.79f,h*.48f,h*.25f,ink);
        } else {weather(c,condition,w*.5f,h*.35f,u*.22f,ink);serif(c,temperature+"°",pad,h*.78f,u*.28f,ink);p.setTypeface(TileTypography.regular(a));float size=Math.min(a.dp(12),w*.075f);text(c,fit(item.optString("location",preview?"Local weather":"Enter weather"),size,w-pad*2),pad,h-pad,size,secondary,false);}
        if(!preview){String updated=item.optLong("weatherUpdated")==0?"Manual entry":new java.text.SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date(item.optLong("weatherUpdated")));if(h<w*.7f)text(c,updated,pad,h-a.dp(5),Math.min(a.dp(8),h*.13f),secondary,false);}
        break;
      }
      case "steps": {
        String count=preview?"4200":a.steps>=0?String.valueOf(a.steps):"—";
        serif(c,count,pad,h*.56f,Math.min(w*.29f,h*.42f),ink);
        text(c,"Measured steps",pad,h*.78f,Math.min(a.dp(12),h*.16f),secondary,false);
        if(h>w*.7f){int goal=Math.max(1,item.optInt("stepGoal",6000));p.setColor(appearance.outline);c.drawRoundRect(new RectF(pad,h*.86f,w-pad,h*.89f),a.dp(3),a.dp(3),p);p.setColor(a.accent);c.drawRoundRect(new RectF(pad,h*.86f,pad+(w-pad*2)*Math.min(1f,(preview?4200:Math.max(0,a.steps))/(float)goal),h*.89f),a.dp(3),a.dp(3),p);}break;
      }
      case "headphones": {
        String connected="No audio device";
        if(preview)connected="Headphones";
        else for(android.media.AudioDeviceInfo device:a.getSystemService(android.media.AudioManager.class).getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS))if(device.getType()==android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||device.getType()==android.media.AudioDeviceInfo.TYPE_BLE_HEADSET||device.getType()==android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES){connected=device.getProductName().toString();break;}
        p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(u*.055f);r.set(w*.28f,h*.16f,w*.72f,h*.62f);c.drawArc(r,180,180,false,p);c.drawLine(w*.28f,h*.39f,w*.28f,h*.63f,p);c.drawLine(w*.72f,h*.39f,w*.72f,h*.63f,p);p.setStyle(Paint.Style.FILL);
        paragraph(c,connected,pad,h*.70f,w-pad*2,h*.15f,Math.min(a.dp(12),w*.08f),ink);
        text(c,"Battery not available",pad,h-pad,Math.min(a.dp(9),w*.065f),secondary,false);break;
      }
      case "compass":
        float degrees = preview ? 315 : a.heading;
        if(!preview && !a.compassAvailable){paragraph(c,"Compass unavailable",pad,h*.35f,w-pad*2,h*.4f,size,ink);break;}
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
        if(!preview&&a.sensorAccuracy==android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE)text(c,"Move in a figure eight",pad,pad+a.dp(10),Math.min(a.dp(10),w*.065f),secondary,false);
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
                                : Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),java.time.Instant.ofEpochMilli(item.optLong("deadline")).atZone(java.time.ZoneId.systemDefault()).toLocalDate()))
                                    + " days")
                            : "Tap to configure";
        text(c, label, pad, pad + size * .7f, Math.min(size, a.dp(11)), secondary, true);
        float valueSize = Math.min(a.dp(22), Math.min(w * .12f, h * .27f));
        if(type.equals("notes") || type.equals("agenda")) paragraph(c,value,pad,h*.26f,w-pad*2,h*.65f-pad,valueSize,ink);
        else text(c, fit(value, valueSize, w - pad * 2), pad, h * .66f, valueSize, ink, false);
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
              ? 0
              : type.equals("clock") || (type.equals("timer") && item.optLong("timerStart") > 0)
                  ? 1000
                  : type.equals("compass") ? 120 : 0;
      if(type.equals("breathing") && item.optLong("breathStarted")>0 && !appearance.reducedMotion) postInvalidateOnAnimation();
      else if (delay > 0) postInvalidateDelayed(delay);
    }
  }

  void weather(Canvas canvas,String condition,float cx,float cy,float radius,int colour) {
    p.setColor(colour);float step=radius/5;
    for(int y=-5;y<=5;y++)for(int x=-6;x<=6;x++){
      boolean cloud=(x*x/36f+y*y/10f<1&&y>=-1)||((x+2)*(x+2)+(y+1)*(y+1)<8)||((x-1)*(x-1)+(y+2)*(y+2)<12);
      boolean sun=x*x+y*y<=13;
      boolean dot=condition.equals("Sunny")||condition.equals("Clear night")?sun:cloud;
      if(condition.equals("Clear night")&&(x-2)*(x-2)+(y+2)*(y+2)<12)dot=false;
      if((condition.equals("Rain")||condition.equals("Snow"))&&y>2&&y<6&&((x+y)%4==0))dot=true;
      if(dot)canvas.drawCircle(cx+x*step,cy+y*step,step*.36f,p);
    }
  }

  void paragraph(Canvas c,String value,float x,float y,float width,float height,float size,int colour) {
    if(width<1 || height<1)return;
    android.text.TextPaint paint=new android.text.TextPaint(3);paint.setColor(colour);paint.setTextSize(size);paint.setTypeface(TileTypography.regular(a));
    android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain(value,0,value.length(),paint,Math.max(1,(int)width)).setIncludePad(false).setMaxLines(Math.max(1,(int)(height/(size*1.25f)))).setEllipsize(android.text.TextUtils.TruncateAt.END).setLineSpacing(0,1.15f).build();
    c.save();c.translate(x,y);c.clipRect(0,0,width,height);layout.draw(c);c.restore();
  }

  void serif(Canvas c, String value, float x, float y, float size, int color) {
    p.setStyle(Paint.Style.FILL);
    p.setColor(color);
    p.setTypeface(TileTypography.display(a));
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
