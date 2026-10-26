package dev.dothome.ui
import android.content.Context
import android.graphics.*
import android.view.View
import java.io.File

/** Samples our own saved wallpaper only; no screen capture or storage permission. */
object DotBackdrop {
    private var key=""
    private var image: Bitmap?=null
    private var blur: Bitmap?=null
    @Synchronized private fun load(context: Context) {
        val name=context.getSharedPreferences("dot-style",0).getString("wallpaper","glass")!!
        val file=File(context.filesDir,"wallpaper-local.jpg")
        val next="$name:${if(name=="local")file.lastModified() else 0}"
        if(key==next && image!=null)return
        image=if(name=="local")BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply {inSampleSize=2}) else null
        if(image==null)image=DotWallpaper.bitmap(name,540,1200)
        val mini=Bitmap.createScaledBitmap(image!!,90,200,true)
        val pixels=IntArray(18000);mini.getPixels(pixels,0,90,0,0,90,200)
        repeat(3) {
            val source=pixels.copyOf()
            for(y in 0 until 200)for(x in 0 until 90) {
                var r=0;var g=0;var b=0;var count=0
                for(dy in -3..3)for(dx in -3..3) {val c=source[(y+dy).coerceIn(0,199)*90+(x+dx).coerceIn(0,89)];r+=Color.red(c);g+=Color.green(c);b+=Color.blue(c);count++}
                pixels[y*90+x]=Color.rgb(r/count,g/count,b/count)
            }
        }
        blur=Bitmap.createBitmap(pixels,90,200,Bitmap.Config.ARGB_8888);key=next
    }
    @JvmStatic fun draw(context: Context,canvas: Canvas,destination: RectF) {
        load(context);image?.let {canvas.drawBitmap(it,null,destination,Paint(3))}
    }
    @JvmStatic fun glass(view: View,canvas: Canvas) {
        load(view.context)
        val position=IntArray(2);view.getLocationOnScreen(position)
        val screen=view.resources.displayMetrics
        blur?.let {
            val source=Rect((position[0]*it.width/screen.widthPixels).coerceIn(0,it.width-1),(position[1]*it.height/screen.heightPixels).coerceIn(0,it.height-1),((position[0]+view.width)*it.width/screen.widthPixels).coerceIn(1,it.width),((position[1]+view.height)*it.height/screen.heightPixels).coerceIn(1,it.height))
            canvas.drawBitmap(it,source,RectF(0f,0f,view.width.toFloat(),view.height.toFloat()),Paint(3))
        }
    }
}
