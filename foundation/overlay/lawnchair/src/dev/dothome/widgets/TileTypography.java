package dev.dothome.widgets;
import android.content.Context;
import android.graphics.Typeface;
import com.android.launcher3.R;
final class TileTypography {
  static Typeface normal, medium;
  static Typeface regular(Context context) { if(normal==null) normal=context.getResources().getFont(R.font.geist_regular); return normal; }
  static Typeface medium(Context context) { if(medium==null) medium=context.getResources().getFont(R.font.geist_medium); return medium; }
}
