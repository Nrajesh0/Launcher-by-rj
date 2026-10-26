package dev.dothome.ui;
import android.graphics.*;

/** Effects operate only on a selected local image, never a captured screen. */
public final class LocalWallpaperEffects {
  public static Bitmap apply(Bitmap source,String effect){
    if(effect.equals("atmosphere"))return atmosphere(source);if(!effect.equals("glass"))return source;
    int w=source.getWidth(),h=source.getHeight();Bitmap output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(output);Paint p=new Paint(3);float cell=Math.max(4,w/24f);
    for(float left=0;left<w;left+=cell){float right=Math.min(w,left+cell),offset=cell*.55f;Rect src=new Rect(Math.max(0,(int)(left-offset)),0,Math.min(w,Math.max((int)(left+1),(int)(right+offset))),h);RectF dst=new RectF(left,0,right,h);c.drawBitmap(source,src,dst,p);p.setShader(new LinearGradient(left,0,right,0,new int[]{0x45ffffff,0x00ffffff,0x24000000},null,Shader.TileMode.CLAMP));c.drawRect(dst,p);p.setShader(null);}return output;
  }
  public static Bitmap atmosphere(Bitmap source){
    int w=source.getWidth(),h=source.getHeight(),mw=72,mh=Math.max(1,Math.min(512,Math.round(h*72f/w)));Bitmap mini=Bitmap.createScaledBitmap(source,mw,mh,true);int[] pixels=new int[mw*mh];mini.getPixels(pixels,0,mw,0,0,mw,mh);if(mini!=source)mini.recycle();
    for(int pass=0;pass<3;pass++){int[] from=pixels.clone();for(int y=0;y<mh;y++)for(int x=0;x<mw;x++){int r=0,g=0,b=0,count=0;for(int yy=Math.max(0,y-3);yy<=Math.min(mh-1,y+3);yy++)for(int xx=Math.max(0,x-3);xx<=Math.min(mw-1,x+3);xx++){int colour=from[yy*mw+xx];r+=Color.red(colour);g+=Color.green(colour);b+=Color.blue(colour);count++;}pixels[y*mw+x]=Color.rgb(r/count,g/count,b/count);}}
    Bitmap blurred=Bitmap.createBitmap(pixels,mw,mh,Bitmap.Config.ARGB_8888);Bitmap output=Bitmap.createScaledBitmap(blurred,w,h,true);if(output!=blurred)blurred.recycle();return output;
  }
}
