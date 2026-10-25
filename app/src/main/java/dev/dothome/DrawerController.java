package dev.dothome;

import android.content.*;
import android.graphics.Color;
import android.os.Handler;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;
import org.json.JSONObject;

/** Dedicated, recycled app drawer. Search and organisation remain entirely local. */
final class DrawerController {
  final LauncherUi ui;
  final HomeActivity a;
  final Consumer<HomeActivity.App> pick;
  final ArrayList<HomeActivity.App> matched = new ArrayList<>();
  final ArrayList<Integer> groups = new ArrayList<>();
  final String[] groupNames = {
    "Games",
    "Audio",
    "Video",
    "Images",
    "Social",
    "News",
    "Maps",
    "Productivity",
    "Accessibility",
    "Other"
  };
  boolean showingGroups;
  GridView grid;
  TextView empty, mode;
  EditText search;
  int category = -1;
  boolean organised, loaded, restored;
  final int savedPosition;
  LinearLayout body;
  final Handler debounce = new Handler(android.os.Looper.getMainLooper());
  final Runnable filter = this::filter;
  final DrawerAdapter adapter = new DrawerAdapter();

  final class DrawerAdapter extends BaseAdapter implements SectionIndexer {
    public int getCount() {
      return showingGroups ? groups.size() : matched.size();
    }

    public HomeActivity.App getItem(int at) {
      return matched.get(at);
    }

    public long getItemId(int at) {
      return showingGroups ? groups.get(at) : getItem(at).key.hashCode();
    }

    public View getView(int at, View recycled, ViewGroup parent) {
      FrameLayout cell =
          recycled instanceof FrameLayout ? (FrameLayout) recycled : new FrameLayout(a);
      cell.removeAllViews();
      if (showingGroups) {
        int category = groups.get(at);
        LinearLayout folder = ui.vertical();
        folder.setGravity(Gravity.CENTER);
        folder.setBackground(a.shape(a.card, 22));
        GridLayout mosaic = new GridLayout(a);
        mosaic.setColumnCount(2);
        int count = 0;
        for (HomeActivity.App app : matched)
          if (groupOf(app) == category && count++ < 4) {
            ImageView mini = new ImageView(a);
            mini.setImageDrawable(a.icon(app));
            mosaic.addView(mini, new ViewGroup.LayoutParams(ui.d(25), ui.d(25)));
          }
        folder.addView(mosaic, new LinearLayout.LayoutParams(-2, ui.d(52)));
        TextView label = a.text(groupNames[category], 11, a.fg);
        label.setGravity(Gravity.CENTER);
        folder.addView(label);
        cell.addView(folder, new FrameLayout.LayoutParams(-1, -1));
        cell.setLayoutParams(new AbsListView.LayoutParams(-1, ui.d(104)));
        cell.setContentDescription(groupNames[category] + " category");
        return cell;
      }
      HomeActivity.App app = getItem(at);
      View icon = a.appView(app, true);
      // GridView owns clicks and long presses; child listeners must not consume them.
      icon.setOnClickListener(null);
      icon.setClickable(false);
      icon.setOnLongClickListener(null);
      icon.setLongClickable(false);
      cell.addView(icon, new FrameLayout.LayoutParams(-1, -1));
      cell.setLayoutParams(new AbsListView.LayoutParams(-1, ui.d(96)));
      cell.setContentDescription(app.label);
      return cell;
    }

    public Object[] getSections() {
      if (showingGroups) return groups.stream().map(i -> groupNames[i]).toArray();
      LinkedHashSet<String> sections = new LinkedHashSet<>();
      for (HomeActivity.App app : matched) sections.add(initial(app.label));
      return sections.toArray();
    }

    public int getPositionForSection(int section) {
      Object[] sections = getSections();
      if (sections.length == 0) return 0;
      if (showingGroups) return Math.max(0, Math.min(section, groups.size() - 1));
      String key = String.valueOf(sections[Math.max(0, Math.min(section, sections.length - 1))]);
      for (int n = 0; n < matched.size(); n++)
        if (initial(matched.get(n).label).equals(key)) return n;
      return 0;
    }

    public int getSectionForPosition(int position) {
      if (showingGroups) return position;
      if (matched.isEmpty()) return 0;
      return Arrays.asList(getSections())
          .indexOf(initial(matched.get(Math.min(position, matched.size() - 1)).label));
    }
  }

