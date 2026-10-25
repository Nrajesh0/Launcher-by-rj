#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1]).resolve()

def edit(path, before, after):
    file = root/path
    text = file.read_text()
    if before not in text: raise RuntimeError(f"Pinned source mismatch: {path}")
    file.write_text(text.replace(before,after))

def function(path, signature, replacement):
    file=root/path; text=file.read_text(); start=text.index(signature); at=text.index('{',start)+1; depth=1
    while depth:
        if text[at]=='{':depth+=1
        elif text[at]=='}':depth-=1
        at+=1
    file.write_text(text[:start]+replacement+text[at:])

function('lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt','        fun getSearchProvider(','''        fun getSearchProvider(context: Context, preferenceManager: PreferenceManager2): QsbSearchProvider = AppSearch''')
edit('lawnchair/res/values/config.xml','>google</string>','>app_search</string>')
edit('lawnchair/res/values/config.xml','name="config_default_show_icon_labels_on_home_screen">true','name="config_default_show_icon_labels_on_home_screen">false')

edit('lawnchair/res/values/config.xml','name="config_default_enable_smartspace">true','name="config_default_enable_smartspace">false')

# Enable the bundled component map even when no separate icon-pack APK is installed.
edit('lawnchair/src/app/lawnchair/icons/LawnchairIconProvider.kt','if (isSupported && isOlderLawniconsInstalled) null else DISABLED_MAP','if (isSupported) null else DISABLED_MAP')
file=root/'lawnchair/src/app/lawnchair/LawnchairProcessInitializer.kt'
text=file.read_text()
start=text.index('        ThemedIconDrawable.COLORS_LOADER = Function {')
end=text.index('        super.init(context)',start)
text=text[:start]+'''        ThemedIconDrawable.COLORS_LOADER = Function {
            val prefs = it.getSharedPreferences("dot-style", 0)
            val dark = prefs.getBoolean("dark", Utilities.isDarkTheme(it))
            when(prefs.getString("material", "classic")) {
                "classic" -> if(dark) intArrayOf(0xff202120.toInt(),0xfff1f1ed.toInt()) else intArrayOf(0xfff8f8f4.toInt(),0xff181918.toInt())
                else -> if(dark) intArrayOf(ColorTokens.Accent2_800.resolveColor(it),ColorTokens.Accent1_100.resolveColor(it)) else intArrayOf(ColorTokens.Accent1_100.resolveColor(it),ColorTokens.Accent1_700.resolveColor(it))
            }
        }
''' + text[end:]
file.write_text(text)

# A local Search label replaces the empty/Google-only dock search field.
file=root/'lawnchair/res/layout/search_container_hotseat.xml'
tree=ET.parse(file); doc=tree.getroot(); a='{http://schemas.android.com/apk/res/android}'
inner=next(el for el in doc.iter() if el.get(a+'id')=='@+id/inner')
ET.SubElement(inner,'TextView',{a+'id':'@+id/dot_search_text',a+'layout_width':'match_parent',a+'layout_height':'match_parent',a+'gravity':'center',a+'text':'Search',a+'textSize':'13sp',a+'fontFamily':'@font/geist_regular',a+'textColor':'?android:attr/textColorPrimary',a+'importantForAccessibility':'no'})
ET.register_namespace('android',a[1:-1]);ET.register_namespace('tools','http://schemas.android.com/tools')
tree.write(file,encoding='unicode',xml_declaration=True)
print('Local dock search, monochrome palette and bundled icon-map support applied.')

# The first-run popup follows the reference hierarchy, with a separate Arrange entry.
file=root/'lawnchair/src/app/lawnchair/ui/popup/LauncherOptionsPopup.kt'
text=file.read_text(); start=text.index('    val DEFAULT_ORDER = listOf('); end=text.index('\n    )',start)+6
text=text[:start]+'    val DEFAULT_ORDER = listOf(\n        LauncherOptionPopupItem("carousel", false),\n        LauncherOptionPopupItem("lock", false),\n        LauncherOptionPopupItem("home_settings", true),\n        LauncherOptionPopupItem("widgets", true),\n        LauncherOptionPopupItem("wallpaper", true),\n        LauncherOptionPopupItem("edit_mode", true),\n        LauncherOptionPopupItem("sys_settings", false),\n    )'+text[end:]; file.write_text(text)
file=root/'lawnchair/res/values/strings.xml'; tree=ET.parse(file); doc=tree.getroot()
for name,value in {'styles_wallpaper_button_text':'Customisation','edit_home_screen':'Arrange'}.items():
    element=next((el for el in doc if el.get('name')==name),None)
    if element is None: element=ET.SubElement(doc,'string',{'name':name})
    element.text=value
tree.write(file,encoding='unicode',xml_declaration=True)


edit("lawnchair/src/app/lawnchair/ui/popup/LauncherOptionsPopup.kt", 'if (Utilities.existsStyleWallpapers(launcher)) R.string.styles_wallpaper_button_text else R.string.wallpapers', 'R.string.styles_wallpaper_button_text')
edit("lawnchair/res/values/config.xml", '<bool name="config_default_edit_home_screen_on_popup">false</bool>', '<bool name="config_default_edit_home_screen_on_popup">true</bool>')

# Bundled monochrome resources do not require a separately installed pack.
file=root/'lawnchair/src/app/lawnchair/LawnchairLauncher.kt'
text=file.read_text();start=text.index('        // Handle update from version 12 Alpha 4 to version 12 Alpha 5.')
end=text.index('        colorScheme = themeProvider.colorScheme',start)
file.write_text(text[:start]+text[end:])

edit("lawnchair/src/app/lawnchair/LawnchairLauncher.kt", 'systemUiController.updateUiState(UI_STATE_BASE_WINDOW, isWorkspaceDarkText || darkStatusBar)', 'systemUiController.updateUiState(UI_STATE_BASE_WINDOW, dev.dothome.ui.DotWallpaperDrawable.darkSystemIcons(this, isWorkspaceDarkText || darkStatusBar))')

edit("lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt", 'val baseColor = if (themed) Themes.getColorBackgroundFloating(context) else Themes.getAttrColor(context, R.attr.qsbFillColor)', 'val baseColor = dev.dothome.ui.DotSurface.paper(context)')
edit("lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt", 'val alphaValue = (transparency * 255) / 100', 'val alphaValue = (transparency * Color.alpha(baseColor)) / 100')
edit("lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt", '            background = qsbBackground', '            background = qsbBackground\n            post {\n                findViewById<android.widget.TextView>(R.id.dot_search_text)?.setTextColor(dev.dothome.ui.DotSurface.ink(context))\n                gIcon.imageTintList = android.content.res.ColorStateList.valueOf(dev.dothome.ui.DotSurface.ink(context))\n            }')

edit("lawnchair/res/values/config.xml", '<string name="config_default_icon_shape" translatable="false">system</string>', '<string name="config_default_icon_shape" translatable="false">circle</string>')
