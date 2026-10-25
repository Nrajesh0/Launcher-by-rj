package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.*;
import org.json.JSONObject;

/** Async local-image rendering shared by Home and the non-destructive crop editor. */
final class PhotoView extends View {
  final HomeActivity a;
  final JSONObject item;
  final Paint paint = new Paint(3);
  Drawable image;
  boolean failed;
  float cropX, cropY, scale;
  float lastX, lastY;
  final boolean editing;
  final ScaleGestureDetector zoom;

  PhotoView(HomeActivity a, JSONObject item, boolean editing) {
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
            loaded = a.scaledImage(Uri.parse(item.optString("uri")));
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
      else mask.addRoundRect(new RectF(0, 0, w, h), a.dp(20), a.dp(20), Path.Direction.CW);
      canvas.clipPath(mask);
    }
    image.setBounds(Math.round(x), Math.round(y), Math.round(x + dw), Math.round(y + dh));
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
      float factor =
          Math.max(
                  getWidth() / (float) image.getIntrinsicWidth(),
                  getHeight() / (float) image.getIntrinsicHeight())
              * scale;
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
