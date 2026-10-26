package dev.dothome.widgets;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.*;
import org.json.JSONObject;

/** Async local-image rendering shared by Home and the non-destructive crop editor. */
final class PhotoView extends View {
  final TileContext a;
  final JSONObject item;
  final Paint paint = new Paint(3);
  Drawable image;
  boolean failed;
  float cropX, cropY, scale;
  float lastX, lastY;
  final boolean editing;
  final ScaleGestureDetector zoom;

  PhotoView(TileContext a, JSONObject item, boolean editing) {
    super(a);
    this.a = a;
    this.item = item;
    this.editing = editing;
    cropX = (float) item.optDouble("cropX", .5);
    cropY = (float) item.optDouble("cropY", Math.max(0, Math.min(2, item.optInt("crop", 1))) / 2f);
    scale = (float) item.optDouble("cropScale", 1);
    zoom =
        new ScaleGestureDetector(
            a,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
              @Override
              public boolean onScale(ScaleGestureDetector detector) {
                scale = Math.max(1, Math.min(4, scale * detector.getScaleFactor()));
                invalidate();
                return true;
              }
            });
    setContentDescription(editing ? "Drag photo to position; pinch to zoom" : "Photo widget");
    a.worker.execute(
        () -> {
          Drawable loaded = null;
          try {
            loaded = a.scaledImage(Uri.parse(PhotoCollection.uri(item)));
            if(item.optBoolean("wallpaper") && !item.optString("wallpaperEffect","none").equals("none") && loaded!=null){
              Bitmap source=Bitmap.createBitmap(Math.max(1,loaded.getIntrinsicWidth()),Math.max(1,loaded.getIntrinsicHeight()),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);loaded.setBounds(0,0,source.getWidth(),source.getHeight());loaded.draw(canvas);
              Bitmap effect=dev.dothome.ui.LocalWallpaperEffects.apply(source,item.optString("wallpaperEffect"));if(effect!=source)source.recycle();loaded=new android.graphics.drawable.BitmapDrawable(a.getResources(),effect);
            }
          } catch (Exception unavailable) {
          }
          Drawable result = loaded;
          post(
              () -> {
                image = result;
                failed = result == null;
                invalidate();
              });
        });
  }

  @Override
  protected void onDraw(Canvas canvas) {
    float w = getWidth(), h = getHeight();
    if (image == null) {
      paint.setColor(a.muted);
      paint.setTextSize(a.dp(12));
      canvas.drawText(failed ? "Photo unavailable" : "Loading photo…", a.dp(12), h / 2, paint);
      return;
    }
    int style = item.optInt("style");
    float iw = Math.max(1, image.getIntrinsicWidth()), ih = Math.max(1, image.getIntrinsicHeight());
    float factor = (style == 2 ? Math.min(w / iw, h / ih) : Math.max(w / iw, h / ih)) * scale;
    float dw = iw * factor, dh = ih * factor;
    float x = dw > w ? (w - dw) * cropX : (w - dw) / 2;
    float y = dh > h ? (h - dh) * cropY : (h - dh) / 2;
    canvas.save();
    if (style != 2) {
      Path mask = new Path();
      if (style == 1) mask.addCircle(w / 2, h / 2, Math.min(w, h) / 2, Path.Direction.CW);
      else if(!item.optBoolean("wallpaper")) mask.addRoundRect(new RectF(0, 0, w, h), a.dp(20), a.dp(20), Path.Direction.CW);
      canvas.clipPath(mask);
    }
    image.setBounds(Math.round(x), Math.round(y), Math.round(x + dw), Math.round(y + dh));
    if(style==2 && item.optBoolean("stickerOutline",true)) {
      // Dilate the image alpha with neighbouring silhouettes, retaining transparent edges.
      android.graphics.PorterDuffColorFilter white=new android.graphics.PorterDuffColorFilter(Color.WHITE,android.graphics.PorterDuff.Mode.SRC_IN);
      image.setColorFilter(white);float stroke=a.dp(2.5f);
      for(int n=0;n<12;n++){double angle=n*Math.PI/6;canvas.save();canvas.translate((float)Math.cos(angle)*stroke,(float)Math.sin(angle)*stroke);image.draw(canvas);canvas.restore();}
      image.clearColorFilter();
    }
    image.draw(canvas);
    canvas.restore();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!editing) return false;
    zoom.onTouchEvent(event);
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      lastX = event.getX();
      lastY = event.getY();
      return true;
    }
    if (event.getActionMasked() == MotionEvent.ACTION_MOVE
        && !zoom.isInProgress()
        && image != null) {
      float sx = getWidth() / (float) image.getIntrinsicWidth();
      float sy = getHeight() / (float) image.getIntrinsicHeight();
      float factor = (item.optInt("style") == 2 ? Math.min(sx, sy) : Math.max(sx, sy)) * scale;
      float extraX = image.getIntrinsicWidth() * factor - getWidth();
      float extraY = image.getIntrinsicHeight() * factor - getHeight();
      if (extraX > 0) cropX = Math.max(0, Math.min(1, cropX - (event.getX() - lastX) / extraX));
      if (extraY > 0) cropY = Math.max(0, Math.min(1, cropY - (event.getY() - lastY) / extraY));
      invalidate();
    }
    lastX = event.getX();
    lastY = event.getY();
    return true;
  }
}
