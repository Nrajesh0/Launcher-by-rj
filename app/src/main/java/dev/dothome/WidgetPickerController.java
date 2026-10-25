package dev.dothome;

import android.appwidget.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.JSONObject;

/** Searchable widget library with shared built-in previews and grouped Android providers. */
final class WidgetPickerController {
  final LauncherUi ui;
  final HomeActivity a;
  final Set<String> expanded = new HashSet<>();
  final Map<String, List<AppWidgetProviderInfo>> apps = new TreeMap<>();
  final Map<String, String> labels = new HashMap<>();
  LinearLayout list;
  EditText search;

  WidgetPickerController(LauncherUi ui) {
    this.ui = ui;
    a = ui.a;
  }

  void mount() {
    LinearLayout body = ui.surface("Widgets", true);
    ui.changeState("picker");
    search = new EditText(a);
    search.setSingleLine();
    search.setTextSize(15);
    search.setTypeface(LauncherTypography.regular(a));
    search.setTextColor(a.fg);
    search.setHintTextColor(a.muted);
    search.setHint("Search widgets");
    search.setPadding(ui.d(16), 0, ui.d(16), 0);
    search.setBackground(a.shape(a.card, 100));
    body.addView(search, new LinearLayout.LayoutParams(-1, ui.d(48)));
    ScrollView scroll = new ScrollView(a);
    list = ui.vertical();
    scroll.addView(list);
    body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    for (AppWidgetProviderInfo provider : AppWidgetManager.getInstance(a).getInstalledProviders()) {
      String pkg = provider.provider.getPackageName();
      apps.computeIfAbsent(pkg, key -> new ArrayList<>()).add(provider);
      try {
        labels.put(
            pkg,
            a.getPackageManager()
                .getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg, 0))
                .toString());
      } catch (Exception missing) {
        labels.put(pkg, pkg);
      }
    }
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int f) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            populate();
          }

          public void afterTextChanged(android.text.Editable e) {}
        });
    populate();
  }

  void populate() {
    list.removeAllViews();
    String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
    list.addView(ui.title("Dot Home", 19), new LinearLayout.LayoutParams(-1, ui.d(46)));
    GridLayout builtins = new GridLayout(a);
    builtins.setColumnCount(2);
    int at = 0;
    for (int n = 0; n < ui.types.length; n++) {
      final String type = ui.types[n];
      if (!ui.names[n].toLowerCase(Locale.ROOT).contains(query)) continue;
      LinearLayout card = ui.vertical();
      card.setPadding(ui.d(10), ui.d(8), ui.d(10), ui.d(8));
      card.setBackground(a.shape(a.card, 22));
      card.addView(ui.title(ui.names[n], 15), new LinearLayout.LayoutParams(-1, ui.d(30)));
      JSONObject sample = ui.sample(type, 0);
      WidgetArtwork art = new WidgetArtwork(a, sample, true);
      card.addView(art, new LinearLayout.LayoutParams(-1, ui.d(104)));
      card.addView(
          a.text(
              WidgetCatalogue.styles(type).length
                  + " styles · "
                  + WidgetCatalogue.sizes(type).length
                  + " sizes",
              11,
              a.muted));
      a.click(card, () -> ui.stylePicker(type, null));
      a.click(art, () -> ui.stylePicker(type, null));
      art.setOnLongClickListener(
          v -> {
            ui.beginDrag(v, ui.sample(type, 0), true);
            return true;
          });
      GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
      lp.width = 0;
      lp.height = ui.d(174);
      lp.columnSpec = GridLayout.spec(at++ % 2, 1f);
      lp.setMargins(ui.d(3), ui.d(4), ui.d(3), ui.d(4));
      builtins.addView(card, lp);
    }
    list.addView(builtins);
    ArrayList<String> packages = new ArrayList<>(apps.keySet());
    packages.sort((x, y) -> labels.get(x).compareToIgnoreCase(labels.get(y)));
    for (String pkg : packages) {
      ArrayList<AppWidgetProviderInfo> matches = new ArrayList<>();
      for (AppWidgetProviderInfo info : apps.get(pkg))
        if (labels.get(pkg).toLowerCase(Locale.ROOT).contains(query)
            || info.loadLabel(a.getPackageManager()).toLowerCase(Locale.ROOT).contains(query))
          matches.add(info);
      if (matches.isEmpty()) continue;
      TextView header =
          ui.button(
              labels.get(pkg)
                  + " · "
                  + matches.size()
                  + (expanded.contains(pkg) || !query.isEmpty() ? "  −" : "  +"),
              () -> {
                if (!expanded.remove(pkg)) expanded.add(pkg);
                populate();
              });
      LinearLayout.LayoutParams space = new LinearLayout.LayoutParams(-1, ui.d(56));
      space.topMargin = ui.d(10);
      list.addView(header, space);
      if (!expanded.contains(pkg) && query.isEmpty()) continue;
      for (AppWidgetProviderInfo info : matches) {
        LinearLayout row = ui.vertical();
        row.setPadding(ui.d(12), ui.d(12), ui.d(12), ui.d(12));
        row.addView(ui.title(info.loadLabel(a.getPackageManager()), 16));
        ImageView preview = new ImageView(a);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(preview, new LinearLayout.LayoutParams(-1, ui.d(130)));
        a.worker.execute(
            () -> {
              Drawable image = null;
              try {
                image = info.loadPreviewImage(a, a.getResources().getDisplayMetrics().densityDpi);
                if (image == null)
                  image = info.loadIcon(a, a.getResources().getDisplayMetrics().densityDpi);
              } catch (Exception unavailable) {
              }
              Drawable result = image;
              preview.post(() -> preview.setImageDrawable(result));
            });
        row.addView(a.text("Tap to add and configure", 12, a.muted));
        a.click(
            row,
            () -> {
              ui.close();
              a.addProvider(info);
            });
        list.addView(row);
      }
    }
    if (at == 0 && apps.isEmpty()) list.addView(a.text("No matching widgets", 15, a.muted));
  }
}
