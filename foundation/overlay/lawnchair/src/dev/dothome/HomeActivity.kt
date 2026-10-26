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
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
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
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if(dev.dothome.ui.DotArrange.touch(this,event))return true
        when(event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                drawerDownX=event.x;drawerDownY=event.y
                drawerSwipe=isInState(com.android.launcher3.LauncherState.ALL_APPS)
            }
            android.view.MotionEvent.ACTION_MOVE -> if(drawerSwipe) {
                val dx=event.x-drawerDownX;val dy=kotlin.math.abs(event.y-drawerDownY)
                if(dy>resources.displayMetrics.density*32) drawerSwipe=false
                else if(dx>resources.displayMetrics.density*96 && dy<dx*.3f) {
                    drawerSwipe=false
                    val cancel=android.view.MotionEvent.obtain(event);cancel.action=android.view.MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(cancel);cancel.recycle()
                    startActivity(Intent(this,dev.dothome.ui.DotHidden::class.java));return true
                }
            }
            android.view.MotionEvent.ACTION_UP,android.view.MotionEvent.ACTION_CANCEL -> drawerSwipe=false
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() { dev.dothome.ui.DotArrange.end(this); super.onDestroy() }

    override fun onStateSetStart(state: com.android.launcher3.LauncherState) {
        super.onStateSetStart(state)
        workspace?.let { dev.dothome.widgets.TileRefresh.setActive(it,state==com.android.launcher3.LauncherState.NORMAL || state==com.android.launcher3.LauncherState.EDIT_MODE) }
    }

    override fun onResume() { super.onResume(); updateMaterial() }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }

    private fun updateMaterial() {
        val style = getSharedPreferences("dot-style", 0)
        val root=rootView ?: return
        val key=style.getString("wallpaper","glass")!!
        if(style.getBoolean("wallpaperApplied",false)) root.background=null
        else if((root.background as? dev.dothome.ui.DotWallpaperDrawable)?.key!=key) root.background=dev.dothome.ui.DotWallpaperDrawable(key)
    }
}
