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
import app.lawnchair.preferences2.firstBlocking as idpValue
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
    private val scrollPositions=mutableMapOf<String,Int>()
    private var currentScroll: ScrollView?=null
    private val backCallback=object: android.window.OnBackAnimationCallback {
        override fun onBackStarted(event: android.window.BackEvent) {}
        override fun onBackProgressed(event: android.window.BackEvent) {if(!appearance.reducedMotion)window.decorView.translationX=d(24)*event.progress}
        override fun onBackCancelled() {window.decorView.animate().translationX(0f).setDuration(if(appearance.reducedMotion)0 else 150).start()}
        override fun onBackInvoked() {window.decorView.translationX=0f;onBackPressed()}
    }
    private var gridChoice: Pair<Int,Int>?=null
    private val dark get() = when (p.launcherTheme.get()) {
        "dark" -> true
        "light" -> false
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    private val appearance get() = DotAppearance.get(this)
    private val ink get() = appearance.ink
    private val secondary get() = appearance.secondary
    private val paper get() = appearance.background
    private val card get() = appearance.card
    private fun d(value: Float) = (value * resources.displayMetrics.density).toInt()
    private fun d(value: Int) = d(value.toFloat())
    private fun text(value: String, size: Float = 15f, color: Int = ink) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = resources.getFont(R.font.geist_regular)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
    }
    private fun shape(color: Int, radius: Int = 20) = GradientDrawable().apply {
        setColor(color); cornerRadius = d(radius).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback)
        savedInstanceState?.getStringArrayList("history")?.let {history.addAll(it)}
        savedInstanceState?.getBundle("positions")?.let {positions->positions.keySet().forEach {scrollPositions[it]=positions.getInt(it)}}
        window.setDecorFitsSystemWindows(false)
        show(savedInstanceState?.getString("route") ?: intent.getStringExtra("dotRoute") ?: "customisation", false)
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putStringArrayList("history",ArrayList(history));currentScroll?.let {scrollPositions[route]=it.scrollY}
        val positions=Bundle();scrollPositions.forEach {(key,value)->positions.putInt(key,value)};state.putBundle("positions",positions)
        state.putString("route", route)
        super.onSaveInstanceState(state)
    }

    override fun onDestroy() {onBackInvokedDispatcher.unregisterOnBackInvokedCallback(backCallback);work.shutdownNow();super.onDestroy()}

    override fun onBackPressed() {
        if (history.isEmpty()) finish() else show(history.removeLast(), false)
    }

    private fun show(next: String, remember: Boolean = true) {
        currentScroll?.let {scrollPositions[route]=it.scrollY}
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
        val back = text(if(resources.configuration.layoutDirection==View.LAYOUT_DIRECTION_RTL)"›" else "‹", 30f).apply { gravity = Gravity.CENTER; contentDescription = "Back"; setOnClickListener { onBackPressed() } }
        header.addView(back, FrameLayout.LayoutParams(d(44), -1, Gravity.START))
        val title = text(when(next) {
            "appearance" -> "Theme & Style"; "wallpaper" -> "Wallpapers"; "grid" -> "Home screen layout"
            "icons" -> "Icon pack"; "drawer" -> "Apps"; "home" -> "Home settings"; "access" -> "Optional access"; "licenses" -> "Open-source licences"; "search" -> "Search"; "layouts" -> "Starting layouts"
            else -> "Customisation"
        }, 17f).apply { gravity = Gravity.CENTER; setAccessibilityHeading(true) }
        header.addView(title, FrameLayout.LayoutParams(-1, -1).apply { leftMargin = d(48); rightMargin = d(48) })
        root.addView(header, LinearLayout.LayoutParams(-1, d(56)))
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipToPadding = false };currentScroll=scroll
        body = LinearLayout(this).apply { orientation = 1; setPadding(0, d(12), 0, d(12)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        window.decorView.windowInsetsController?.setSystemBarsAppearance(
            if (dark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        when(next) {
            "appearance" -> appearance(); "drawer" -> drawer(); "wallpaper" -> wallpapers(); "grid" -> grid(); "icons" -> icons()
            "home" -> home(); "access" -> access(); "licenses" -> licences(); "search" -> search(); "layouts" -> layouts(); else -> customisation()
        }
        scroll.post {scroll.scrollTo(0,scrollPositions[next] ?: 0)}
        if(remember)DotUi.enter(body)
    }

    private fun gap(height: Int = 16) { body.addView(View(this), LinearLayout.LayoutParams(1, d(height))) }
    private fun row(title: String, value: String = "", action: (() -> Unit)? = null): LinearLayout {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(d(18), 0, d(18), 0)
            background = DotUi.clickable(this@DotPreferences,card,16f)
        }
        row.addView(text(title,14f).apply {setPadding(0,d(16),d(8),d(16));maxLines=3}, LinearLayout.LayoutParams(0, -2, 1f))
        if (value.isNotEmpty()) row.addView(text(value, 12f, secondary), LinearLayout.LayoutParams(-2, -1))
        if (action != null) {
            row.addView(text("›", 22f, secondary).apply { setPadding(d(12), 0, 0, 0) })
            row.setOnClickListener { action() }; row.isFocusable = true
        }
        row.minimumHeight=d(54)
        body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = d(2) })
        return row
    }

    private fun toggle(title: String, checked: Boolean, change: (Boolean) -> Unit) {
        val row = row(title)
        val control=Switch(this).apply {
            isChecked = checked; contentDescription = title
            val states=arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf())
            thumbTintList=android.content.res.ColorStateList(states,intArrayOf(if(dark)0xff222321.toInt() else Color.WHITE,if(dark)0xffbfc3bd.toInt() else Color.WHITE))
            trackTintList=android.content.res.ColorStateList(states,intArrayOf(if(dark)0xffeeeeea.toInt() else 0xff222321.toInt(),if(dark)0xff444743.toInt() else 0xffd5d7d0.toInt()))
            setOnCheckedChangeListener { _, value -> change(value) }
        }
        row.addView(control, LinearLayout.LayoutParams(-2,-2))
        row.setOnClickListener {control.toggle()}
    }

    private fun preview(height: Int) {
        val home=LawnchairLauncher.instance
        val pages=home?.workspace?.pageCount ?: 1
        val strip=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        val scroll=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;clipToPadding=false;isFillViewport=true}
        for(index in 0 until pages.coerceIn(1,6)) {
            val column=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER}
            column.addView(CurrentHomePreview(index),LinearLayout.LayoutParams(d((height-22)*.46f),d(height-22)))
            column.addView(text("Page ${index+1}",11f,secondary).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,d(22)))
            strip.addView(column,LinearLayout.LayoutParams(-2,-1).apply {marginStart=d(10);marginEnd=d(10)})
        }
        scroll.addView(strip);body.addView(scroll,LinearLayout.LayoutParams(-1,d(height)))
        if(pages==1)strip.gravity=Gravity.CENTER
    }

    private fun applyPalette() {
        val a=appearance
        p2.accentColor.setBlocking(ColorOption.CustomColor(a.accent))
        LawnchairLauncher.instance?.rootView?.let {dev.dothome.widgets.TileRefresh.sync(it);it.invalidate()}
    }

    private fun search() {
        toggle("Apps",p.searchResultApps.get()) {p.searchResultApps.set(it)}
        toggle("Calculator",p.searchResultCalculator.get()) {p.searchResultCalculator.set(it)}
        toggle("Settings",p.searchResultSettingsEntry.get()) {p.searchResultSettingsEntry.set(it)}
        toggle("App shortcuts",p.searchResultShortcuts.get()) {p.searchResultShortcuts.set(it)}
        gap()
        row("Hidden apps in search",when(p2.hiddenAppsInSearch.firstBlocking()) {"on"->"Show";"if_name_typed"->"Exact name only";else->"Hide"}) {
            val choices=arrayOf("Hide","Exact name only","Show")
            android.app.AlertDialog.Builder(this).setTitle("Hidden apps in search").setItems(choices) {_,which->p2.hiddenAppsInSearch.setBlocking(arrayOf("off","if_name_typed","on")[which]);show("search",false)}.setNegativeButton("Cancel",null).show()
        }
    }

    private fun customisation() {
        preview(310); gap(20)
        row("Theme & Style", when(style.getString("material", "classic")) { "translucent" -> "Lucent"; "dynamic" -> "Dynamic"; else -> "Classic" }) { show("appearance") }
        row("Wallpaper") { show("wallpaper") }
        row("Home screen layout", "${p.workspaceColumns.get()} × ${p.workspaceRows.get()}") { show("grid") }
        row("Icon pack", packLabel()) { show("icons") }
        gap();row("Starting layouts","Add a page") {show("layouts")}
    }

    private fun layouts() {
        preview(270);gap()
        body.addView(text("Add a new page with a complete starting layout. Your current pages and dock stay as they are.",14f,secondary))
        gap()
        row("Classic layout","Clocks, weather & controls") {LawnchairLauncher.instance?.let {DotPresets.add(it,false)};finish()}
        row("Lucent layout","Away, Focus & Collector") {LawnchairLauncher.instance?.let {DotPresets.add(it,true)};finish()}
    }

    private fun appearance() {
        preview(320); gap(12)
        val choices = LinearLayout(this).apply { background = shape(if(dark) 0xff262625.toInt() else 0xffe9e9e4.toInt(), 100); setPadding(d(3), d(3), d(3), d(3)) }
        listOf("translucent" to "Lucent", "classic" to "Classic", "dynamic" to "Dynamic").forEach { (key, label) ->
            choices.addView(text(label, 12f).apply {
                gravity = Gravity.CENTER
                if (style.getString("material", "classic") == key) background = shape(card, 100)
                setOnClickListener {
                    style.edit().putString("material", key).apply()
                    applyPalette()
                    show("appearance", false)
                }
            }, LinearLayout.LayoutParams(0, d(42), 1f))
        }
        body.addView(choices); gap(18)
        toggle("Dark theme", dark) {
            style.edit().putBoolean("dark",it).apply();p.launcherTheme.set(if(it)"dark" else "light");applyPalette();show("appearance",false)
        }
        row("Colour source",if(style.getBoolean("manualAccent",false))"Chosen palette" else "Wallpaper") {
            style.edit().putBoolean("manualAccent",false).apply();DotAppearance.refreshWallpaper(this);applyPalette();show("appearance",false)
        }
        val colours = LinearLayout(this).apply { gravity=Gravity.CENTER;setPadding(d(8),d(12),d(8),d(12));background=shape(card,16) }
        val colourNames=listOf("Sage","Blue","Lime","Terracotta","Lilac","Slate")
        intArrayOf(0xff81a98e.toInt(),0xff4b94b8.toInt(),0xff91af4a.toInt(),0xffb9755c.toInt(),0xff9a7cad.toInt(),0xff61728b.toInt()).forEachIndexed { index,colour ->
            colours.addView(object: View(this) {
                val paint=Paint(3)
                init {contentDescription="${colourNames[index]} accent";isFocusable=true;setOnClickListener {style.edit().putInt("accent",colour).putBoolean("manualAccent",true).apply();applyPalette();show("appearance",false)}}
                override fun onDraw(canvas: Canvas) {
                    val r=minOf(width,height)*.37f;val cx=width/2f;val cy=height/2f
                    canvas.save();val path=Path();path.addCircle(cx,cy,r,Path.Direction.CW);canvas.clipPath(path)
                    paint.color=DotAppearance.tone(colour,.85f,.32f);canvas.drawRect(cx-r,cy-r,cx+r,cy+r,paint)
                    paint.color=DotAppearance.tone(colour,.35f,.46f);canvas.drawRect(cx-r,cy-r,cx,cy,paint)
                    paint.color=DotAppearance.tone(colour,.62f,.62f);canvas.drawRect(cx,cy,cx+r,cy+r,paint);canvas.restore()
                    if(style.getBoolean("manualAccent",false) && style.getInt("accent",0)==colour){paint.color=ink;paint.style=Paint.Style.STROKE;paint.strokeWidth=d(1).toFloat();canvas.drawCircle(cx,cy,r+d(3),paint);paint.style=Paint.Style.FILL}
                }
            },LinearLayout.LayoutParams(0,d(48),1f))
        }
        body.addView(colours,LinearLayout.LayoutParams(-1,d(72)));gap(2)
        row("Icon pack", packLabel()) { show("icons") }
        if(p.iconPackPackage.get().isEmpty() && p.themedIcons.get()) toggle("Apply icons to Home screen only",style.getBoolean("homeOnlyIcons",false)) {style.edit().putBoolean("homeOnlyIcons",it).apply();p.drawerThemedIcons.set(!it)}
    }

    private fun packLabel(): String {
        val pack = p.iconPackPackage.get()
        return if(pack.isEmpty()) {if(p.themedIcons.get())"Monochrome" else "System icons"} else runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pack, 0)).toString() }.getOrDefault("Unavailable pack")
    }

    private fun icons() {
        val preview = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, d(20), 0, d(28)) }
        LawnchairLauncher.instance?.appsView?.appsStore?.apps?.take(5)?.forEach { app ->
            preview.addView(ImageView(this).apply { setImageDrawable(app.newIcon(this@DotPreferences,p.themedIcons.get())); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(d(50), d(50)).apply { leftMargin = d(7); rightMargin = d(7) })
        }
        body.addView(preview)
        row("Monochrome", if(p.iconPackPackage.get().isEmpty() && p.themedIcons.get()) "Selected" else "") { p.iconPackPackage.set(""); p.themedIcons.set(true); p.drawerThemedIcons.set(!style.getBoolean("homeOnlyIcons",false)); show("icons", false) }
        row("System icons", if(p.iconPackPackage.get().isEmpty() && !p.themedIcons.get()) "Selected" else "") { p.iconPackPackage.set(""); p.themedIcons.set(false); p.drawerThemedIcons.set(false); show("icons", false) }
        val packs = packageManager.queryIntentActivities(Intent("org.adw.launcher.THEMES"), 0).distinctBy { it.activityInfo.packageName }
        packs.forEach { info ->
            val key = info.activityInfo.packageName
            row(info.loadLabel(packageManager).toString(), if(p.iconPackPackage.get() == key) "Selected" else "") { p.iconPackPackage.set(key); p.themedIcons.set(false); p.drawerThemedIcons.set(false); show("icons", false) }
        }
        gap(); body.addView(text("Choose a pack installed on this phone.", 13f, secondary))
    }

    private fun grid() {
        preview(230);gap()
        val selected=gridChoice ?: (p.workspaceColumns.get() to p.workspaceRows.get())
        val layouts=GridLayout(this).apply {columnCount=2}
        listOf(4 to 6,4 to 7,5 to 6,5 to 7).forEachIndexed {index,pair->
            val (columns,rows)=pair
            val option=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER;setPadding(d(12),d(12),d(12),d(10));background=shape(card,20).apply {if(pair==selected)setStroke(d(2),ink)};contentDescription="$columns by $rows grid";isFocusable=true;setOnClickListener {gridChoice=pair;show("grid",false)}}
            option.addView(object: View(this){val paint=Paint(3);override fun onDraw(canvas: Canvas){paint.color=secondary;val cell=minOf(width.toFloat()/(columns+1),height.toFloat()/(rows+1));val left=(width-cell*(columns-1))/2;val top=(height-cell*(rows-1))/2;for(y in 0 until rows)for(x in 0 until columns)canvas.drawCircle(left+x*cell,top+y*cell,d(2).toFloat(),paint)}},LinearLayout.LayoutParams(-1,d(105)))
            option.addView(text("$columns × $rows",14f).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,d(32)))
            layouts.addView(option,GridLayout.LayoutParams().apply {width=0;columnSpec=GridLayout.spec(index%2,1f);setMargins(d(3),d(3),d(3),d(3))})
        };body.addView(layouts)
        if(selected!=(p.workspaceColumns.get() to p.workspaceRows.get()))body.addView(DotUi.button(this,"Apply grid") {
            if(LawnchairLauncher.instance?.let {DotArrange.locked(it)}==true)return@button
            p.batchEdit {p.workspaceColumns.set(selected.first);p.workspaceRows.set(selected.second)}
            InvariantDeviceProfile.INSTANCE.get(this).onPreferencesChanged(this);gridChoice=null;show("grid",false)
        },LinearLayout.LayoutParams(-1,-2))
        gap();toggle("App labels",p2.showIconLabelsOnHomeScreen.firstBlocking()){p2.showIconLabelsOnHomeScreen.setBlocking(it)}
        toggle("Labels in folders",p2.showIconLabelsOnHomeScreenFolder.firstBlocking()){p2.showIconLabelsOnHomeScreenFolder.setBlocking(it)}
        gap();row("Dock","4 apps")
        toggle("Search bar below dock",p2.hotseatMode.firstBlocking().toString()!="disabled") {p2.hotseatMode.setBlocking(if(it)app.lawnchair.hotseat.LawnchairHotseat else app.lawnchair.hotseat.DisabledHotseat)}
    }

    private fun drawer() {
        row("Alphabetical",if(p.drawerList.get()) "Selected" else "") {p.drawerList.set(true);show("drawer",false)}
        row("Smart categories",if(!p.drawerList.get()) "Selected" else "") {p.drawerList.set(false);show("drawer",false)}
        gap()
        val defaultGrid=InvariantDeviceProfile.INSTANCE.get(this).closestProfile
        for(columns in listOf(4,5)) row("$columns columns",if(p2.drawerColumns.idpValue(defaultGrid)==columns) "Selected" else "") {kotlinx.coroutines.runBlocking {p2.drawerColumns.set(columns,defaultGrid)};show("drawer",false)}
        gap()
        toggle("App labels",p2.showIconLabelsInDrawer.firstBlocking()) {p2.showIconLabelsInDrawer.setBlocking(it)}
        row("Search") {show("search")}
        toggle("Search bar in drawer",!p2.hideAppDrawerSearchBar.firstBlocking()) {p2.hideAppDrawerSearchBar.setBlocking(!it)}
        row("Hidden icons") {startActivity(Intent(this,DotHidden::class.java))}
    }

    private fun wallpapers() {
        val grid = GridLayout(this).apply { columnCount = 2 }
        listOf("glass" to "Glass", "optic" to "Optic", "mist" to "Mist", "graphite" to "Graphite", "prism" to "Prism", "noir" to "Noir").forEachIndexed { index, (key, label) ->
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
                        p2.darkStatusBar.setBlocking(key!="optic" && key!="graphite" && key!="noir")
                        style.edit().putString("wallpaper", key).putBoolean("wallpaperApplied", true).putBoolean("externalWallpaper",false).putInt("ownedWallpaperId",WallpaperManager.getInstance(this).getWallpaperId(WallpaperManager.FLAG_SYSTEM)).apply()
                        DotAppearance.refreshWallpaper(this)
                    }
                    runOnUiThread { Toast.makeText(this, if(result.isSuccess) "Wallpaper applied" else "Wallpaper unavailable", Toast.LENGTH_SHORT).show() }
                }
            }
            grid.addView(cardView, GridLayout.LayoutParams().apply { width=0; columnSpec=GridLayout.spec(index % 2,1f) })
        }
        body.addView(grid); gap()
        style.getString("backupWallpaper",null)?.let {saved->row("Saved backup image") {
            val item=org.json.JSONObject().put("type","photo").put("style",0).put("uri",saved).put("wallpaper",true)
            startActivity(Intent(this,dev.dothome.widgets.TileConfiguration::class.java).putExtra("initial",item.toString()))
        }}
        row("Choose a local image") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), 61) }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if(result==RESULT_OK && data?.data!=null) {
            if(request==71) {DotBackup.export(this,data.data!!);return}
            if(request==72) {DotBackup.inspect(this,data.data!!);return}
        }
        if(request == 61 && result == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            runCatching {contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val item=org.json.JSONObject().put("type","photo").put("style",0).put("uri",uri.toString()).put("wallpaper",true)
            startActivity(Intent(this,dev.dothome.widgets.TileConfiguration::class.java).putExtra("initial",item.toString()))
        }
    }

    private fun home() {
        row("Home screen layout") { show("grid") }
        row("Apps", if(p.drawerList.get()) "Alphabetical" else "Smart categories") { show("drawer") }
        row("Theme & Style") { show("appearance") }
        row("Search") {show("search")}
        toggle("Reduce motion",style.getBoolean("reduceMotion",false)) {style.edit().putBoolean("reduceMotion",it).apply()}
        gap()
        toggle("Freeze Home screen layout", p2.lockHomeScreen.firstBlocking()) { p2.lockHomeScreen.setBlocking(it) }
        toggle("App labels in drawer", p2.showIconLabelsInDrawer.firstBlocking()) { p2.showIconLabelsInDrawer.setBlocking(it) }
        gap()
        val role = getSystemService(RoleManager::class.java)
        row("Default Home app", if(role?.isRoleHeld(RoleManager.ROLE_HOME) == true) "Dot Home" else "Choose") { if(role != null) startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME), 62) else startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        row("Hidden icons") { startActivity(Intent(this,DotHidden::class.java)) }
        row("Back up Home layout") { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"dot-home-layout.json"),71) }
        row("Restore Home layout") { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),72) }
        row("Optional access") { show("access") }
        row("Open-source licences") { show("licenses") }
        gap(); body.addView(text("Dot Home · ${com.android.launcher3.BuildConfig.VERSION_NAME}", 12f, secondary))
    }

    private fun licences() {
        assets.list("licenses")?.sorted()?.forEach { name ->
            body.addView(text(name.removeSuffix(".txt"),14f).apply {setTypeface(typeface,Typeface.BOLD)},LinearLayout.LayoutParams(-1,d(48)))
            body.addView(text(assets.open("licenses/$name").bufferedReader().use {it.readText()},11f,secondary).apply {setTextIsSelectable(true)},LinearLayout.LayoutParams(-1,-2))
            gap(20)
        }
    }

    private fun access() {
        row("Media and notification dots") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        row("Usage history") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        gap(); body.addView(text("Home and app organisation work without extra access. Other features ask only when needed.", 14f, secondary))
    }

    private inner class CurrentHomePreview(private val index: Int): View(this@DotPreferences) {
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            for(step in 1..8) postDelayed({if(isAttachedToWindow)invalidate()},step*120L)
        }
        override fun onDraw(canvas: Canvas) {
            val home = LawnchairLauncher.instance?.rootView ?: return
            if(home.width == 0 || home.height == 0) return
            val scale = minOf(width.toFloat()/home.width, height.toFloat()/home.height)
            val w = home.width*scale; val h = home.height*scale
            canvas.save(); canvas.translate((width-w)/2, (height-h)/2)
            val clip = Path().apply { addRoundRect(RectF(0f,0f,w,h),d(22).toFloat(),d(22).toFloat(),Path.Direction.CW) }
            canvas.clipPath(clip)
            DotBackdrop.draw(this@DotPreferences,canvas,RectF(0f,0f,w,h))
            canvas.scale(scale,scale)
            dev.dothome.widgets.TileRefresh.sync(home)
            runCatching {
                val launcher=LawnchairLauncher.instance!!;val workspace=launcher.workspace
                if(index==workspace.currentPage) home.draw(canvas)
                else {
                    val page=workspace.getPageAt(index)
                    canvas.save();canvas.translate(workspace.left.toFloat()+workspace.paddingLeft,workspace.top.toFloat());page?.draw(canvas);canvas.restore()
                    canvas.save();canvas.translate(launcher.hotseat.left.toFloat(),launcher.hotseat.top.toFloat());launcher.hotseat.draw(canvas);canvas.restore()
                }
            }
            canvas.restore()
        }
    }
}
