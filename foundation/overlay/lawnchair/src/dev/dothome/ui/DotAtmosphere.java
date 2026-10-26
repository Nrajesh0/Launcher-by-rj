package dev.dothome.ui;
import android.graphics.*;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.view.*;
import java.io.File;
import java.util.concurrent.*;

/** Optional wallpaper motion, scheduled only while Android reports it visible. */
public final class DotAtmosphere extends WallpaperService {
  private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
  @Override public Engine onCreateEngine(){return new Surface();}
  private final class Surface extends Engine implements Choreographer.FrameCallback {
    final Handler main=new Handler(Looper.getMainLooper());final Paint paint=new Paint(3);Bitmap image;boolean visible,alive=true,posted;long firstFrame,lastDraw;int generation;
    @Override public void onCreate(SurfaceHolder holder){super.onCreate(holder);load();}
    void load(){final int request=++generation;WORK.execute(()->{Bitmap result=null;try{File own=new File(getFilesDir(),"wallpaper-local.jpg");String name=getSharedPreferences("dot-style",0).getString("wallpaper","glass");Bitmap base=name.equals("local")?BitmapFactory.decodeFile(own.getPath(),new BitmapFactory.Options()):null;if(base==null)base=DotWallpaper.INSTANCE.bitmap(name,720,1600);result=LocalWallpaperEffects.atmosphere(base);base.recycle();}catch(RuntimeException|OutOfMemoryError ignored){}Bitmap loaded=result;main.post(()->{if(!alive||request!=generation){if(loaded!=null)loaded.recycle();return;}Bitmap old=image;image=loaded;if(old!=null)old.recycle();lastDraw=0;notifyColorsChanged();schedule();});});}
    void schedule(){if(visible&&alive&&!posted){posted=true;Choreographer.getInstance().postFrameCallback(this);}}
    @Override public void onVisibilityChanged(boolean state){visible=state;if(state){lastDraw=0;schedule();}else{Choreographer.getInstance().removeFrameCallback(this);posted=false;}}
    @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){super.onSurfaceChanged(holder,format,width,height);schedule();}
    @Override public void doFrame(long time){posted=false;if(!visible||!alive)return;if(firstFrame==0)firstFrame=time;boolean motion=!DotAppearance.get(DotAtmosphere.this).reducedMotion;
      if(lastDraw==0||time-lastDraw>=32_000_000L){lastDraw=time;Canvas c=null;try{c=getSurfaceHolder().lockHardwareCanvas();if(c!=null){if(image==null)c.drawColor(DotAppearance.get(DotAtmosphere.this).background);else{float t=(time-firstFrame)/1_000_000_000f,dx=motion?(float)Math.sin(t/11)*c.getWidth()*.035f:0,dy=motion?(float)Math.cos(t/15)*c.getHeight()*.025f:0;c.drawBitmap(image,null,new RectF(-c.getWidth()*.08f+dx,-c.getHeight()*.08f+dy,c.getWidth()*1.08f+dx,c.getHeight()*1.08f+dy),paint);}}}catch(RuntimeException unavailable){}finally{if(c!=null)getSurfaceHolder().unlockCanvasAndPost(c);}}
      if(motion)schedule();
    }
    @Override public android.app.WallpaperColors onComputeColors(){return image==null?null:android.app.WallpaperColors.fromBitmap(image);}
    @Override public void onDestroy(){alive=false;visible=false;generation++;Choreographer.getInstance().removeFrameCallback(this);main.removeCallbacksAndMessages(null);Bitmap old=image;image=null;if(old!=null)old.recycle();super.onDestroy();}
  }
}
