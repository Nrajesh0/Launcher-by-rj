package dev.dothome.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.setBlocking

/** Separate hidden-icons page backed by the native drawer's component/profile keys. */
class DotHidden : Activity() {
    private val prefs by lazy {PreferenceManager2.getInstance(this)}
    private val dark get()=getSharedPreferences("dot-style",0).getBoolean("dark",false)
    private val ink get()=DotAppearance.get(this).ink
    private val paper get()=DotAppearance.get(this).background
    private fun d(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun text(value: String)=TextView(this).apply {text=value;textSize=15f;setTextColor(ink);typeface=resources.getFont(com.android.launcher3.R.font.geist_regular);gravity=Gravity.CENTER}
    private val apps get()=LawnchairLauncher.instance?.appsView?.appsStore?.apps?.sortedBy {it.title.toString().lowercase()} ?: emptyList()
    override fun onCreate(state: Bundle?) {super.onCreate(state);show()}
    override fun onResume() {super.onResume();show()}
    private fun show() {
        window.setDecorFitsSystemWindows(false)
        val body=LinearLayout(this).apply {orientation=1;setBackgroundColor(paper);setPadding(d(16),0,d(16),0)}
        setContentView(body)
        window.decorView.windowInsetsController?.setSystemBarsAppearance(if(dark)0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        body.setOnApplyWindowInsetsListener {view,insets->val bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(d(16),bars.top,d(16),bars.bottom);insets}
        val header=LinearLayout(this)
        header.addView(text("‹").apply {contentDescription="Back";setOnClickListener {finish()}},LinearLayout.LayoutParams(d(48),d(56)))
        header.addView(text("Hidden icons").apply {textSize=17f},LinearLayout.LayoutParams(0,d(56),1f))
        header.addView(text("+").apply {textSize=24f;contentDescription="Manage hidden icons";setOnClickListener {manage()}},LinearLayout.LayoutParams(d(48),d(56)));body.addView(header)
        val hidden=prefs.hiddenApps.firstBlocking()
        val selected=apps.filter {it.toComponentKey().toString() in hidden}
        if(selected.isEmpty()) body.addView(text("Tap + to choose apps"),LinearLayout.LayoutParams(-1,0,1f))
        else {
            val scroll=ScrollView(this);val grid=GridLayout(this).apply {columnCount=4}
            selected.forEachIndexed {index,app->
                val tile=LinearLayout(this).apply {orientation=1;gravity=Gravity.CENTER;setPadding(d(4),d(16),d(4),d(8));contentDescription=app.title}
                tile.addView(ImageView(this).apply {setImageDrawable(app.newIcon(this@DotHidden))},LinearLayout.LayoutParams(d(56),d(56)))
                tile.addView(text(app.title.toString()).apply {textSize=11f;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END},LinearLayout.LayoutParams(-1,d(28)))
                tile.setOnClickListener {LawnchairLauncher.instance?.startActivitySafely(it,app.intent,app)}
                grid.addView(tile,GridLayout.LayoutParams().apply {width=0;columnSpec=GridLayout.spec(index%4,1f)})
            }
            scroll.addView(grid);body.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        }
    }
    private fun manage() {
        val chosen=prefs.hiddenApps.firstBlocking().toMutableSet()
        val dialog=Dialog(this);val body=LinearLayout(this).apply {orientation=1;setBackgroundColor(paper);setPadding(d(20),d(16),d(20),d(24))}
        val title=text("Hidden icons").apply {textSize=17f};body.addView(title,LinearLayout.LayoutParams(-1,d(48)))
        val search=EditText(this).apply {setSingleLine(true);hint="Search apps";setTextColor(ink);setHintTextColor(DotAppearance.get(this@DotHidden).secondary);typeface=resources.getFont(com.android.launcher3.R.font.geist_regular)}
        body.addView(search,LinearLayout.LayoutParams(-1,d(52)))
        val scroll=ScrollView(this);val list=LinearLayout(this).apply {orientation=1}
        val rows=mutableListOf<View>()
        apps.forEach {app->
            val key=app.toComponentKey().toString()
            val row=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
            row.addView(ImageView(this).apply {setImageDrawable(app.newIcon(this@DotHidden))},LinearLayout.LayoutParams(d(40),d(40)))
            row.addView(text(app.title.toString()).apply {gravity=Gravity.CENTER_VERTICAL;setPadding(d(12),0,d(12),0)},LinearLayout.LayoutParams(0,d(56),1f))
            row.addView(CheckBox(this).apply {isChecked=key in chosen;contentDescription=app.title;setOnCheckedChangeListener {_,checked->if(checked)chosen.add(key) else chosen.remove(key)}})
            row.tag=app.title.toString().lowercase();rows.add(row);list.addView(row)
        }
        search.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int) {}
            override fun onTextChanged(s: CharSequence?,start: Int,before: Int,count: Int) {val query=s.toString().lowercase();rows.forEach {it.visibility=if(it.tag.toString().contains(query))View.VISIBLE else View.GONE}}
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        scroll.addView(list);body.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        body.addView(text("Done").apply {setOnClickListener {prefs.hiddenApps.setBlocking(chosen);dialog.dismiss();show()}},LinearLayout.LayoutParams(-1,d(52)))
        dialog.setContentView(body);dialog.window?.setLayout(-1,(resources.displayMetrics.heightPixels*.82f).toInt());dialog.window?.setGravity(Gravity.BOTTOM);dialog.show();dialog.window?.setLayout(-1,(resources.displayMetrics.heightPixels*.82f).toInt())
    }
}
