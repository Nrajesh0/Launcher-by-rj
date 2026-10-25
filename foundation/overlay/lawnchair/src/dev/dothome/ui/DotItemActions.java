package dev.dothome.ui;

import android.graphics.Rect;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.*;
import com.android.launcher3.model.data.*;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.AbstractFloatingView;

/** Native item actions retain the core's placement model and database writer. */
public final class DotItemActions {
  public static final SystemShortcut.Factory<Launcher> SIZE=(launcher,item,view)-> {
    if(item.container!=LauncherSettings.Favorites.CONTAINER_DESKTOP || !(item instanceof WorkspaceItemInfo || item instanceof FolderInfo)) return null;
    return new Size(launcher,item,view);
  };
  private static final class Size extends SystemShortcut<Launcher> {
    Size(Launcher launcher,ItemInfo item,View view) { super(R.drawable.ic_widget,item.spanX>1?R.string.dot_shrink:R.string.dot_enlarge,launcher,item,view); }
    @Override public void onClick(View view) {
      if (DotArrange.locked(mTarget)) return;
      int span=mItemInfo.spanX>1?1:2;
      var grid=InvariantDeviceProfile.INSTANCE.get(mTarget);
      Rect desired=new Rect(mItemInfo.cellX,mItemInfo.cellY,mItemInfo.cellX+span,mItemInfo.cellY+span);
      boolean fits=desired.right<=grid.numColumns && desired.bottom<=grid.numRows;
      CellLayout page=mTarget.getWorkspace().getScreenWithId(mItemInfo.screenId);
      if(page==null) return;
      var children=page.getShortcutsAndWidgets();
      for(int n=0;n<children.getChildCount();n++) {
        Object tag=children.getChildAt(n).getTag();
        if(tag instanceof ItemInfo other && other.id!=mItemInfo.id && Rect.intersects(desired,new Rect(other.cellX,other.cellY,other.cellX+other.spanX,other.cellY+other.spanY))) fits=false;
      }
      if(!fits) { Toast.makeText(mTarget,"Make room beside this item first",Toast.LENGTH_SHORT).show();return; }
      AbstractFloatingView.closeAllOpenViews(mTarget);
      mTarget.getModelWriter().modifyItemInDatabase(mItemInfo,mItemInfo.container,mItemInfo.screenId,mItemInfo.cellX,mItemInfo.cellY,span,span);
      LauncherAppState.getInstance(mTarget).getModel().forceReload();
    }
  }
}
