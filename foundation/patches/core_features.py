#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]).resolve()
def edit(path,before,after):
    file=root/path; text=file.read_text()
    if before not in text: raise RuntimeError('Pinned source mismatch: '+path+': '+before[:50])
    file.write_text(text.replace(before,after))

file='lawnchair/src/app/lawnchair/LawnchairLauncher.kt'
edit(file,'Stream.of(LawnchairShortcut.UNINSTALL, LawnchairShortcut.CUSTOMIZE),','Stream.of(LawnchairShortcut.UNINSTALL, dev.dothome.ui.DotItemActions.SIZE),')
file='src/com/android/launcher3/model/WorkspaceItemProcessor.kt'
edit(file,'Favorites.ITEM_TYPE_DEEP_SHORTCUT -> processAppOrDeepShortcut()','Favorites.ITEM_TYPE_DEEP_SHORTCUT,\n                1 -> processAppOrDeepShortcut()')
edit(file,'(c.itemType != Favorites.ITEM_TYPE_DEEP_SHORTCUT)', '(c.itemType == Favorites.ITEM_TYPE_APPLICATION)')
edit(file,'            info.spanX = 1\n            info.spanY = 1','            info.spanX = if(c.container == Favorites.CONTAINER_DESKTOP) c.spanX.coerceIn(1,2) else 1\n            info.spanY = if(c.container == Favorites.CONTAINER_DESKTOP) c.spanY.coerceIn(1,2) else 1')
edit(file,'        collection.spanX = 1\n        collection.spanY = 1','        collection.spanX = c.spanX.coerceIn(1,2)\n        collection.spanY = c.spanY.coerceIn(1,2)')
file='src/com/android/launcher3/BubbleTextView.java'
edit(file,'private final int mIconSize;', 'private int mIconSize;\n    private int mDotBaseIconSize;')
edit(file,'        a.recycle();\n\n        mRunningAppIndicatorSize', '        a.recycle();\n        mDotBaseIconSize = mIconSize;\n\n        mRunningAppIndicatorSize')
edit(file,'        super.onSizeChanged(w, h, oldw, oldh);\n        checkForEllipsis();','''        super.onSizeChanged(w, h, oldw, oldh);
        int nextSize = mDotBaseIconSize;
        if (mDisplay == DISPLAY_WORKSPACE && getTag() instanceof ItemInfo info && info.spanX > 1 && info.spanY > 1) {
            nextSize = Math.max(1, (int)Math.min(w * .76f, h * .70f));
        }
        if (mIcon != null && nextSize != mIconSize) {
            mIconSize = nextSize;
            mIcon.setBounds(0, 0, nextSize, nextSize);
            updateIcon(mIcon);
        }
        checkForEllipsis();''')
file='src/com/android/launcher3/folder/FolderIcon.java'
edit(file,'        icon.setOnClickListener(activity.getItemOnClickListener());','''        icon.setOnClickListener(view -> {
            if (!dev.dothome.ui.DotFolderPreview.launch(icon, folderInfo, icon.mDotTapX, icon.mDotTapY)) activity.getItemOnClickListener().onClick(view);
        });''')
edit(file,'    @Override\n    protected void dispatchDraw(Canvas canvas) {\n        super.dispatchDraw(canvas);','''    private float mDotTapX, mDotTapY;
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (dev.dothome.ui.DotFolderPreview.expanded(mInfo)) {
            dev.dothome.ui.DotFolderPreview.draw(canvas, this, mInfo);
            return;
        }
        super.dispatchDraw(canvas);''')
edit(file,'    public boolean onTouchEvent(MotionEvent event) {','    public boolean onTouchEvent(MotionEvent event) {\n        mDotTapX = event.getX(); mDotTapY = event.getY();')
edit(file,'    public void getPreviewBounds(Rect outBounds) {','''    public void getPreviewBounds(Rect outBounds) {
        if (dev.dothome.ui.DotFolderPreview.expanded(mInfo)) {
            outBounds.set(0,0,getWidth(),getHeight());
            return;
        }''')
print('Native Max Icons/Folders, direct expanded-folder taps and legacy repair-entry loading integrated.')

