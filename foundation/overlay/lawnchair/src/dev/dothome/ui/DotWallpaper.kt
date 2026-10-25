package dev.dothome.ui

import android.graphics.*
import kotlin.math.*
import java.util.Random

/** Original, bundled optical materials. Computed locally; no images are fetched. */
object DotWallpaper {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grain: Bitmap by lazy {
        val random = Random(28471)
        Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(128*128) { val gray=random.nextInt(256); Color.argb(18,gray,gray,gray) }
            setPixels(pixels,0,128,0,0,128,128)
        }
    }
    @Synchronized
    fun paint(canvas: Canvas, width: Int, height: Int, key: String) {
        val w=width.toFloat(); val h=height.toFloat()
        if(w<=0 || h<=0) return
        canvas.save(); canvas.clipRect(0f,0f,w,h)
        val dark=key=="optic" || key=="graphite"
        canvas.drawColor(if(dark) 0xff0d171b.toInt() else 0xffc9ccc8.toInt())
        val colours = when(key) {
            "optic" -> intArrayOf(0xff12566a.toInt(),0xff25333e.toInt(),0xff518c98.toInt(),0xffbc514f.toInt(),0xff122e3b.toInt())
            "graphite" -> intArrayOf(0xff343943.toInt(),0xff111317.toInt(),0xff696d72.toInt(),0xff584f48.toInt(),0xff1f252d.toInt())
            "mist" -> intArrayOf(0xffdfd8c9.toInt(),0xffb9c8c8.toInt(),0xffc9bfd7.toInt(),0xffaccdbe.toInt(),0xffced7d0.toInt())
            else -> intArrayOf(0xffafd2bd.toInt(),0xffbac0d7.toInt(),0xffc5d6a2.toInt(),0xffd5bfd3.toInt(),0xffe7dfcf.toInt())
        }
        val centers=arrayOf(floatArrayOf(.02f,.03f,.95f),floatArrayOf(.82f,.13f,.76f),floatArrayOf(.91f,.51f,.9f),floatArrayOf(.12f,.68f,.7f),floatArrayOf(.52f,1.04f,.85f))
        for(n in centresIndices()) {
            val spot=centers[n]
            paint.shader=RadialGradient(w*spot[0],h*spot[1],w*spot[2],intArrayOf(colours[n],colours[n] and 0x00ffffff),floatArrayOf(0f,1f),Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,w,h,paint)
        }
        paint.shader=null
        // Fine curved refraction lines have a quiet highlight and a dark edge.
        val ring=RectF(-w*.56f,h*.18f,w*2.3f,h*.18f+w*2.86f)
        paint.style=Paint.Style.STROKE
        for(n in 0..185) {
            val inset=n*w*.0041f
            paint.color=if(dark) Color.argb(17,178,224,226) else Color.argb(26,255,255,250)
            paint.strokeWidth=maxOf(.35f,w*.00062f)
            canvas.drawArc(RectF(ring.left+inset,ring.top+inset,ring.right-inset,ring.bottom-inset),145f,174f,false,paint)
        }
        if(key=="optic") {
            paint.style=Paint.Style.FILL
            paint.shader=RadialGradient(w*.68f,h*.34f,w*.19f,intArrayOf(0x99e68476.toInt(),0x00ba5849),floatArrayOf(0f,1f),Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,w,h,paint)
        }
        paint.style=Paint.Style.FILL
        paint.shader=BitmapShader(grain,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT)
        canvas.drawRect(0f,0f,w,h,paint)
        paint.shader=null
        canvas.restore()
    }
    private fun centresIndices() = 0..4
    fun bitmap(key: String,width: Int,height: Int): Bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also { paint(Canvas(it),width,height,key) }
}
