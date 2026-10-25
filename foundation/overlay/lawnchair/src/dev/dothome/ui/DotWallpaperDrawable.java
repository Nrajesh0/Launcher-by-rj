package dev.dothome.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Bake static optical detail once; widget motion must not redraw hundreds of wallpaper paths. */
public final class DotWallpaperDrawable extends Drawable {
  private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
  private static final Handler MAIN=new Handler(Looper.getMainLooper());
  public final String key;
  private Bitmap image;
  private int requestedWidth,requestedHeight,generation;
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  public static boolean darkSystemIcons(android.content.Context context,boolean fallback) {
    var style=context.getSharedPreferences("dot-style",0);
    if(style.getBoolean("wallpaperApplied",false))return fallback;
    String key=style.getString("wallpaper","glass");return !key.equals("optic")&&!key.equals("graphite");
  }
  public DotWallpaperDrawable(String key) {this.key=key;}
  @Override public void draw(Canvas canvas) {
    Rect bounds=getBounds();
    if(image!=null && !image.isRecycled()) canvas.drawBitmap(image,null,bounds,paint);
    else DotWallpaper.INSTANCE.paint(canvas,bounds.width(),bounds.height(),key);
    if(bounds.width()>0 && bounds.height()>0 && (requestedWidth!=bounds.width() || requestedHeight!=bounds.height())) {
      requestedWidth=bounds.width();requestedHeight=bounds.height();
      final int width=requestedWidth,height=requestedHeight,version=++generation;
      WeakReference<DotWallpaperDrawable> reference=new WeakReference<>(this);
      String preset=key;
      WORK.execute(()->{
        Bitmap result;
        try {result=DotWallpaper.INSTANCE.bitmap(preset,width,height);}catch(OutOfMemoryError|RuntimeException unavailable){return;}
        MAIN.post(()->{
          DotWallpaperDrawable target=reference.get();
          if(target==null || target.generation!=version) {result.recycle();return;}
          // The renderer may still have submitted the previous bitmap; let GC reclaim it.
          target.image=result;target.invalidateSelf();
        });
      });
    }
  }
  @Override public void setAlpha(int alpha) {paint.setAlpha(alpha);invalidateSelf();}
  @Override public void setColorFilter(ColorFilter filter) {paint.setColorFilter(filter);invalidateSelf();}
  @Override public int getOpacity() {return PixelFormat.OPAQUE;}
}
