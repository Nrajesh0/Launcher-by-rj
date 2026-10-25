package dev.dothome.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.widget.custom.CustomAppWidgetProviderInfo
import com.android.systemui.plugins.CustomWidgetPlugin

/** Native widgets participate in the core tray, drag, resize and durable item model. */
open class DotTile(private val context: Context, val kind: String, val face: Int, val label: String, val columns: Int=2, val rows: Int=2): CustomWidgetPlugin {
    override fun updateWidgetInfo(info: AppWidgetProviderInfo, context: Context) {
        info.label=label; info.initialLayout=R.layout.dot_widget_empty
        info.previewImage=if(kind=="clock") R.drawable.dot_preview_clock else R.drawable.dot_preview_widget
        info.icon=R.drawable.ic_launcher_home
        info.resizeMode=if(columns==1) AppWidgetProviderInfo.RESIZE_NONE else AppWidgetProviderInfo.RESIZE_BOTH
        info.configure=null; info.targetCellWidth=columns; info.targetCellHeight=rows
        (info as CustomAppWidgetProviderInfo).apply { spanX=columns; spanY=rows; minSpanX=columns; minSpanY=rows; maxSpanX=4; maxSpanY=if(kind=="photo") 4 else rows }
    }
    override fun onViewCreated(parent: AppWidgetHostView) {
        parent.removeAllViews(); parent.setPadding(0,0,0,0)
        parent.addView(Surface(parent.context,parent,this), FrameLayout.LayoutParams(-1,-1))
    }
    private class Surface(context: Context, private val host: AppWidgetHostView, private val tile: DotTile): FrameLayout(context) {
        private val adapter=TileContext(context)
        private val handler=Handler(Looper.getMainLooper())
        private var id=-1L
        private var item=TileContext.load(context,id,tile.kind,tile.face)
        private val refresh=object: Runnable { override fun run() { if(windowVisibility==VISIBLE && isShown) { adapter.sync(); render(); handler.postDelayed(this,30000) } } }
        private fun render() {
            removeAllViews()
            item.put("w",if(width>height*1.4f) 4 else tile.columns); item.put("h",tile.rows)
            val art: View = if(tile.kind=="photo" && item.optString("uri").isNotEmpty()) PhotoView(adapter,item,false) else TileArtwork(adapter,item,false)
            addView(art,LayoutParams(-1,-1))
            contentDescription=tile.label
            setOnClickListener { TileActions.tap(context,id,item) }
        }
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post {
                id=(host.tag as? ItemInfo)?.id?.toLong() ?: -1L
                item=TileContext.load(context,id,tile.kind,tile.face)
                adapter.foreground=true; handler.post(refresh)
            }
        }
        override fun onDetachedFromWindow() { adapter.foreground=false; handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }
        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility); adapter.foreground=visibility==VISIBLE
            handler.removeCallbacks(refresh); if(visibility==VISIBLE) { item=TileContext.load(context,id,tile.kind,tile.face); handler.post(refresh) }
        }
    }
}

object DotTiles {
    class Clock(context: Context): DotTile(context,"clock",1,"Clock · Minimal")
    class DotClock(context: Context): DotTile(context,"clock",0,"Clock · Dot matrix")
    class DialClock(context: Context): DotTile(context,"clock",3,"Clock · Dial")
    class SerifClock(context: Context): DotTile(context,"clock",2,"Clock · Serif")
    class DigitalClock(context: Context): DotTile(context,"clock",4,"Clock · Digital")
    class Date(context: Context): DotTile(context,"date",2,"Date")
    class Battery(context: Context): DotTile(context,"battery",2,"Battery · Dots",2,1)
    class BatteryRing(context: Context): DotTile(context,"battery",0,"Battery · Ring")
    class Focus(context: Context): DotTile(context,"breathing",0,"Focus")
    class Away(context: Context): DotTile(context,"away",0,"Away time")
    class Photo(context: Context): DotTile(context,"photo",0,"Photos")
    class Sticker(context: Context): DotTile(context,"photo",2,"Photos · Sticker")
    class Notes(context: Context): DotTile(context,"notes",0,"Notes")
    class Agenda(context: Context): DotTile(context,"agenda",0,"Quick look")
    class Countdown(context: Context): DotTile(context,"countdown",0,"Countdown")
    class Timer(context: Context): DotTile(context,"timer",0,"Elapsed timer",2,1)
    class Usage(context: Context): DotTile(context,"usage",0,"Screen time",2,1)
    class Compass(context: Context): DotTile(context,"compass",0,"Compass")
    class Torch(context: Context): DotTile(context,"torch",0,"Flashlight",1,1)
    class Bluetooth(context: Context): DotTile(context,"bluetooth",0,"Bluetooth",1,1)
    class Sound(context: Context): DotTile(context,"sound",0,"Sound",1,1)
    class Dnd(context: Context): DotTile(context,"dnd",0,"Do not disturb",1,1)
}
