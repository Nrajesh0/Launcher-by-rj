package dev.dothome.ui
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import java.io.File
import java.util.WeakHashMap
import java.util.concurrent.Executors

/** Owned-image sampling. Decode and blur never run in an animation frame. */
object DotBackdrop {
    @Volatile private var key=""
    @Volatile private var image: Bitmap?=null
    @Volatile private var blur: Bitmap?=null
    private val work=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val observers=WeakHashMap<View,Boolean>()
    private fun load(context: Context) {
        val app=context.applicationContext
        val prefs=app.getSharedPreferences("dot-style",0)
        val name=prefs.getString("wallpaper","glass")!!
        val file=File(app.filesDir,"wallpaper-local.jpg")
        val next="$name:${if(name=="local")file.lastModified() else 0}:${prefs.getBoolean("externalWallpaper",false)}"
        if(key==next)return
        synchronized(this) {if(key==next)return;key=next;image=null;blur=null}
        if(prefs.getBoolean("externalWallpaper",false))return
        work.execute {
            runCatching {
                val loaded=if(name=="local")BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply {inSampleSize=2}) else null
                val source=loaded?:DotWallpaper.bitmap(name,540,1200)
                val tiny=Bitmap.createScaledBitmap(source,90,200,true)
                val frosted=LocalWallpaperEffects.atmosphere(tiny)
                if(tiny!==source && tiny!==frosted)tiny.recycle()
                main.post {if(key==next){image=source;blur=frosted;synchronized(observers){observers.keys.toList().forEach {it.invalidate()}}}}
            }
        }
    }
    @JvmStatic fun draw(context: Context,canvas: Canvas,destination: RectF) {
        load(context);val source=image
        if(source!=null)canvas.drawBitmap(source,null,destination,Paint(3))
        else {val paint=Paint(3);paint.color=DotAppearance.get(context).background;canvas.drawRect(destination,paint)}
    }
    @JvmStatic fun glass(view: View,canvas: Canvas) {
        synchronized(observers){observers[view]=true}
        load(view.context)
        if(view.context.getSharedPreferences("dot-style",0).getBoolean("externalWallpaper",false))return
        val position=IntArray(2);view.getLocationOnScreen(position)
        val screen=view.resources.displayMetrics
        blur?.let {
            val left=(position[0]*it.width/screen.widthPixels).coerceIn(0,it.width-1)
            val top=(position[1]*it.height/screen.heightPixels).coerceIn(0,it.height-1)
            val right=((position[0]+view.width)*it.width/screen.widthPixels).coerceIn(left+1,it.width)
            val bottom=((position[1]+view.height)*it.height/screen.heightPixels).coerceIn(top+1,it.height)
            canvas.drawBitmap(it,Rect(left,top,right,bottom),RectF(0f,0f,view.width.toFloat(),view.height.toFloat()),Paint(3))
        }
    }
}
