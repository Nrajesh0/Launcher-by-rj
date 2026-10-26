#!/usr/bin/env python3
"""Integrate Dot Home identity, settings and local resources into the pinned core."""
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1]).resolve()
project = Path(__file__).resolve().parents[2]
version = dict(line.split("=",1) for line in (project/"version.properties").read_text().splitlines() if "=" in line)

def edit(path, before, after):
    file = root / path
    text = file.read_text()
    if before not in text: raise RuntimeError(f"Pinned source mismatch: {path}: {before[:55]}")
    file.write_text(text.replace(before, after))

def function(path, signature, replacement):
    file = root / path
    text = file.read_text()
    start = text.index(signature)
    at = text.index("{", start) + 1
    depth = 1
    while depth:
        if text[at] == "{": depth += 1
        elif text[at] == "}": depth -= 1
        at += 1
    file.write_text(text[:start] + replacement + text[at:])

edit("build.gradle", 'final def buildCommit = providers.exec {', 'final def buildCommit = System.getenv("DOT_HOME_SOURCE_COMMIT")?.take(7) ?: providers.exec {')
edit("build.gradle", "applicationId 'app.lawnchair'", "applicationId 'dev.dothome'")
edit("build.gradle", 'applicationIdSuffix ".debug"', '// Preserve the installed package for test and release variants.')
edit("build.gradle", "versionCode 15_00_02_03_00", "versionCode " + version["versionCode"])
edit("build.gradle", 'versionName "${versionDisplayName}"', 'versionName "'+version['versionName']+'"')
edit("build.gradle", "targetSdk 35", "targetSdk 36")
edit("build.gradle", "minSdk 26", "minSdk 35")
edit("build.gradle", 'resValue("string", "derived_app_name", "Lawnchair")', 'resValue("string", "derived_app_name", "Dot Home")')
edit("build.gradle", 'resValue("string", "derived_app_name", "Lawnchair (Debug)")', 'resValue("string", "derived_app_name", "Dot Home")')
edit("build.gradle", "${applicationId}/app.lawnchair.LawnchairLauncher", "${applicationId}/dev.dothome.HomeActivity")
edit("build.gradle", "        release {\n", "        release {\n            signingConfig null\n")
edit("lawnchair/src/app/lawnchair/LawnchairLauncher.kt", "class LawnchairLauncher : QuickstepLauncher()", "open class LawnchairLauncher : QuickstepLauncher()")
edit("lawnchair/src/app/lawnchair/LawnchairApp.kt", "        super.onCreate()\n", "        super.onCreate()\n        dev.dothome.DotDefaults.apply(this)\n")

# Same component and optional listener identity as installed builds.
for rel in ("quickstep/AndroidManifest-launcher.xml", "AndroidManifest-common.xml"):
    file = root / rel
    text = file.read_text().replace('android:name="app.lawnchair.LawnchairLauncher"', 'android:name="dev.dothome.HomeActivity"')
    text = text.replace('android:name="com.android.launcher3.notification.NotificationListener"', 'android:name="dev.dothome.MediaAccess"')
    text = text.replace('android:targetSdkVersion="33"', 'android:targetSdkVersion="36"')
    file.write_text(text)
file = root / "src/com/android/launcher3/notification/NotificationListener.java"
text = file.read_text().replace("NotificationListener.class", "dev.dothome.MediaAccess.class")
file.write_text(text)

# Geometric popup hierarchy: no wallpaper carousel; explicit editing entry.
edit("lawnchair/src/app/lawnchair/ui/popup/LauncherOptionsPopup.kt", 'LauncherOptionPopupItem("carousel", true)', 'LauncherOptionPopupItem("carousel", false)')
edit("lawnchair/src/app/lawnchair/ui/popup/LauncherOptionsPopup.kt", 'LauncherOptionPopupItem("edit_mode", false)', 'LauncherOptionPopupItem("edit_mode", true)')
function("src/com/android/launcher3/views/OptionsPopupView.java", "    private static boolean startWallpaperPicker(View v)", """    private static boolean startWallpaperPicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        launcher.startActivity(new Intent(launcher, app.lawnchair.ui.preferences.PreferenceActivity.class)
                .putExtra("dotRoute", "customisation"));
        return true;
    }""")

