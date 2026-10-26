package dev.dothome.ui

import android.content.Intent
import android.graphics.Color
import android.view.*
import android.widget.*
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking
import java.util.WeakHashMap

/** A reversible drawer page. The standalone Activity remains a settings/editor entry. */
object DotHiddenPanel {
    private val panels=WeakHashMap<LawnchairLauncher,Panel>()
    @JvmStatic fun dragging(home: LawnchairLauncher,distance: Float) {
        val panel=panels.getOrPut(home){Panel(home).also {home.dragLayer.addView(it,com.android.launcher3.views.BaseDragLayer.LayoutParams(-1,-1))}}
        panel.translationX=(-home.dragLayer.width+distance).coerceAtMost(0f)
    }
    @JvmStatic fun finish(home: LawnchairLauncher,open: Boolean) {
        val panel=panels[home] ?: return
        panel.opened=open
        if(DotAppearance.get(home).reducedMotion){panel.translationX=if(open)0f else -panel.width.toFloat();if(!open)close(home)}
        else panel.animate().translationX(if(open)0f else -panel.width.toFloat()).setDuration(190).setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction {if(!open)close(home)}.start()
    }
    @JvmStatic fun isOpen(home: LawnchairLauncher)=panels[home]?.opened==true
    @JvmStatic fun close(home: LawnchairLauncher): Boolean {val panel=panels.remove(home)?:return false;panel.animate().cancel();(panel.parent as? ViewGroup)?.removeView(panel);return true}
    private class Panel(val home: LawnchairLauncher): LinearLayout(home) {
        var opened=false
        var start=0f
        var startY=0f
        var dragging=false
        init {
            orientation=VERTICAL;setBackgroundColor(DotAppearance.get(home).background);isClickable=true;isFocusable=true
            val b=home.deviceProfile.insets;setPadding(DotUi.dp(home,18f),b.top,DotUi.dp(home,18f),b.bottom)
            val header=LinearLayout(home).apply {gravity=Gravity.CENTER_VERTICAL}
            header.addView(DotUi.button(home,"‹"){finish(home,false)}.apply {contentDescription="Back"},LayoutParams(DotUi.dp(home,48f),DotUi.dp(home,56f)))
            header.addView(DotUi.text(home,"Hidden icons",17f).apply {gravity=Gravity.CENTER},LayoutParams(0,-1,1f))
            header.addView(DotUi.button(home,"Edit"){close(home);home.startActivity(Intent(home,DotHidden::class.java))});addView(header)
            val selected=PreferenceManager2.getInstance(home).hiddenApps.firstBlocking()
            val apps=home.appsView.appsStore.apps.filter {it.toComponentKey().toString() in selected}.sortedBy {it.title.toString().lowercase()}
            if(apps.isEmpty())addView(DotUi.text(home,"No hidden apps",15f).apply {gravity=Gravity.CENTER;setTextColor(DotAppearance.get(home).secondary)},LayoutParams(-1,0,1f))
            else {
                val scroll=ScrollView(home);val grid=GridLayout(home).apply {columnCount=4}
                apps.forEachIndexed {index,app->
                    val cell=DotUi.column(home).apply {gravity=Gravity.CENTER;setPadding(0,DotUi.dp(home,16f),0,DotUi.dp(home,12f));contentDescription=app.title;isFocusable=true}
                    cell.addView(ImageView(home).apply {setImageDrawable(app.newIcon(home,if(PreferenceManager.getInstance(home).drawerThemedIcons.get())com.android.launcher3.icons.BitmapInfo.FLAG_THEMED else 0));scaleType=ImageView.ScaleType.FIT_CENTER},LayoutParams(DotUi.dp(home,52f),DotUi.dp(home,52f)))
                    cell.addView(DotUi.text(home,app.title.toString(),11f).apply {gravity=Gravity.CENTER;maxLines=2},LayoutParams(-1,DotUi.dp(home,40f)))
                    cell.setOnClickListener {home.startActivitySafely(it,app.intent,app)}
                    grid.addView(cell,GridLayout.LayoutParams().apply {width=0;columnSpec=GridLayout.spec(index%4,1f)})
                }
                scroll.addView(grid);addView(scroll,LayoutParams(-1,0,1f))
            }
        }
        override fun onInterceptTouchEvent(event: android.view.MotionEvent): Boolean {
            if(event.actionMasked==android.view.MotionEvent.ACTION_DOWN){start=event.x;startY=event.y;dragging=false}
            if(event.actionMasked==android.view.MotionEvent.ACTION_MOVE && start-event.x>DotUi.dp(home,20f) && kotlin.math.abs(event.y-startY)<(start-event.x)*.55f)dragging=true
            return dragging
        }
        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            when(event.actionMasked){android.view.MotionEvent.ACTION_MOVE->translationX=minOf(0f,event.x-start);android.view.MotionEvent.ACTION_UP->finish(home,translationX>-width*.25f);android.view.MotionEvent.ACTION_CANCEL->finish(home,true)}
            return true
        }
    }
}
