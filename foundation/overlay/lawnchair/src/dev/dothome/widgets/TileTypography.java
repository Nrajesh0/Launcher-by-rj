package dev.dothome.widgets;
import android.content.Context;
import android.graphics.Typeface;
import com.android.launcher3.R;
final class TileTypography {
  static Typeface normal, medium, display, mono;
  static Typeface regular(Context context) { if(normal==null) normal=context.getResources().getFont(R.font.geist_regular); return normal; }
  static Typeface medium(Context context) { if(medium==null) medium=context.getResources().getFont(R.font.geist_medium); return medium; }
  static Typeface display(Context context) {if(display==null)display=context.getResources().getFont(R.font.dot_display);return display;}
  static Typeface mono(Context context) {if(mono==null)mono=context.getResources().getFont(R.font.dot_mono);return mono;}
}
