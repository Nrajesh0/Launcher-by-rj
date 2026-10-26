package dev.dothome.ui

import android.content.Context
import androidx.datastore.preferences.core.*
import app.lawnchair.preferences2.PreferenceManager2
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Typed appearance/organisation settings only; no permissions, roles or online providers. */
object DotSettingsBackup {
    private val legacy=setOf("pref_workspaceColumns","pref_workspaceRows","pref_hotseatColumns","pref_iconPackPackage","pref_launcherTheme","prefs_wrapAdaptive","pref_shadowBGIcons","themed_icons","drawer_themed_icons","pref_searchHotseatTranparency","pref_hotseatQsbCornerRadius","pref_searchResultApps","pref_searchResultShortcuts","pref_searchResultCalculator","pref_searchResultSettingsEntry","pref_drawerList","pref_drawerListOrder","pref_hideFolderApps")
    private val modern=setOf("hidden_apps","hidden_apps_in_search","show_hidden_apps_in_search","hide_app_drawer_search_bar","show_icon_labels_on_home_screen","show_icon_labels_on_home_screen_folder","show_icon_labels_in_drawer","lock_home_screen","hotseat_mode","accent_color","icon_shape","themed_hotseat_qsb")
    private fun allowed(key: String)=key in modern||key=="drawer_columns"||key=="drawer_columns_landscape"
    private val legacyStrings=setOf("pref_iconPackPackage","pref_launcherTheme","pref_drawerListOrder")
    private val legacyInts=setOf("pref_workspaceColumns","pref_workspaceRows","pref_hotseatColumns","pref_searchHotseatTranparency")
    private fun expected(key: String)=when(key){"hidden_apps"->"set";"hidden_apps_in_search","accent_color","icon_shape","hotseat_mode"->"string";"drawer_columns","drawer_columns_landscape"->"int";else->"boolean"}
    @JvmStatic fun validate(source: JSONObject) {
        val old=source.optJSONObject("legacy")?:JSONObject()
        for(key in old.keys())if(key in legacy){val value=old.get(key);require(when(key){in legacyStrings->value is String;in legacyInts->value is Number;"pref_hotseatQsbCornerRadius"->value is Number;else->value is Boolean})}
        val store=source.optJSONObject("modern")?:JSONObject()
        for(key in store.keys())if(allowed(key)){
            val item=store.getJSONObject(key);require(item.getString("type")==expected(key))
            val value=item.get("value")
            require(when(expected(key)){"boolean"->value is Boolean;"int"->value is Number;"set"->value is JSONArray&&value.length()<=2000&&(0 until value.length()).all {value.get(it) is String};else->value is String})
            if(expected(key)=="int")require(item.getInt("value") in -1..20)
            if(key=="hidden_apps_in_search")require(item.getString("value") in setOf("off","if_name_typed","on"))
        }
    }
    @JvmStatic fun export(context: Context): JSONObject = runBlocking {
        val old=JSONObject();context.getSharedPreferences("com.android.launcher3.prefs",0).all.forEach {(key,value)->if(key in legacy)old.put(key,value)}
        val store=JSONObject();PreferenceManager2.getInstance(context).preferencesDataStore.data.first().asMap().forEach {(key,value)->
            if(allowed(key.name))store.put(key.name,JSONObject().put("type",when(value){is Boolean->"boolean";is Int->"int";is Long->"long";is Float->"float";is Double->"double";is Set<*>->"set";else->"string"}).put("value",if(value is Set<*>)JSONArray(value.toList())else value))
        }
        JSONObject().put("legacy",old).put("modern",store)
    }
    @JvmStatic fun restore(context: Context,source: JSONObject) = runBlocking {
        validate(source)
        val old=source.optJSONObject("legacy")?:JSONObject();val editor=context.getSharedPreferences("com.android.launcher3.prefs",0).edit()
        legacy.forEach {editor.remove(it)}
        for(key in old.keys())if(key in legacy)if(key=="pref_hotseatQsbCornerRadius")editor.putFloat(key,old.getDouble(key).toFloat())else when(val value=old.get(key)){is Boolean->editor.putBoolean(key,value);is Int->editor.putInt(key,value);is Long->editor.putLong(key,value);is Number->editor.putFloat(key,value.toFloat());is String->editor.putString(key,value)}
        check(editor.commit()){ "Appearance settings could not be saved" }
        val store=source.optJSONObject("modern")?:JSONObject()
        PreferenceManager2.getInstance(context).preferencesDataStore.edit { prefs->
            prefs.asMap().keys.filter {allowed(it.name)}.forEach {prefs.remove(it)}
            for(key in store.keys())if(allowed(key)){
                val item=store.getJSONObject(key)
                when(item.getString("type")){
                    "boolean"->prefs[booleanPreferencesKey(key)]=item.getBoolean("value")
                    "int"->prefs[intPreferencesKey(key)]=item.getInt("value")
                    "long"->prefs[longPreferencesKey(key)]=item.getLong("value")
                    "float"->prefs[floatPreferencesKey(key)]=item.getDouble("value").toFloat()
                    "double"->prefs[doublePreferencesKey(key)]=item.getDouble("value")
                    "set"->{val values=item.getJSONArray("value");prefs[stringSetPreferencesKey(key)]=(0 until values.length()).map {values.getString(it)}.toSet()}
                    "string"->prefs[stringPreferencesKey(key)]=item.getString("value")
                }
            }
        }
    }
}