prefs = root / "lawnchair/src/app/lawnchair/ui/preferences/PreferenceActivity.kt"
prefs.write_text('''package app.lawnchair.ui.preferences
import android.content.Context
import android.content.Intent
import android.os.Bundle
import app.lawnchair.ui.preferences.navigation.PreferenceRoute
class PreferenceActivity : dev.dothome.ui.DotPreferences() {
    override fun onCreate(state: Bundle?) {
        if(intent.action == Intent.ACTION_APPLICATION_PREFERENCES && !intent.hasExtra("dotRoute")) intent.putExtra("dotRoute", "home")
        super.onCreate(state)
    }
    companion object {
        fun createIntent(context: Context, destination: PreferenceRoute): Intent = Intent(context, PreferenceActivity::class.java)
    }
}
''')

edit("lawnchair/src/app/lawnchair/font/FontCache.kt", "R.font.inter_regular", "R.font.geist_regular")
edit("lawnchair/src/app/lawnchair/font/FontCache.kt", "R.font.inter_medium", "R.font.geist_medium")
file = root / "lawnchair/src/app/lawnchair/font/FontCache.kt"
text = file.read_text()
start = text.index("    class GoogleFont(")
tail = text[start:]
first = tail.index("        override val composeFontFamily = FontFamily(")
last = tail.index("\n        private fun createVariantName()", first)
tail = tail[:first] + "        override val composeFontFamily = FontFamily.Default\n" + tail[last:]
first = tail.index("        override suspend fun load(): Typeface?")
last = tail.index("        override fun saveToJson", first)
tail = tail[:first] + "        override suspend fun load(): Typeface? = Typeface.DEFAULT\n\n" + tail[last:]
file.write_text(text[:start] + tail)

# Font and attribution files are local, including in the installed APK.
fonts = root / "lawnchair/res/font"
fonts.mkdir(parents=True, exist_ok=True)
for name in ("geist_regular.ttf", "geist_medium.ttf"):
    shutil.copy2(project / "app/src/main/res/font" / name, fonts / name)
licenses = root / "lawnchair/assets/licenses"
licenses.mkdir(parents=True, exist_ok=True)
shutil.copy2(project / "LICENSES/Geist-OFL.txt", licenses / "Geist-OFL.txt")
for name in ("LibreCaslonDisplay-OFL.txt", "SpaceMono-OFL.txt"):
    shutil.copy2(project / "LICENSES" / name, licenses / name)
shutil.copy2(root / "LICENSE.txt", licenses / "Launcher3-Lawnchair-Apache-2.0.txt")
(licenses / "DotHome-NOTICE.txt").write_text("Dot Home adapts Launcher3/Lawnchair; upstream source and patches: https://github.com/Nrajesh0/Launcher-by-rj/tree/rebuild-v4\nPinned Lawnchair source: https://github.com/LawnchairLauncher/lawnchair/tree/505dbc40e6154c05158b5d0271c45f6a885a411b\nModified for offline operation, native Dot Home identity, local icons/widgets, appearance, arrangement and legacy migration. Upstream source notices are retained.\n")
for file in (project / "foundation/overlay").rglob("*"):
    if file.is_file():
        dest = root / file.relative_to(project / "foundation/overlay")
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, dest)
print("Dot Home foundation identity, compact settings, popup routes and local typography integrated.")

