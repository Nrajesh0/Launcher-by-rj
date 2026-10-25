package dev.dothome;

import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.Settings;

/** Observes Android's actual Home assignment independently of setup presentation. */
final class HomeRoleController {
  static final int REQUEST = 100;
  final HomeActivity activity;
  boolean pending;

  HomeRoleController(HomeActivity activity) {
    this.activity = activity;
  }

  boolean isDefault() {
    RoleManager roles = activity.getSystemService(RoleManager.class);
    if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME))
      return roles.isRoleHeld(RoleManager.ROLE_HOME);
    Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
    android.content.pm.ResolveInfo resolved =
        activity.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
    return resolved != null
        && resolved.activityInfo != null
        && resolved.activityInfo.packageName.equals(activity.getPackageName())
        && resolved.activityInfo.name.equals(HomeActivity.class.getName());
  }

  void refresh() {
    boolean selected = isDefault();
    if (selected) {
      pending = false;
      activity.prefs.edit().putBoolean("homeConfirmed", true).apply();
    }
    if (activity.ui != null) activity.ui.updateHomeStatus();
  }

  void request() {
    refresh();
    if (isDefault()) {
      activity.toast("Dot Home is already your default launcher");
      return;
    }
    if (pending) return;
    RoleManager roles = activity.getSystemService(RoleManager.class);
    try {
      if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
        pending = true;
        activity.startActivityForResult(
            roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST);
        activity.prefs.edit().putBoolean("homePromptShown", true).apply();
        return;
      }
    } catch (ActivityNotFoundException | SecurityException unavailable) {
      pending = false;
    }
    activity.open(new Intent(Settings.ACTION_HOME_SETTINGS));
  }

  void result() {
    pending = false;
    refresh();
    if (!isDefault())
      activity.toast("Default Home was not changed. You can choose it again in setup.");
  }

  String diagnostics() {
    Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
    android.content.pm.ResolveInfo resolved =
        activity.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
    String component =
        resolved == null || resolved.activityInfo == null
            ? "unresolved"
            : new ComponentName(resolved.activityInfo.packageName, resolved.activityInfo.name)
                .flattenToShortString();
    return "Default Home confirmed: "
        + isDefault()
        + "\nResolved Home: "
        + component
        + "\nSelection pending: "
        + pending
        + "\nApp: "
        + BuildInfo.VERSION_NAME;
  }
}
