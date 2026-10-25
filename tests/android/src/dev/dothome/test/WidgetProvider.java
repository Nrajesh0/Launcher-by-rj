package dev.dothome.test;

import android.appwidget.*;
import android.content.Context;
import android.widget.RemoteViews;

/** Test-only provider installed separately from the offline launcher APK. */
public class WidgetProvider extends AppWidgetProvider {
  static void update(Context context, int id) {
    RemoteViews view = new RemoteViews("android", android.R.layout.simple_list_item_1);
    view.setTextViewText(android.R.id.text1, "Provider widget configured");
    AppWidgetManager.getInstance(context).updateAppWidget(id, view);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
    for (int id : ids) update(context, id);
  }
}
