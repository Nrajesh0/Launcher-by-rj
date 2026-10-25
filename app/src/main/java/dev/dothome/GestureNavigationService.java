package dev.dothome;

import android.accessibilityservice.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import java.util.*;

/** User-enabled, offline edge controls. Uses global actions; never retrieves window contents. */
public final class GestureNavigationService extends AccessibilityService {
  static volatile GestureNavigationService active;
  WindowManager windows;
  final ArrayList<View> edges = new ArrayList<>();
  final Handler handler = new Handler(Looper.getMainLooper());

  int dp(float v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    AccessibilityServiceInfo config = getServiceInfo();
    config.eventTypes = 0;
    config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
    config.flags = 0;
    setServiceInfo(config);
    active = this;
    windows = (WindowManager) getSystemService(WINDOW_SERVICE);
    rebuild();
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent ignored) {}

  @Override
  public void onInterrupt() {}

  @Override
  public void onConfigurationChanged(android.content.res.Configuration c) {
    super.onConfigurationChanged(c);
    rebuild();
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacksAndMessages(null);
    for (View v : edges)
      try {
        windows.removeView(v);
      } catch (Exception ignored) {
      }
    edges.clear();
    if (active == this) active = null;
    super.onDestroy();
  }

  void rebuild() {
    for (View v : edges)
      try {
        windows.removeView(v);
      } catch (Exception ignored) {
      }
    edges.clear();
    int height = windows.getCurrentWindowMetrics().getBounds().height();
    addEdge(0, -1, dp(36), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    addEdge(1, dp(12), Math.max(dp(120), height - dp(240)), Gravity.LEFT | Gravity.CENTER_VERTICAL);
    addEdge(
        2, dp(12), Math.max(dp(120), height - dp(240)), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
  }

  void addEdge(int kind, int width, int height, int gravity) {
    Edge edge = new Edge(kind);
    WindowManager.LayoutParams lp =
        new WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    lp.gravity = gravity;
    lp.setFitInsetsTypes(0);
    if (kind == 0) {
      lp.y =
          windows
              .getCurrentWindowMetrics()
              .getWindowInsets()
              .getInsets(WindowInsets.Type.navigationBars())
              .bottom;
      edge.setOnApplyWindowInsetsListener(
          (view, insets) -> {
            int bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            if (lp.y != bottom) {
              lp.y = bottom;
              view.post(
                  () -> {
                    if (view.isAttachedToWindow())
                      try {
                        windows.updateViewLayout(view, lp);
                      } catch (Exception ignored) {
                      }
                  });
            }
            return insets;
          });
    }
    lp.setTitle("Dot Home edge gestures");
    try {
      windows.addView(edge, lp);
      edges.add(edge);
    } catch (Exception ignored) {
    }
  }

  final class Edge extends View {
    final int kind;
    float startX, startY;
    boolean recent, scheduled, touching;
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Runnable hold =
        () -> {
          if (touching) {
            recent = true;
            performGlobalAction(GLOBAL_ACTION_RECENTS);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
          }
        };

    Edge(int kind) {
      super(GestureNavigationService.this);
      this.kind = kind;
      setContentDescription(
          kind == 0 ? "Swipe up for Home; swipe up and hold for Recents" : "Swipe inward for Back");
    }

    @Override
    protected void onDraw(Canvas c) {
      if (kind == 0) {
        paint.setColor(0x99eeeeee);
        c.drawRoundRect(
            new RectF(
                getWidth() / 2f - dp(38),
                getHeight() - dp(9),
                getWidth() / 2f + dp(38),
                getHeight() - dp(5)),
            dp(2),
            dp(2),
            paint);
      }
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
      switch (e.getAction()) {
        case MotionEvent.ACTION_DOWN:
          startX = e.getRawX();
          startY = e.getRawY();
          recent = false;
          scheduled = false;
          touching = true;
          return true;
        case MotionEvent.ACTION_MOVE:
          if (kind == 0 && startY - e.getRawY() > dp(28) && !scheduled) {
            scheduled = true;
            handler.postDelayed(hold, 450);
          }
          return true;
        case MotionEvent.ACTION_UP:
          touching = false;
          handler.removeCallbacks(hold);
          float dx = e.getRawX() - startX, dy = e.getRawY() - startY;
          if (kind == 0) {
            if (!recent && dy < -dp(30)) performGlobalAction(GLOBAL_ACTION_HOME);
          } else if ((kind == 1 && dx > dp(32)) || (kind == 2 && dx < -dp(32)))
            performGlobalAction(GLOBAL_ACTION_BACK);
          return true;
        case MotionEvent.ACTION_CANCEL:
          touching = false;
          handler.removeCallbacks(hold);
          return true;
      }
      return true;
    }
  }
}
