package dev.dothome.migration
import android.content.Context
import android.content.ComponentName
import android.os.UserManager
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.util.ComponentKey
import com.patrykmichalik.opto.core.setBlocking
import org.json.JSONObject
object LegacySettings {
    @JvmStatic fun apply(context: Context,settings: JSONObject) {
        val hidden=settings.optJSONObject("hidden") ?: return
        val users=context.getSystemService(UserManager::class.java)
        val keys=mutableSetOf<String>()
        hidden.keys().forEach {key->if(hidden.optBoolean(key))runCatching {
            val at=key.lastIndexOf('@')
            val user=if(at>=0)users?.getUserForSerialNumber(key.substring(at+1).toLong()) else android.os.Process.myUserHandle()
            val component=ComponentName.unflattenFromString(if(at>=0)key.substring(0,at) else key)
            if(user!=null&&component!=null)keys.add(ComponentKey(component,user).toString())
        }}
        PreferenceManager2.getInstance(context).hiddenApps.setBlocking(keys)
    }
}
