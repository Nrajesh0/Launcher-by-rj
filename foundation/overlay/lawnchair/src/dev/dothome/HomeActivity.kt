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

    override fun onResume() { super.onResume(); updateMaterial() }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }

    private fun updateMaterial() {
        val style = getSharedPreferences("dot-style", 0)
        rootView?.background = if(style.getBoolean("wallpaperApplied", false)) null else object: Drawable() {
            override fun draw(canvas: Canvas) { DotWallpaper.paint(canvas, bounds.width(), bounds.height(), style.getString("wallpaper", "glass")!!) }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(filter: ColorFilter?) {}
            @Deprecated("Opacity") override fun getOpacity(): Int = PixelFormat.OPAQUE
        }
    }
}
