package dev.dothome.widgets;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Original transport geometry, with 48dp targets and a shared pressed state. */
final class TransportView extends View {
  final int action;final boolean playing;final int ink;final Paint paint=new Paint(3);
  TransportView(Context context,int action,boolean playing,int ink){super(context);this.action=action;this.playing=playing;this.ink=ink;setFocusable(true);setBackground(dev.dothome.ui.DotUi.clickable(context,Color.TRANSPARENT,24));}
  @Override protected void onDraw(Canvas canvas){float s=Math.min(getWidth(),getHeight()),cx=getWidth()/2f,cy=getHeight()/2f;paint.setColor(ink);
    if(action==0&&playing){canvas.drawRoundRect(cx-s*.17f,cy-s*.21f,cx-s*.04f,cy+s*.21f,s*.025f,s*.025f,paint);canvas.drawRoundRect(cx+s*.04f,cy-s*.21f,cx+s*.17f,cy+s*.21f,s*.025f,s*.025f,paint);return;}
    canvas.save();if(action<0)canvas.scale(-1,1,cx,cy);Path triangle=new Path();triangle.moveTo(cx-s*.13f,cy-s*.21f);triangle.lineTo(cx+s*.19f,cy);triangle.lineTo(cx-s*.13f,cy+s*.21f);triangle.close();canvas.drawPath(triangle,paint);if(action!=0)canvas.drawRoundRect(cx+s*.20f,cy-s*.21f,cx+s*.26f,cy+s*.21f,s*.02f,s*.02f,paint);canvas.restore();
  }
}
