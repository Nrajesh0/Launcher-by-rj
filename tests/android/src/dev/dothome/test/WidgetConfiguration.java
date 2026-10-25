package dev.dothome.test;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

public class WidgetConfiguration extends Activity {
  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setResult(RESULT_CANCELED);
    int id =
        getIntent()
            .getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(1);
    Button apply = new Button(this);
    apply.setText("Apply test widget");
    apply.setOnClickListener(
        v -> {
          WidgetProvider.update(this, id);
          setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
          finish();
        });
    body.addView(apply);
    Button cancel = new Button(this);
    cancel.setText("Cancel test widget");
    cancel.setOnClickListener(v -> finish());
    body.addView(cancel);
    setContentView(body);
  }
}
