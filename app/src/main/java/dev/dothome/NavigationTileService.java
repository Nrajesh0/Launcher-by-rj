package dev.dothome;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.*;

/** System Quick Settings recovery entry; no app overlay or network permissions. */
public final class NavigationTileService extends TileService {
  @Override
  public void onStartListening() {
    Tile tile = getQsTile();
    if (tile != null) {
      tile.setLabel("Dot Home navigation");
      tile.setState(
          GestureNavigationService.active == null ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
      tile.updateTile();
    }
  }

  @Override
  public void onClick() {
    Intent intent =
        new Intent(this, HomeActivity.class)
            .putExtra("navigationSetup", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivityAndCollapse(
        PendingIntent.getActivity(
            this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
  }
}
