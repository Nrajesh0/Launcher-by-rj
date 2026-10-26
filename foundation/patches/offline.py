#!/usr/bin/env python3
"""Remove upstream online/privileged facilities before compiling the pinned foundation.

This file is build-time tooling. No downloader or remote option is installed in Dot Home.
Each replacement checks its source so an upstream update cannot silently skip a removal.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1]).resolve()

def edit(path, before, after):
    file = root / path
    text = file.read_text()
    if before not in text:
        raise RuntimeError(f"Pinned source does not match patch: {path}: {before[:60]}")
    file.write_text(text.replace(before, after))

def write(path, body):
    file = root / path
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(body)

def remove(path):
    file = root / path
    if not file.is_file():
        raise RuntimeError(f"Pinned source missing: {path}")
    file.unlink()

def function(path, signature, replacement):
    file = root / path
    text = file.read_text()
    start = text.index(signature)
    opening = text.index("{", start)
    depth = 1
    at = opening + 1
    while depth:
        if text[at] == "{": depth += 1
        elif text[at] == "}": depth -= 1
        at += 1
    file.write_text(text[:start] + replacement + text[at:])

lc = "lawnchair/src/app/lawnchair/"
edit("build.gradle", "    implementation libs.bundles.retrofit\n", "")
edit("build.gradle", "    implementation libs.coil.compose\n", "")

# Updater, remote defaults, online contributors and crash-report uploads have no
# implementation or user-facing availability in this distribution.
remove(lc + "ui/preferences/data/liveinfo/LiveInformationService.kt")
write(lc + "ui/preferences/data/liveinfo/LiveInformationRequest.kt", """package app.lawnchair.ui.preferences.data.liveinfo
import app.lawnchair.ui.preferences.data.liveinfo.model.LiveInformation
suspend fun getLiveInformation(): LiveInformation? = null
""")
remove(lc + "ui/preferences/about/NightlyBuildsRepository.kt")
path = root / (lc + "ui/preferences/about/GithubService.kt")
text = path.read_text()
models = text[text.index("@Serializable"):].split("internal val gitHubApiRetrofit:")[0]
path.write_text("package app.lawnchair.ui.preferences.about\nimport kotlinx.serialization.SerialName\nimport kotlinx.serialization.Serializable\n" + models)
write(lc + "ui/preferences/about/AboutViewModel.kt", """package app.lawnchair.ui.preferences.about
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.android.launcher3.BuildConfig
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
class AboutViewModel(application: Application) : AndroidViewModel(application) {
    val uiState = MutableStateFlow(AboutUiState(versionName = BuildConfig.VERSION_NAME, commitHash = BuildConfig.COMMIT_HASH)).asStateFlow()
    val updateState = MutableStateFlow<UpdateState>(UpdateState.Hidden).asStateFlow()
    fun downloadUpdate() = Unit
    fun installUpdate(file: File) = Unit
}
""")
path = root / (lc + "ui/preferences/about/ContributorRow.kt")
text = path.read_text().replace("import coil.compose.SubcomposeAsyncImage\n", "")
start = text.index("            SubcomposeAsyncImage(")
end = text.index("\n        },", start)
text = text[:start] + '            Text(text = name.take(1), modifier = Modifier.size(32.dp))' + text[end:]
path.write_text(text)
remove(lc + "bugreport/KatbinService.kt")
write(lc + "bugreport/UploaderUtils.kt", """package app.lawnchair.bugreport
object UploaderUtils {
    const val IS_ALIVE_AVAILABLE = false
    suspend fun upload(report: BugReport): String = error("Uploads are not included")
}
""")

# Existing serialized search preferences remain readable, but there is only a
# local provider. No search URL, HTTP client or web-suggestion implementation.
remove(lc + "search/algorithms/engine/provider/web/BuiltInWebSearchProviders.kt")
write(lc + "search/algorithms/engine/provider/web/WebSearchProvider.kt", """package app.lawnchair.search.algorithms.engine.provider.web
import android.content.Context
import com.android.launcher3.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
interface WebSearchProvider {
    val label: Int get() = R.string.search_provider_custom
    val iconRes: Int get() = R.drawable.ic_search
    val id: String get() = "offline"
    fun configure(context: Context): WebSearchProvider = this
    fun getSuggestions(query: String): Flow<List<String>> = flowOf(emptyList())
    fun getSearchUrl(query: String): String = ""
    companion object {
        fun values(): List<WebSearchProvider> = listOf(OfflineSearchProvider)
        fun fromString(value: String): WebSearchProvider = OfflineSearchProvider
    }
}
object OfflineSearchProvider : WebSearchProvider
""")
write(lc + "search/algorithms/engine/provider/web/CustomWebSearchProvider.kt", """package app.lawnchair.search.algorithms.engine.provider.web
object CustomWebSearchProvider : WebSearchProvider {
    fun getDisplayName(): String = "Local apps"
}
""")
path = lc + "search/algorithms/LawnchairLocalSearchAlgorithm.kt"
edit(path, "        WebSuggestionProvider,\n", "")
function(path, "    private fun generateActionResults(query: String)", "    private fun generateActionResults(query: String): List<SearchResult.Action> = emptyList()")

# The core remains a standard third-party Home application. It neither binds an
# Accessibility service nor offers root/secure-setting navigation workarounds.
remove(lc + "LawnchairAccessibilityService.kt")
edit(lc + "LawnchairApp.kt", "    internal var accessibilityService: LawnchairAccessibilityService? = null\n", "")
edit(lc + "LawnchairApp.kt", "fun isAccessibilityServiceBound(): Boolean = accessibilityService != null", "fun isAccessibilityServiceBound(): Boolean = false")
function(lc + "LawnchairApp.kt", "    fun performGlobalAction(action: Int)", "    fun performGlobalAction(action: Int): Boolean = false")
edit(lc + "LawnchairLauncher.kt", "        showQuickstepWarningIfNecessary()\n", "")

# Strip dangerous/online/privileged manifest entries in every compiled variant.
android = "{http://schemas.android.com/apk/res/android}"
allowed = {
    "android.permission.VIBRATE", "android.permission.BIND_APPWIDGET",
    "android.permission.PACKAGE_USAGE_STATS", "android.permission.SET_WALLPAPER",
    "android.permission.SET_WALLPAPER_HINTS", "android.permission.ACCESS_HIDDEN_PROFILES",
    "android.permission.READ_CALENDAR", "android.permission.CAMERA",
    "com.android.alarm.permission.SET_ALARM",
}
blocked_components = ("LawnchairAccessibilityService", "UploaderService", "SleepDeviceAdmin", "TouchInteractionService", "AppWidgetsRestoredReceiver")
for file in root.rglob("*Manifest*.xml"):
    if any(part in ("build", ".git", ".gradle") for part in file.relative_to(root).parts): continue
    tree = ET.parse(file)
    doc = tree.getroot()
    for node in list(doc):
        if node.tag.startswith("uses-permission"):
            name = node.get(android + "name", "")
            if name not in allowed and not name.startswith("${applicationId}.permission."):
                doc.remove(node)
    for app in doc.findall("application"):
        app.set(android + "allowBackup", "false")
        for node in list(app):
            if any(part in node.get(android + "name", "") for part in blocked_components):
                app.remove(node)
    ET.register_namespace("android", "http://schemas.android.com/apk/res/android")
    ET.register_namespace("tools", "http://schemas.android.com/tools")
    tree.write(file, encoding="unicode", xml_declaration=True)

# A build fails if upstream introduces another directly imported network client.
for base in ("lawnchair/src", "src", "quickstep/src"):
    for file in (root / base).rglob("*"):
        if file.suffix not in (".kt", ".java"): continue
        if re.search(r"^import\s+(?:retrofit2|okhttp3|coil)\.", file.read_text(), re.M):
            raise RuntimeError(f"Network-capable import remains: {file.relative_to(root)}")
print("Pinned foundation: HTTP clients, update/upload implementations and privileged manifest entries removed.")

# Remove the crash-notification upload UI as well as its already removed HTTP client.
edit(lc+'LawnchairProcessInitializer.kt', 'import app.lawnchair.bugreport.LawnchairBugReporter\n', '')
edit(lc+'LawnchairProcessInitializer.kt', '        LawnchairBugReporter.INSTANCE.get(context)\n', '')
for file in (root/lc/'bugreport').glob('*.kt'):
    file.unlink()
for file in root.rglob('*Manifest*.xml'):
    if any(part in ('build','.git','.gradle') for part in file.relative_to(root).parts): continue
    tree=ET.parse(file);app=tree.getroot().find('application')
    if app is None: continue
    for child in list(app):
        if child.get(android+'name','').startswith('app.lawnchair.bugreport.'):
            app.remove(child)
    tree.write(file,encoding='unicode',xml_declaration=True)

# Do not bind an online feed/search overlay hosted by the Google app or a bridge.
edit(lc+'LawnchairLauncher.kt', '    private val defaultOverlay by unsafeLazy { OverlayCallbackImpl(this) }', '    private val defaultOverlay = object : LauncherOverlayManager {}')
edit(lc+'LawnchairLauncher.kt', '''        prefs.feedProvider.subscribeChanges(this, defaultOverlay::reconnect)
        preferenceManager2.enableFeed.get().distinctUntilChanged().onEach { enable ->
            defaultOverlay.setEnableFeed(enable)
        }.launchIn(scope = lifecycleScope)
''', '')
