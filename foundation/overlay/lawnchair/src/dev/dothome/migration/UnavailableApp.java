package dev.dothome.migration;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
/** A recoverable local entry when a saved app/profile is not currently available. */
public final class UnavailableApp extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout body=new LinearLayout(this);body.setOrientation(1);int pad=Math.round(28*getResources().getDisplayMetrics().density);body.setPadding(pad,pad*3,pad,pad);
    TextView title=new TextView(this);title.setText(getIntent().getStringExtra("label"));title.setTextSize(24);body.addView(title);
    TextView message=new TextView(this);message.setText("This saved app or profile is unavailable. Its original placement is preserved in your recovery copy. Enable the profile or add an installed app from Apps.");message.setTextSize(16);body.addView(message);
    Button close=new Button(this);close.setText("Return Home");close.setOnClickListener(v->finish());body.addView(close);setContentView(body);
  }
}