  static String initial(String label) {
    if (label.isEmpty()) return "#";
    int cp = label.codePointAt(0);
    return Character.isLetter(cp)
        ? new String(Character.toChars(cp)).toUpperCase(Locale.ROOT)
        : "#";
  }

  DrawerController(LauncherUi ui, Consumer<HomeActivity.App> pick) {
    this.ui = ui;
    a = ui.a;
    this.pick = pick;
    organised = a.settings.optBoolean("drawerOrganised", false);
    savedPosition = ui.drawerPosition;
  }

  void mount() {
    body = ui.surface(pick == null ? "Apps" : "Choose an app", true);
    ui.changeState("drawer");
    body.setPadding(ui.d(16), ui.d(12), ui.d(16), ui.d(12));
    body.removeViewAt(0);
    LinearLayout header = new LinearLayout(a);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView heading = ui.title(pick == null ? "Apps" : "Choose an app", 24);
    heading.setAccessibilityHeading(true);
    header.addView(heading, new LinearLayout.LayoutParams(0, ui.d(48), 1));
    TextView menu = ui.button("•••", this::menu);
    menu.setContentDescription("App drawer options");
    header.addView(menu, new LinearLayout.LayoutParams(ui.d(52), ui.d(48)));
    body.addView(header);
    mode = ui.button(organised ? "Organised · All apps" : "All apps · A–Z", this::chooseCategory);
    mode.setTextSize(13);
    body.addView(mode, new LinearLayout.LayoutParams(-1, ui.d(40)));
    FrameLayout content = new FrameLayout(a);
    grid =
        new GridView(a) {
          float startY, startX;
          boolean pulling;

          @Override
          public boolean onInterceptTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
              startY = event.getRawY();
              startX = event.getRawX();
              pulling = false;
            }
            float dy = event.getRawY() - startY;
            boolean top =
                getFirstVisiblePosition() == 0
                    && (getChildCount() == 0 || getChildAt(0).getTop() >= getPaddingTop());
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                && top
                && dy > ui.d(24)
                && dy > Math.abs(event.getRawX() - startX) * 1.4f) {
              pulling = true;
              body.animate().cancel();
              return true;
            }
            return super.onInterceptTouchEvent(event);
          }

          @Override
          public boolean onTouchEvent(MotionEvent event) {
            if (!pulling) return super.onTouchEvent(event);
            float dy = Math.max(0, event.getRawY() - startY);
            body.setTranslationY(dy);
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
              pulling = false;
              if (event.getActionMasked() == MotionEvent.ACTION_UP && dy > ui.d(90)) ui.close();
              else body.animate().translationY(0).setDuration(ui.duration()).start();
            }
            return true;
          }
        };
    grid.setNumColumns(a.cols());
    grid.setColumnWidth(ui.d(72));
    grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
    grid.setVerticalSpacing(ui.d(5));
    grid.setClipToPadding(false);
    grid.setPadding(0, ui.d(12), 0, ui.d(12));
    grid.setFastScrollEnabled(true);
    grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    grid.setAdapter(adapter);
    grid.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (showingGroups) {
            category = groups.get(position);
            mode.setText(groupNames[category] + " · All categories");
            filter();
          } else choose(matched.get(position));
        });
    grid.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          if (showingGroups) return false;
          HomeActivity.App app = matched.get(position);
          if (pick != null) {
            choose(app);
            return true;
          }
          JSONObject item = a.obj("app");
          HomeActivity.put(item, "key", app.key);
          HomeActivity.put(item, "label", app.label);
          ui.beginDrag(view, item, true);
          return true;
        });
    content.addView(grid, new FrameLayout.LayoutParams(-1, -1));
    empty = a.text("No matching apps", 15, a.muted);
    empty.setGravity(Gravity.CENTER);
    content.addView(empty, new FrameLayout.LayoutParams(-1, -1));
    body.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
    search = new EditText(a);
    a.search = search;
    search.setTypeface(LauncherTypography.regular(a));
    search.setSingleLine();
    search.setTextSize(15);
    search.setTextColor(a.fg);
    search.setHintTextColor(a.muted);
    search.setHint("Search apps");
    search.setContentDescription("Search apps");
    search.setPadding(ui.d(18), 0, ui.d(18), 0);
    search.setBackground(a.shape(a.card, 100));
    search.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    search.addTextChangedListener(
        new android.text.TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int f) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            debounce.removeCallbacks(filter);
            debounce.postDelayed(filter, 60);
          }

          public void afterTextChanged(android.text.Editable e) {}
        });
    search.setOnEditorActionListener(
        (v, action, event) -> {
          filter();
          if (matched.isEmpty()) return false;
          choose(matched.get(0));
          return true;
        });
    body.addView(search, new LinearLayout.LayoutParams(-1, ui.d(52)));
    body.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
          int navigation = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
          body.setPadding(ui.d(16), ui.d(12), ui.d(16), Math.max(ui.d(12), ime - navigation));
          return insets;
        });
    filter();
    if (ui.drawerPosition > 0) grid.setSelection(ui.drawerPosition);
    grid.setOnScrollListener(
        new AbsListView.OnScrollListener() {
          public void onScrollStateChanged(AbsListView view, int state) {}

          public void onScroll(AbsListView view, int first, int visible, int total) {
            if (loaded && search.getText().length() == 0 && category < 0) ui.drawerPosition = first;
          }
        });
  }

  void choose(HomeActivity.App app) {
    ui.close();
    if (pick == null) a.launch(app.key);
    else pick.accept(app);
  }

  void filter() {
    if (ui.drawerController != this) return;
    String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
    matched.clear();
    groups.clear();
    showingGroups = organised && category < 0 && query.isEmpty();
    for (HomeActivity.App app : a.apps)
      if (!a.hidden(app.key)
          && app.label.toLowerCase(Locale.ROOT).contains(query)
          && (category < 0 || groupOf(app) == category)) matched.add(app);
    if (organised && category < 0)
      matched.sort(
          (x, y) -> {
            int compare = Integer.compare(a.categoryOf(x), a.categoryOf(y));
            return compare != 0
                ? compare
                : java.text.Collator.getInstance().compare(x.label, y.label);
          });
    if (showingGroups) {
      java.util.TreeSet<Integer> categories = new java.util.TreeSet<>();
      for (HomeActivity.App app : matched) categories.add(groupOf(app));
      groups.addAll(categories);
    }
    adapter.notifyDataSetChanged();
    if (!restored) {
      restored = true;
      grid.post(
          () -> {
            grid.setSelection(Math.min(savedPosition, Math.max(0, adapter.getCount() - 1)));
            loaded = true;
          });
    }
    empty.setVisibility(matched.isEmpty() ? View.VISIBLE : View.GONE);
  }

  void menu() {
    a.menu(
        "App drawer",
        new String[] {
          "Alphabetical", "Organised", "Categories", "Hidden apps", "Assign category", "Close Apps"
        },
        n -> {
          if (n < 2) {
            organised = n == 1;
            category = -1;
            HomeActivity.put(a.settings, "drawerOrganised", organised);
            a.save();
            mode.setText(organised ? "Organised · All apps" : "All apps · A–Z");
            filter();
          } else if (n == 2) chooseCategory();
          else if (n == 3) ui.hiddenApps();
          else if (n == 4) assignCategory();
          else ui.close();
        });
  }

  int groupOf(HomeActivity.App app) {
    int category = a.categoryOf(app);
    return category >= 0 && category < 9 ? category : 9;
  }

  void assignCategory() {
    ui.drawer(
        app ->
            a.menu(
                "Category for " + app.label,
                groupNames,
                selected -> {
                  JSONObject overrides = a.settings.optJSONObject("categories");
                  if (overrides == null) {
                    overrides = new JSONObject();
                    HomeActivity.put(a.settings, "categories", overrides);
                  }
                  HomeActivity.put(overrides, app.key, selected);
                  a.save();
                  ui.drawer(null);
                }));
  }

  void chooseCategory() {
    String[] labels = {
      "All apps",
      "Games",
      "Audio",
      "Video",
      "Images",
      "Social",
      "News",
      "Maps",
      "Productivity",
      "Accessibility",
      "Other"
    };
    a.menu(
        "Categories",
        labels,
        n -> {
          category = n - 1;
          mode.setText(labels[n]);
          filter();
        });
  }

  void dispose() {
    debounce.removeCallbacksAndMessages(null);
  }
}