# Local custom widgets are managed in-process by the native widget host.
edit("src/com/android/launcher3/widget/custom/CustomWidgetManager.java", "if (enableSmartspaceAsAWidget())", "if (true)")
edit("src/com/android/launcher3/widget/custom/CustomWidgetManager.java", 'private static final String PLUGIN_PKG = "android";', 'private static final String PLUGIN_PKG = "dev.dothome";')
file = root/"lawnchair/AndroidManifest.xml"
tree=ET.parse(file); doc=tree.getroot(); app=doc.find("application"); a="{http://schemas.android.com/apk/res/android}"
seed=ET.SubElement(app,"receiver",{a+"name":"dev.dothome.widgets.LocalWidgetSeed",a+"exported":"false",a+"label":"Dot Home"})
intent=ET.SubElement(seed,"intent-filter")
ET.SubElement(intent,"action",{a+"name":"android.appwidget.action.APPWIDGET_UPDATE"})
ET.SubElement(seed,"meta-data",{a+"name":"android.appwidget.provider",a+"resource":"@xml/dot_seed_widget"})
queries=ET.SubElement(doc,"queries")
for action,category in (("android.intent.action.MAIN","android.intent.category.LAUNCHER"),("android.intent.action.MAIN","android.intent.category.HOME"),("android.appwidget.action.APPWIDGET_UPDATE",None),("org.adw.launcher.THEMES",None),("com.novalauncher.THEME",None),("com.anddoes.launcher.THEME",None)):
    query=ET.SubElement(queries,"intent")
    ET.SubElement(query,"action",{a+"name":action})
    if category: ET.SubElement(query,"category",{a+"name":category})
ET.SubElement(app,"activity",{a+"name":"dev.dothome.ui.DotHidden",a+"exported":"false",a+"theme":"@android:style/Theme.Material.Light.NoActionBar"})
ET.SubElement(app,"activity",{a+"name":"dev.dothome.widgets.TileConfiguration",a+"exported":"false",a+"theme":"@style/DotWidgetSheet"})
ET.SubElement(app,"activity",{a+"name":"dev.dothome.widgets.TileDetails",a+"exported":"false",a+"theme":"@android:style/Theme.Material.Light.NoActionBar"})
wallpaper=ET.SubElement(app,"service",{a+"name":"dev.dothome.ui.DotAtmosphere",a+"exported":"true",a+"permission":"android.permission.BIND_WALLPAPER",a+"label":"Atmosphere"})
intent=ET.SubElement(wallpaper,"intent-filter");ET.SubElement(intent,"action",{a+"name":"android.service.wallpaper.WallpaperService"})
ET.SubElement(wallpaper,"meta-data",{a+"name":"android.service.wallpaper",a+"resource":"@xml/dot_atmosphere"})
ET.SubElement(app,"activity",{a+"name":"dev.dothome.migration.UnavailableApp",a+"exported":"false",a+"theme":"@android:style/Theme.Material.Light.NoActionBar"})
for permission in ("android.permission.CAMERA","android.permission.READ_CALENDAR","com.android.alarm.permission.SET_ALARM","android.permission.ACTIVITY_RECOGNITION","android.permission.MODIFY_AUDIO_SETTINGS","android.permission.ACCESS_NOTIFICATION_POLICY"):
    ET.SubElement(doc,"uses-permission",{a+"name":permission})
ET.register_namespace("android",a[1:-1]);ET.register_namespace("tools","http://schemas.android.com/tools")
tree.write(file,encoding="unicode",xml_declaration=True)

# Provider class names are durable workspace identities and loaded from local XML.
with (root / 'proguard.pro').open('a') as rules:
    rules.write('\n-keep class dev.dothome.** { *; }\n-keep class com.android.launcher3.** { *; }\n-keep class app.lawnchair.LawnchairLauncher { *; }\n-keep class app.lawnchair.LawnchairLauncher$* { *; }\n-keep class dev.dothome.widgets.DotTiles$* { public <init>(android.content.Context); *; }\n-keep class dev.dothome.widgets.DotTile { *; }\n')

# Providers initialise launcher preferences before Application.onCreate.
edit("lawnchair/src/app/lawnchair/LawnchairApp.kt", "    override fun onCreate() {", "    override fun attachBaseContext(base: Context) {\n        super.attachBaseContext(base)\n        if (base.getSystemService(android.os.UserManager::class.java)?.isUserUnlocked == true) dev.dothome.DotDefaults.apply(base)\n    }\n\n    override fun onCreate() {")
edit("proguard.pro", "-keepattributes InnerClasses,", "-keepattributes EnclosingMethod, InnerClasses,")
