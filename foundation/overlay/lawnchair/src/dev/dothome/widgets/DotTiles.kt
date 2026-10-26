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
        info.configure=null; info.previewLayout=0; info.widgetFeatures=0; info.generatedPreviewCategories=0
        info.widgetCategory=AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN; info.autoAdvanceViewId=View.NO_ID; info.updatePeriodMillis=0
        info.targetCellWidth=columns; info.targetCellHeight=rows
        (info as CustomAppWidgetProviderInfo).apply { spanX=columns; spanY=rows; minSpanX=columns; minSpanY=rows; maxSpanX=4; maxSpanY=if(kind=="photo") 4 else rows }
    }
    override fun onViewCreated(parent: AppWidgetHostView) {
        parent.removeAllViews(); parent.setPadding(0,0,0,0)
        parent.addView(Surface(parent.context,parent,this), FrameLayout.LayoutParams(-1,-1))
    }
    private class Surface(context: Context, private val host: AppWidgetHostView, private val tile: DotTile): FrameLayout(context), TileRefresh.Active {
        private val adapter=TileContext(context)
        private val handler=Handler(Looper.getMainLooper())
        private val sensors=TileSensors(adapter,tile.kind) { invalidateArtwork() }
        private fun invalidateArtwork() { for(n in 0 until childCount) getChildAt(n).invalidate() }
        private var signature=""
        private var id=-1L
        private var item=TileContext.load(context,id,tile.kind,tile.face)
        private val refresh=object: Runnable { override fun run() { if(windowVisibility==VISIBLE && isShown && hasWindowFocus() && getGlobalVisibleRect(android.graphics.Rect())) { adapter.foreground=true;adapter.sync(); render(); sensors.start(); handler.postDelayed(this,if(tile.kind=="media") 1000 else 30000) } else {adapter.foreground=false;sensors.stop()} } }
        private fun render() {
            item.put("w",(host.tag as? ItemInfo)?.spanX ?: tile.columns); item.put("h",(host.tag as? ItemInfo)?.spanY ?: tile.rows)
            adapter.page=item.optInt("page",0)
            val media=if(tile.kind=="media")TileMedia.read(context) else null
            val nextSignature=item.toString()+(media?.let {"${it.title}:${it.artist}:${it.playing}:${it.controller?.packageName}:${it.artwork?.generationId}"} ?: "")
            if(childCount>0 && signature==nextSignature) { invalidateArtwork(); return }
            signature=nextSignature
            removeAllViews()
            val art: View = if(tile.kind=="photo" && item.optString("uri").isNotEmpty()) PhotoView(adapter,item,false) else TileArtwork(adapter,item,false)
            addView(art,LayoutParams(-1,-1))
            contentDescription=tile.label
            if(tile.kind=="controls") {
                val grid=android.widget.GridLayout(context).apply {columnCount=2;rowCount=2}
                arrayOf("bluetooth","sound","torch","dnd").forEachIndexed {index,kind->
                    grid.addView(android.widget.TextView(context).apply {text=arrayOf("Bluetooth","Sound","Flashlight","Do not disturb")[index];textSize=11f;gravity=android.view.Gravity.CENTER;setTextColor(dev.dothome.ui.DotSurface.ink(context));setOnClickListener {TileActions.tap(context,this@Surface.id,org.json.JSONObject().put("type",kind))}},android.widget.GridLayout.LayoutParams().apply {width=0;height=0;columnSpec=android.widget.GridLayout.spec(index%2,1f);rowSpec=android.widget.GridLayout.spec(index/2,1f)})
                }
                addView(grid,LayoutParams(-1,-1));setOnClickListener(null);return
            }
            if(tile.kind=="media") {
                val controls=android.widget.LinearLayout(context).apply { gravity=android.view.Gravity.CENTER }
                val state=TileMedia.read(context)
                val icons=intArrayOf(android.R.drawable.ic_media_previous,if(state.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,android.R.drawable.ic_media_next)
                val labels=arrayOf("Previous track",if(state.playing) "Pause" else "Play","Next track")
                icons.forEachIndexed { index, icon ->
                    controls.addView(android.widget.ImageButton(context).apply {
                        setImageResource(icon); contentDescription=labels[index]; imageTintList=android.content.res.ColorStateList.valueOf(if(adapter.dark()) 0xfff0f0ea.toInt() else 0xff191a18.toInt())
                        background=null; setOnClickListener { state.action(context,index-1); handler.removeCallbacks(refresh);handler.post(refresh) }
                    },android.widget.LinearLayout.LayoutParams(adapter.dp(48f),adapter.dp(48f)))
                }
                addView(controls,LayoutParams(-1,adapter.dp(48f),android.view.Gravity.BOTTOM))
            }
            setOnClickListener { TileActions.tap(context,id,item); if(tile.kind in setOf("torch","sound","dnd")) { handler.removeCallbacks(refresh);handler.postDelayed(refresh,120) } }
        }
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            post {
                id=(host.tag as? ItemInfo)?.id?.toLong() ?: -1L
                item=TileContext.load(context,id,tile.kind,tile.face)
                // Bind static artwork even on an off-screen page. Visibility
                // controls updates and sensors, not whether the tile exists.
                render()
                setActive(isShown && hasWindowFocus())
            }
        }
        override fun setActive(active: Boolean) {
            val home=TileActions.activity(context) as? com.android.launcher3.Launcher
            val allowed=active && getGlobalVisibleRect(android.graphics.Rect()) && (home==null || home.isInState(com.android.launcher3.LauncherState.NORMAL) || home.isInState(com.android.launcher3.LauncherState.EDIT_MODE))
            handler.removeCallbacks(refresh); adapter.foreground=allowed
            if(allowed && isAttachedToWindow && isShown) {
                item=TileContext.load(context,id,tile.kind,tile.face); handler.post(refresh)
            } else sensors.stop()
        }
        override fun onVisibilityAggregated(visible: Boolean) { super.onVisibilityAggregated(visible); setActive(visible && hasWindowFocus()) }
        override fun onWindowFocusChanged(focused: Boolean) { super.onWindowFocusChanged(focused); setActive(focused && isShown) }
        override fun onDetachedFromWindow() { adapter.foreground=false; sensors.stop(); handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }
        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            setActive(visibility==VISIBLE && hasWindowFocus())
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
    class AwayCompact(context: Context): DotTile(context,"away",0,"Away time · Compact",2,1)
    class AwaySmall(context: Context): DotTile(context,"away",0,"Away time · Small",1,1)
    class Weather(context: Context): DotTile(context,"weather",0,"Weather · Local")
    class WeatherCompact(context: Context): DotTile(context,"weather",0,"Weather · Compact",2,1)
    class Steps(context: Context): DotTile(context,"steps",0,"Pedometer")
    class StepsSmall(context: Context): DotTile(context,"steps",0,"Pedometer · Small",1,1)
    class Headphones(context: Context): DotTile(context,"headphones",0,"Audio devices")
    class UsageSmall(context: Context): DotTile(context,"usage",0,"Screen time · Small",1,1)
    class Away(context: Context): DotTile(context,"away",0,"Away time")
    class Photo(context: Context): DotTile(context,"photo",0,"Photos")
    class Sticker(context: Context): DotTile(context,"photo",2,"Photos · Sticker")
    class Notes(context: Context): DotTile(context,"notes",0,"Notes")
    class Agenda(context: Context): DotTile(context,"agenda",0,"Quick look")
    class Countdown(context: Context): DotTile(context,"countdown",0,"Countdown")
    class Timer(context: Context): DotTile(context,"timer",0,"Elapsed timer",2,1)
    class Controls(context: Context): DotTile(context,"controls",0,"Quick controls")
    class Media(context: Context): DotTile(context,"media",0,"Media player",4,2)
    class Usage(context: Context): DotTile(context,"usage",0,"Screen time",2,1)
    class Compass(context: Context): DotTile(context,"compass",0,"Compass")
    class Torch(context: Context): DotTile(context,"torch",0,"Flashlight",1,1)
    class Bluetooth(context: Context): DotTile(context,"bluetooth",0,"Bluetooth",1,1)
    class Sound(context: Context): DotTile(context,"sound",0,"Sound",1,1)
    class Dnd(context: Context): DotTile(context,"dnd",0,"Do not disturb",1,1)
}
