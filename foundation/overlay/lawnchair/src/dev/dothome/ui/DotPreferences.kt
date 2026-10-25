package dev.dothome.ui

import android.app.Activity
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.theme.color.ColorOption
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.R
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.setBlocking
import java.util.concurrent.Executors

/** Launcher-owned settings. No web/update/font-download/privileged-gesture routes. */
open class DotPreferences : Activity() {
    private val p by lazy { PreferenceManager.getInstance(this) }
    private val p2 by lazy { PreferenceManager2.getInstance(this) }
    private val style by lazy { getSharedPreferences("dot-style", 0) }
    private lateinit var body: LinearLayout
    private var route = "customisation"
    private val history = java.util.ArrayDeque<String>()
    private val work = Executors.newSingleThreadExecutor()
    private val dark get() = when (p.launcherTheme.get()) {
        "dark" -> true
        "light" -> false
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private val ink get() = if (dark) Color.rgb(241,241,236) else Color.rgb(24,24,23)
    private val secondary get() = if (dark) Color.rgb(169,169,161) else Color.rgb(104,104,98)
    private val paper get() = if (dark) Color.rgb(16,16,16) else Color.rgb(245,245,241)
    private val card get() = if (dark) Color.rgb(34,34,33) else Color.WHITE
    private fun d(value: Float) = (value * resources.displayMetrics.density).toInt()
    private fun d(value: Int) = d(value.toFloat())
    private fun text(value: String, size: Float = 15f, color: Int = ink) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = resources.getFont(R.font.geist_regular)
        gravity = Gravity.CENTER_VERTICAL
    }
    private fun shape(color: Int, radius: Int = 20) = GradientDrawable().apply {
        setColor(color); cornerRadius = d(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        show(savedInstanceState?.getString("route") ?: intent.getStringExtra("dotRoute") ?: "customisation", false)
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putString("route", route)
        super.onSaveInstanceState(state)
    }

    override fun onDestroy() { work.shutdownNow(); super.onDestroy() }

    override fun onBackPressed() {
        if (history.isEmpty()) finish() else show(history.removeLast(), false)
    }

    private fun show(next: String, remember: Boolean = true) {
        if (remember && next != route) history.addLast(route)
        route = next
        setTheme(if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        val root = LinearLayout(this).apply { orientation = 1; setBackgroundColor(paper) }
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(d(20), bars.top, d(20), bars.bottom + d(12)); insets
        }
        val header = FrameLayout(this)
        val back = text("‹", 30f).apply { gravity = Gravity.CENTER; contentDescription = "Back"; setOnClickListener { onBackPressed() } }
        header.addView(back, FrameLayout.LayoutParams(d(44), -1, Gravity.START))
        val title = text(when(next) {
            "appearance" -> "Theme & Style"; "wallpaper" -> "Wallpapers"; "grid" -> "Home screen layout"
            "icons" -> "Icon pack"; "home" -> "Home settings"; "access" -> "Optional access"
            else -> "Customisation"
        }, 17f).apply { gravity = Gravity.CENTER; setAccessibilityHeading(true) }
        header.addView(title, FrameLayout.LayoutParams(-1, -1).apply { leftMargin = d(48); rightMargin = d(48) })
        root.addView(header, LinearLayout.LayoutParams(-1, d(56)))
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipToPadding = false }
        body = LinearLayout(this).apply { orientation = 1; setPadding(0, d(12), 0, d(12)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        window.decorView.windowInsetsController?.setSystemBarsAppearance(
            if (dark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        when(next) {
            "appearance" -> appearance(); "wallpaper" -> wallpapers(); "grid" -> grid(); "icons" -> icons()
            "home" -> home(); "access" -> access(); else -> customisation()
        }
    }

    private fun gap(height: Int = 16) { body.addView(View(this), LinearLayout.LayoutParams(1, d(height))) }
    private fun row(title: String, value: String = "", action: (() -> Unit)? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(d(18), 0, d(18), 0)
            background = shape(card, 18)
        }
        row.addView(text(title), LinearLayout.LayoutParams(0, -1, 1f))
        if (value.isNotEmpty()) row.addView(text(value, 12f, secondary), LinearLayout.LayoutParams(-2, -1))
        if (action != null) {
            row.addView(text("›", 22f, secondary).apply { setPadding(d(12), 0, 0, 0) })
            row.setOnClickListener { action() }; row.isFocusable = true
        }
        body.addView(row, LinearLayout.LayoutParams(-1, d(58)).apply { bottomMargin = d(2) })
        return row
    }

    private fun toggle(title: String, checked: Boolean, change: (Boolean) -> Unit) {
        val row = row(title)
        row.addView(Switch(this).apply {
            isChecked = checked; contentDescription = title
            setOnCheckedChangeListener { _, value -> change(value) }
        }, LinearLayout.LayoutParams(-2, -2))
    }

    private fun preview(height: Int) {
        body.addView(CurrentHomePreview(), LinearLayout.LayoutParams(-1, d(height)))
    }

    private fun customisation() {
        preview(310); gap(20)
        row("Theme & Style", when(style.getString("material", "classic")) { "translucent" -> "Translucent"; "dynamic" -> "Dynamic"; else -> "Classic" }) { show("appearance") }
        row("Wallpaper") { show("wallpaper") }
        row("Home screen layout", "${p.workspaceColumns.get()} × ${p.workspaceRows.get()}") { show("grid") }
        row("Icon pack", packLabel()) { show("icons") }
    }

    private fun appearance() {
        preview(320); gap(12)
        val choices = LinearLayout(this).apply { background = shape(if(dark) 0xff262625.toInt() else 0xffe9e9e4.toInt(), 100); setPadding(d(3), d(3), d(3), d(3)) }
        listOf("translucent" to "Translucent", "classic" to "Classic", "dynamic" to "Dynamic").forEach { (key, label) ->
            choices.addView(text(label, 12f).apply {
                gravity = Gravity.CENTER
                if (style.getString("material", "classic") == key) background = shape(card, 100)
                setOnClickListener {
                    style.edit().putString("material", key).apply()
                    p2.accentColor.setBlocking(if(key == "classic") ColorOption.CustomColor(0xffd71920.toInt()) else ColorOption.WallpaperPrimary)
                    show("appearance", false)
                }
            }, LinearLayout.LayoutParams(0, d(42), 1f))
        }
        body.addView(choices); gap(18)
        toggle("Dark theme", dark) { style.edit().putBoolean("dark", it).apply(); p.launcherTheme.set(if(it) "dark" else "light"); show("appearance", false) }
        val colours = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(d(8), d(12), d(8), d(12)); background = shape(card, 14) }
        intArrayOf(0xffbccbbf.toInt(),0xff56a5c7.toInt(),0xff99bd68.toInt(),0xffd7a28d.toInt(),0xffb3a2bf.toInt(),0xff8894ac.toInt()).forEach { color ->
            colours.addView(View(this).apply {
                background = shape(color, 100); contentDescription = "Accent ${Integer.toHexString(color)}"
                setOnClickListener { style.edit().putInt("accent",color).apply(); p2.accentColor.setBlocking(ColorOption.CustomColor(color)); invalidate() }
            }, LinearLayout.LayoutParams(d(34), d(34)).apply { leftMargin = d(6); rightMargin = d(6) })
        }
        body.addView(colours, LinearLayout.LayoutParams(-1, d(62)).apply { bottomMargin = d(2) })
        row("Icon pack", packLabel()) { show("icons") }
        toggle("Apply icons to Home screen only", !p.drawerThemedIcons.get()) { p.drawerThemedIcons.set(!it) }
    }

    private fun packLabel(): String {
        val pack = p.iconPackPackage.get()
        return if(pack.isEmpty()) "Monochrome" else runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pack, 0)).toString() }.getOrDefault("Unavailable pack")
    }

    private fun icons() {
        val preview = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, d(20), 0, d(28)) }
        LawnchairLauncher.instance?.appsView?.appsStore?.apps?.take(5)?.forEach { app ->
            preview.addView(ImageView(this).apply { setImageBitmap(app.bitmap.icon); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(d(50), d(50)).apply { leftMargin = d(7); rightMargin = d(7) })
        }
        body.addView(preview)
        row("Monochrome", if(p.iconPackPackage.get().isEmpty() && p.themedIcons.get()) "Selected" else "") { p.iconPackPackage.set(""); p.themedIcons.set(true); show("icons", false) }
        row("System icons", if(p.iconPackPackage.get().isEmpty() && !p.themedIcons.get()) "Selected" else "") { p.iconPackPackage.set(""); p.themedIcons.set(false); show("icons", false) }
        val packs = packageManager.queryIntentActivities(Intent("org.adw.launcher.THEMES"), 0).distinctBy { it.activityInfo.packageName }
        packs.forEach { info ->
            val key = info.activityInfo.packageName
            row(info.loadLabel(packageManager).toString(), if(p.iconPackPackage.get() == key) "Selected" else "") { p.iconPackPackage.set(key); show("icons", false) }
        }
        gap(); body.addView(text("Installed icon packs appear here. Everything is loaded locally.", 13f, secondary))
    }

    private fun grid() {
        preview(300); gap()
        listOf(4 to 6,4 to 7,5 to 6,5 to 7).forEach { (columns, rows) ->
            row("$columns × $rows", if(p.workspaceColumns.get() == columns && p.workspaceRows.get() == rows) "Selected" else "") {
                p.batchEdit { p.workspaceColumns.set(columns); p.workspaceRows.set(rows) }
                InvariantDeviceProfile.INSTANCE.get(this).onPreferencesChanged(this)
                show("grid", false)
            }
        }
        gap()
        toggle("App labels", p2.showIconLabelsOnHomeScreen.firstBlocking()) { p2.showIconLabelsOnHomeScreen.setBlocking(it) }
        toggle("Labels in folders", p2.showIconLabelsOnHomeScreenFolder.firstBlocking()) { p2.showIconLabelsOnHomeScreenFolder.setBlocking(it) }
    }

    private fun wallpapers() {
        val grid = GridLayout(this).apply { columnCount = 2 }
        listOf("glass" to "Glass", "optic" to "Optic", "mist" to "Atmosphere", "graphite" to "Graphite").forEachIndexed { index, (key, label) ->
            val cardView = LinearLayout(this).apply { orientation = 1; setPadding(d(4),d(4),d(4),d(4)) }
            cardView.addView(object: View(this) {
                override fun onDraw(canvas: Canvas) { DotWallpaper.paint(canvas, width, height, key) }
            }, LinearLayout.LayoutParams(-1, d(238)))
            cardView.addView(text(label, 13f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1,d(38)))
            cardView.setOnClickListener {
                work.execute {
                    val result = runCatching {
                        val bitmap = DotWallpaper.bitmap(key, 1080, 2400)
                        WallpaperManager.getInstance(this).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        bitmap.recycle()
                        style.edit().putString("wallpaper", key).putBoolean("wallpaperApplied", true).apply()
                    }
                    runOnUiThread { Toast.makeText(this, if(result.isSuccess) "Wallpaper applied" else "Wallpaper unavailable", Toast.LENGTH_SHORT).show() }
                }
            }
            grid.addView(cardView, GridLayout.LayoutParams().apply { width=0; columnSpec=GridLayout.spec(index % 2,1f) })
        }
        body.addView(grid); gap()
        row("Choose a local image") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), 61) }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if(request == 61 && result == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            work.execute {
                val success = runCatching { contentResolver.openInputStream(uri).use { WallpaperManager.getInstance(this).setStream(it, null, true, WallpaperManager.FLAG_SYSTEM) }; style.edit().putBoolean("wallpaperApplied", true).putString("wallpaper", "local").apply() }.isSuccess
                runOnUiThread { Toast.makeText(this, if(success) "Wallpaper applied" else "Image unavailable", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun home() {
        row("Home screen layout") { show("grid") }
        row("Theme & Style") { show("appearance") }
        gap()
        toggle("Freeze Home screen layout", p2.lockHomeScreen.firstBlocking()) { p2.lockHomeScreen.setBlocking(it) }
        toggle("App labels in drawer", p2.showIconLabelsInDrawer.firstBlocking()) { p2.showIconLabelsInDrawer.setBlocking(it) }
        gap()
        val role = getSystemService(RoleManager::class.java)
        row("Default Home app", if(role?.isRoleHeld(RoleManager.ROLE_HOME) == true) "Dot Home" else "Choose") { if(role != null) startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME), 62) else startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        row("Hidden icons") { startActivity(Intent(this,DotHidden::class.java)) }
        row("Optional access") { show("access") }
        gap(); body.addView(text("Dot Home · ${com.android.launcher3.BuildConfig.VERSION_NAME}", 12f, secondary))
    }

    private fun access() {
        row("Media and notification dots") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        row("Usage history") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        gap(); body.addView(text("Core Home, Apps and arrangement need no runtime grants. Calendar, flashlight and photos ask when you use them.", 14f, secondary))
    }

    private inner class CurrentHomePreview: View(this@DotPreferences) {
        override fun onDraw(canvas: Canvas) {
            val home = LawnchairLauncher.instance?.rootView ?: return
            if(home.width == 0 || home.height == 0) return
            val scale = minOf(width.toFloat()/home.width, height.toFloat()/home.height)
            val w = home.width*scale; val h = home.height*scale
            canvas.save(); canvas.translate((width-w)/2, (height-h)/2)
            val clip = Path().apply { addRoundRect(RectF(0f,0f,w,h),d(22).toFloat(),d(22).toFloat(),Path.Direction.CW) }
            canvas.clipPath(clip)
            DotWallpaper.paint(canvas,w.toInt(),h.toInt(),style.getString("wallpaper","glass")!!)
            canvas.scale(scale,scale)
            dev.dothome.widgets.TileRefresh.sync(home)
            runCatching { home.draw(canvas) }
            canvas.restore()
        }
    }
}
