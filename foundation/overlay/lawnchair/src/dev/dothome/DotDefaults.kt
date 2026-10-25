package dev.dothome

import android.content.Context
import com.android.launcher3.Utilities
import org.json.JSONObject
import java.io.File

/** Defaults for the new engine. Legacy placement data is preserved for explicit migration. */
object DotDefaults {
    @JvmStatic fun apply(context: Context) {
        val marker = context.getSharedPreferences("dot-style", 0)
        if(marker.getBoolean("foundationDefaults", false)) return
        val previous = File(context.filesDir, "workspace-v1.json")
        if(previous.isFile) {
            val recovery = File(context.filesDir, "pre-foundation-layout.json")
            if(!recovery.exists()) previous.copyTo(recovery)
        }
        val prefs = context.getSharedPreferences("com.android.launcher3.prefs", Context.MODE_PRIVATE)
        val legacySettings = runCatching { JSONObject(previous.readText()).getJSONObject("settings") }.getOrNull()
        val legacyDark = legacySettings?.optBoolean("dark", false) ?: false
        val columns = (legacySettings?.optInt("columns",4) ?: 4).coerceIn(4,5)
        val rows = (legacySettings?.optInt("rows",7) ?: 7).coerceIn(5,8)
        prefs.edit()
            .putString("pref_accentColor2", "custom|#ff757575")
            .putBoolean("pref_darkStatusBar", true)
            .putString("pref_iconShape", "circle")
            .putBoolean("prefs_wrapAdaptive", true)
            .putString("pref_launcherTheme", if(legacyDark) "dark" else "light")
            .putInt("pref_workspaceColumns", columns)
            .putInt("pref_workspaceRows", rows)
            .putInt("pref_hotseatColumns", 4)
            .putBoolean("themed_icons", true)
            .putBoolean("drawer_themed_icons", true)
            .putBoolean("pref_shadowBGIcons", false)
            .putBoolean("pref_autoLaunchRoot", false)
            .putBoolean("pref_add_icon_to_home", false)
            .putBoolean("pref_searchResultStartPageSuggestion", false)
            .commit()
        marker.edit().putBoolean("foundationDefaults", true).putBoolean("dark", legacyDark).putString("material", "classic").apply()
    }
}
