package dev.dothome;

import android.content.Context;
import android.graphics.Typeface;

/** Bundled SIL-OFL fonts; never requests a downloadable font provider. */
final class LauncherTypography {
  private static Typeface regular, medium;

  static Typeface regular(Context context) {
    if (regular == null) regular = context.getResources().getFont(R.font.geist_regular);
    return regular;
  }

  static Typeface medium(Context context) {
    if (medium == null) medium = context.getResources().getFont(R.font.geist_medium);
    return medium;
  }
}
