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