edit("src/com/android/launcher3/folder/FolderIcon.java", "    protected boolean shouldIgnoreTouchDown(float x, float y) {", "    protected boolean shouldIgnoreTouchDown(float x, float y) {\n        if (dev.dothome.ui.DotFolderPreview.expanded(mInfo)) return false;")

edit("src/com/android/launcher3/widget/DatabaseWidgetPreviewLoader.java", "        // Load the preview image if possible", "        android.graphics.Bitmap localPreview = dev.dothome.widgets.TilePreviews.render(mContext, info, maxPreviewWidth, preScaledWidthOut);\n        if (localPreview != null) return localPreview;\n        // Load the preview image if possible")

edit("src/com/android/launcher3/views/OptionsPopupView.java", "        launcher.getStateManager().goToState(EDIT_MODE);", "        dev.dothome.ui.DotArrange.begin(launcher);")
edit("src/com/android/launcher3/touch/ItemClickHandler.java", "    private static void onClick(View v) {", "    private static void onClick(View v) {\n        if (dev.dothome.ui.DotArrange.select(v)) return;")
edit("src/com/android/launcher3/folder/FolderIcon.java", "        icon.setOnClickListener(view -> {", "        icon.setOnClickListener(view -> {\n            if (dev.dothome.ui.DotArrange.select(view)) return;")

edit("src/com/android/launcher3/util/ItemInflater.kt", "            Favorites.ITEM_TYPE_SEARCH_ACTION -> {", "            Favorites.ITEM_TYPE_SEARCH_ACTION,\n            1 -> {")
# Native grid changes must retain local custom widgets and recovery shortcuts.
edit("src/com/android/launcher3/model/GridSizeMigrationUtil.java", "case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION: {", "case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:\n                        case 1: {")
edit("src/com/android/launcher3/model/GridSizeMigrationUtil.java", "case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET: {", "case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:\n                        case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET: {")
edit("src/com/android/launcher3/model/GridSizeMigrationUtil.java", "                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:\n                    // mProvider", "                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:\n                case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:\n                    // mProvider")
edit("src/com/android/launcher3/model/GridSizeMigrationUtil.java", "                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:\n                    final String", "                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:\n                case 1:\n                    final String")

# In-process widgets must not be recycled through the framework RemoteViews host.
edit("src/com/android/launcher3/widget/LauncherWidgetHolder.java", "            lahv.setAppWidget(0, appWidget);", "            lahv.setAppWidget(appWidgetId, appWidget);")
edit("src/com/android/launcher3/widget/LauncherWidgetHolder.java", "        // Binder can also inflate placeholder widgets", "        if (view.getAppWidgetInfo() instanceof LauncherAppWidgetProviderInfo local && local.isCustomWidget()) return view;\n        // Binder can also inflate placeholder widgets")

edit("src/com/android/launcher3/model/BgDataModel.java", "case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:", "case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:\n            case 1:")
edit("src/com/android/launcher3/Workspace.java", "boolean isWidget = itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;", "boolean isWidget = itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET || itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET;")
edit("src/com/android/launcher3/Workspace.java", "if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {", "if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET) {")
edit("src/com/android/launcher3/Workspace.java", "if (dragObject.dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET\n                    &&", "if ((dragObject.dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET || dragObject.dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET)\n                    &&")

# Framework color extraction reapplies RemoteViews and would erase local child views.
edit("src/com/android/launcher3/widget/LauncherAppWidgetHostView.java", "    public void updateAppWidget(RemoteViews remoteViews) {", "    public void updateAppWidget(RemoteViews remoteViews) {\n        if (getAppWidgetInfo() instanceof LauncherAppWidgetProviderInfo local && local.isCustomWidget()) return;")
edit("src/com/android/launcher3/widget/LauncherAppWidgetHostView.java", "    public void setColorResources(@Nullable SparseIntArray colors) {", "    public void setColorResources(@Nullable SparseIntArray colors) {\n        if (getAppWidgetInfo() instanceof LauncherAppWidgetProviderInfo local && local.isCustomWidget()) return;")
edit("src/com/android/launcher3/views/OptionsPopupView.java", "        dev.dothome.ui.DotArrange.begin(launcher);", "        AbstractFloatingView.closeAllOpenViews(launcher);\n        launcher.getDragLayer().post(() -> dev.dothome.ui.DotArrange.begin(launcher));")
