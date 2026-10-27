"""Use shared appearance tokens in the engine rather than independent hardcoded palettes."""
from pathlib import Path
import sys
root=Path(sys.argv[1]).resolve()
def edit(path,before,after):
 f=root/path;t=f.read_text()
 if before not in t:raise RuntimeError('Pinned design patch mismatch: '+path+': '+before[:60])
 f.write_text(t.replace(before,after))
f=root/'lawnchair/src/app/lawnchair/LawnchairProcessInitializer.kt'
t=f.read_text();start=t.index('        ThemedIconDrawable.COLORS_LOADER = Function {');end=t.index('        super.init(context)',start)
f.write_text(t[:start]+'''        ThemedIconDrawable.COLORS_LOADER = Function {
            val appearance = dev.dothome.ui.DotAppearance.get(it)
            intArrayOf(appearance.iconBackground, appearance.iconForeground)
        }
'''+t[end:])
edit('lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt','            background = qsbBackground','            background = if(dev.dothome.ui.DotAppearance.get(context).glass) dev.dothome.ui.DotGlassDrawable(this, 100f) else qsbBackground')
print('Engine icon colours and dock search use the shared appearance model.')
edit('src/com/android/launcher3/views/OptionsPopupView.java','    private static boolean onWidgetsClicked(View view) {','    private static boolean onWidgetsClicked(View view) {\n        if (view != null) {\n            Launcher launcher = Launcher.getLauncher(view.getContext());\n            com.android.launcher3.AbstractFloatingView.closeAllOpenViews(launcher);\n            launcher.getDragLayer().post(() -> dev.dothome.widgets.DotWidgetTray.show(launcher));\n            return true;\n        }')
edit('lawnchair/src/app/lawnchair/util/AppCategorizationUtils.kt','    return finalCategorizedApps','    return dev.dothome.ui.DotCategories.apply(context, apps, finalCategorizedApps)')
edit('lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt','if (apps.size == 1) {','if (false) {')
edit('src/com/android/launcher3/allapps/AllAppsGridAdapter.java','        mGridLayoutMgr.setSpanCount(totalSpans);','        if (totalSpans % 2 != 0) totalSpans *= 2;\n        mGridLayoutMgr.setSpanCount(totalSpans);')
edit('src/com/android/launcher3/allapps/AllAppsGridAdapter.java','            if (isIconViewType(viewType)) {','            if (viewType == VIEW_TYPE_FOLDER && !app.lawnchair.preferences.PreferenceManager.getInstance(mActivityContext).getDrawerList().get()) return totalSpans / 2;\n            if (isIconViewType(viewType)) {')
edit('src/com/android/launcher3/allapps/BaseAllAppsAdapter.java','''                container.addView(FolderIcon.inflateFolderAndIcon(R.layout.all_apps_folder_icon, mActivityContext,
                    container, folderInfo));''','''                if (!app.lawnchair.preferences.PreferenceManager.getInstance(mActivityContext).getDrawerList().get()) {
                    container.addView(new dev.dothome.ui.DotCategoryCard(mActivityContext, folderInfo));
                } else container.addView(FolderIcon.inflateFolderAndIcon(R.layout.all_apps_folder_icon, mActivityContext,
                    container, folderInfo));''')

# The native popup keeps its placement/accessibility, with the same owned colour roles.
for token in ("PopupColorPrimary","PopupShadeFirst","PopupShadeSecond","PopupShadeThird"):
 edit('src/com/android/launcher3/popup/ArrowPopup.java',f'ColorTokens.{token}.resolveColor(context)','dev.dothome.ui.DotAppearance.get(context).card')
edit('src/com/android/launcher3/popup/ArrowPopup.java','        colorAnimator.setDuration(0).start();','        dev.dothome.ui.DotUi.popup(viewGroup);\n        colorAnimator.setDuration(0).start();')

# Preserve native notification-dot state/animation over the custom expanded/cover renderer.
edit('src/com/android/launcher3/folder/FolderIcon.java','            dev.dothome.ui.DotFolderPreview.draw(canvas, this, mInfo);\n            return;','            dev.dothome.ui.DotFolderPreview.draw(canvas, this, mInfo);\n            drawDot(canvas);\n            return;')
edit('src/com/android/launcher3/folder/FolderIcon.java','            Utilities.scaleRectAboutCenter(iconBounds, iconScale);','            Utilities.scaleRectAboutCenter(iconBounds, iconScale);\n            if (dev.dothome.ui.DotFolderPreview.expanded(mInfo) || dev.dothome.ui.DotFolderPreview.covered(this,mInfo)) dev.dothome.ui.DotFolderPreview.bounds(this,iconBounds);')
# Disable autonomous state transitions under the launcher-local reduced motion setting;
# finger-controlled workspace/drawer transitions still follow the user's gesture.
edit('src/com/android/launcher3/statemanager/StateManager.java','        animated &= areAnimatorsEnabled();','        animated &= areAnimatorsEnabled() && !dev.dothome.ui.DotAppearance.get(mStatefulContainer).reducedMotion;')

# Search scope switches must control the local result providers, including shortcuts.
edit('lawnchair/src/app/lawnchair/search/algorithms/LawnchairLocalSearchAlgorithm.kt',
'''            val appResults = appSearchProvider.search(context, query, apps)
            val shortcutResults = shortcutSearchProvider.search(context, appResults)''',
'''            val scope = PreferenceManager.getInstance(context)
            val candidates = appSearchProvider.search(context, query, apps)
            val appResults = if (scope.searchResultApps.get()) candidates else emptyList()
            val shortcutResults = if (scope.searchResultShortcuts.get()) shortcutSearchProvider.search(context, candidates) else emptyList()''')

# Keep the paused Home preview coherent while its Activity recreation is deferred.
edit('lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt','    private fun setUpBackground(themed: Boolean = false) {','    fun refreshDotAppearance() { setUpBackground() }\n\n    private fun setUpBackground(themed: Boolean = false) {')

edit('lawnchair/src/app/lawnchair/search/algorithms/LawnchairLocalSearchAlgorithm.kt',
'        SettingsSearchProvider,','        SettingsSearchProvider,\n        dev.dothome.ui.DotDocumentSearchProvider,')
edit('lawnchair/src/app/lawnchair/search/adapter/SearchTargetFactory.kt',
'            is FileInfo -> Uri.withAppendedPath(',
'            is FileInfo -> if(info.fileId.startsWith("content://")) Uri.parse(info.fileId) else Uri.withAppendedPath(')

# The upstream V2 flow persists a provisional widget before configuration. A cancelled
# configuration must remove that native item as well as its system widget binding.
edit('src/com/android/launcher3/Launcher.java',
'        } else if (resultCode == RESULT_CANCELED) {\n            mAppWidgetHolder.deleteAppWidgetId(appWidgetId);\n            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;',
'        } else if (resultCode == RESULT_CANCELED) {\n            LauncherAppWidgetHostView provisional = mWorkspace.getWidgetForAppWidgetId(appWidgetId);\n            if (enableAddAppWidgetViaConfigActivityV2() && provisional != null\n                    && provisional.getTag() instanceof LauncherAppWidgetInfo item) {\n                removeItem(provisional, item, true, "Widget configuration cancelled");\n            } else {\n                mAppWidgetHolder.deleteAppWidgetId(appWidgetId);\n            }\n            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;')

# Recents/navigation onboarding belongs to the system launcher, not this Home APK.
edit('quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java',
'        QuickstepOnboardingPrefs.setup(this);',
'        if (app.lawnchair.LawnchairApp.isRecentsEnabled()) QuickstepOnboardingPrefs.setup(this);')
