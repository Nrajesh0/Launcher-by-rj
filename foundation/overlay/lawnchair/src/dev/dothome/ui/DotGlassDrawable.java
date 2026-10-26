package dev.dothome.ui;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
/** Same wallpaper material for the dock field, folder/editor previews and owned widgets. */
public final class DotGlassDrawable extends Drawable {
  private final java.lang.ref.WeakReference<View> view;
  private final float radius;
  private int alpha=255;
  public DotGlassDrawable(View view,float radius){this.view=new java.lang.ref.WeakReference<>(view);this.radius=radius;}
  @Override public void draw(Canvas canvas){View v=view.get();if(v==null)return;var a=DotAppearance.get(v.getContext());RectF b=new RectF(getBounds());Paint p=new Paint(3);Path clip=new Path();float r=Math.min(DotUi.dp(v.getContext(),radius),b.height()/2);clip.addRoundRect(b,r,r,Path.Direction.CW);canvas.save();canvas.clipPath(clip);if(a.glass)DotBackdrop.glass(v,canvas);p.setColor(a.surface);p.setAlpha(Color.alpha(a.surface)*alpha/255);canvas.drawRect(b,p);p.setColor(DotAppearance.alpha(a.dark?Color.WHITE:Color.WHITE,a.dark?20:65));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(DotUi.dp(v.getContext(),.65f));canvas.drawRoundRect(b,r,r,p);canvas.restore();}
  @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
  @Override public void setColorFilter(ColorFilter filter){}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
