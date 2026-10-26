package dev.dothome

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import app.lawnchair.LawnchairLauncher
import dev.dothome.ui.DotWallpaper

/** Keep the installed Home component while replacing the previous custom Activity engine. */
class HomeActivity : LawnchairLauncher() {
    private val unlockReceiver=object: android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context,intent: Intent) {
            if(intent.action==Intent.ACTION_USER_PRESENT) {dev.dothome.widgets.TileRefresh.onUnlock(context);workspace?.let {dev.dothome.widgets.TileRefresh.setActive(it,hasWindowFocus())}}
        }
    }
    private val wallpaperListener=android.app.WallpaperManager.OnColorsChangedListener {_,_->
        dev.dothome.ui.DotAppearance.refreshWallpaper(this)
        updateMaterial()
        workspace?.let {dev.dothome.widgets.TileRefresh.sync(it);it.invalidate()}
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        registerReceiver(unlockReceiver,android.content.IntentFilter(Intent.ACTION_USER_PRESENT),android.content.Context.RECEIVER_NOT_EXPORTED)
        android.app.WallpaperManager.getInstance(this).addOnColorsChangedListener(wallpaperListener,android.os.Handler(mainLooper))
        dev.dothome.ui.DotAppearance.refreshWallpaper(this)
        updateMaterial()
        val saved = getSharedPreferences("runtime", 0)
        val roles = getSystemService(RoleManager::class.java) ?: return
        if(!roles.isRoleHeld(RoleManager.ROLE_HOME) && !saved.getBoolean("foundationHomePrompt", false)) {
            window.decorView.postDelayed({
                if(!isFinishing && !isDestroyed) {
                    saved.edit().putBoolean("foundationHomePrompt", true).apply()
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 100)
                }
            }, 500)
        }
    }

    override fun finishBindingItems(pagesBoundFirst: com.android.launcher3.util.IntSet) {
        super.finishBindingItems(pagesBoundFirst)
        dev.dothome.migration.DotMigration.start(this)
    }

    private var drawerDownX=0f
    private var drawerDownY=0f
    private var drawerSwipe=false
    private var hiddenDragging=false
    override fun getOnBackPressedHandler(): com.android.launcher3.OnBackPressedHandler {
        if(dev.dothome.ui.DotHiddenPanel.isOpen(this))return object: com.android.launcher3.OnBackPressedHandler {
            override fun onBackInvoked() {dev.dothome.ui.DotHiddenPanel.close(this@HomeActivity)}
            override fun onBackProgressed(progress: Float) {if(!dev.dothome.ui.DotAppearance.get(this@HomeActivity).reducedMotion)dev.dothome.ui.DotHiddenPanel.dragging(this@HomeActivity,dragLayer.width*(1-progress))}
            override fun onBackCancelled() {dev.dothome.ui.DotHiddenPanel.finish(this@HomeActivity,true)}
        }
        return super.getOnBackPressedHandler()
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if(dev.dothome.ui.DotArrange.touch(this,event))return true
        when(event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                if(dev.dothome.ui.DotHiddenPanel.isOpen(this))return super.dispatchTouchEvent(event)
                hiddenDragging=false
                drawerDownX=event.x;drawerDownY=event.y
                drawerSwipe=isInState(com.android.launcher3.LauncherState.ALL_APPS)
            }
            android.view.MotionEvent.ACTION_MOVE -> if(drawerSwipe) {
                val dx=event.x-drawerDownX;val dy=kotlin.math.abs(event.y-drawerDownY)
                if(hiddenDragging) {dev.dothome.ui.DotHiddenPanel.dragging(this,dx);return true}
                if(dy>resources.displayMetrics.density*32) drawerSwipe=false
                else if(dx>resources.displayMetrics.density*18 && dy<dx*.45f) {
                    hiddenDragging=true
                    val cancel=android.view.MotionEvent.obtain(event);cancel.action=android.view.MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(cancel);cancel.recycle()
                    dev.dothome.ui.DotHiddenPanel.dragging(this,dx);return true
                }
            }
            android.view.MotionEvent.ACTION_UP,android.view.MotionEvent.ACTION_CANCEL -> {
                if(hiddenDragging) {
                    val open=event.actionMasked==android.view.MotionEvent.ACTION_UP && event.x-drawerDownX>resources.displayMetrics.density*96
                    dev.dothome.ui.DotHiddenPanel.finish(this,open);hiddenDragging=false;drawerSwipe=false;return true
                }
                drawerSwipe=false
                workspace.postDelayed({if(!isDestroyed)dev.dothome.widgets.TileRefresh.setActive(workspace,isInState(com.android.launcher3.LauncherState.NORMAL) || isInState(com.android.launcher3.LauncherState.EDIT_MODE))},500)
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() {dev.dothome.widgets.DotWidgetTray.close();dev.dothome.ui.DotCategoryCard.close();dev.dothome.ui.DotHiddenPanel.close(this); unregisterReceiver(unlockReceiver);android.app.WallpaperManager.getInstance(this).removeOnColorsChangedListener(wallpaperListener);dev.dothome.ui.DotArrange.end(this); super.onDestroy() }

    override fun onStateSetStart(state: com.android.launcher3.LauncherState) {
        super.onStateSetStart(state)
        workspace?.let { dev.dothome.widgets.TileRefresh.setActive(it,state==com.android.launcher3.LauncherState.NORMAL || state==com.android.launcher3.LauncherState.EDIT_MODE) }
    }

    override fun onResume() { super.onResume();dev.dothome.ui.DotAppearance.invalidate();dev.dothome.ui.DotAppearance.refreshWallpaper(this); updateMaterial(); dev.dothome.migration.DotMigration.start(this) }

    override fun onNewIntent(intent: Intent) {
        dev.dothome.widgets.DotWidgetTray.close();dev.dothome.ui.DotCategoryCard.close()
        dev.dothome.ui.DotHiddenPanel.close(this)
        super.onNewIntent(intent); setIntent(intent)
        workspace?.postDelayed({if(!isDestroyed)dev.dothome.widgets.TileRefresh.setActive(workspace,isInState(com.android.launcher3.LauncherState.NORMAL) || isInState(com.android.launcher3.LauncherState.EDIT_MODE))},500)
    }

    private fun updateMaterial() {
        val style = getSharedPreferences("dot-style", 0)
        val root=rootView ?: return
        val key=style.getString("wallpaper","glass")!!
        if(style.getBoolean("wallpaperApplied",false)) root.background=null
        else if((root.background as? dev.dothome.ui.DotWallpaperDrawable)?.key!=key) root.background=dev.dothome.ui.DotWallpaperDrawable(key)
    }
}
