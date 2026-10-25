package dev.dothome;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Read-only rendering of the actual Home hierarchy; not an invented thumbnail layout. */
final class HomePreview extends View {
  final LauncherUi ui;

  HomePreview(LauncherUi ui) {
    super(ui.a);
    this.ui = ui;
    setContentDescription("Home screen preview");
  }

  @Override
  protected void onDraw(Canvas canvas) {
    View root = ui.a.root;
    if (root == null || root.getWidth() == 0 || root.getHeight() == 0) return;
    float scale =
        Math.min(getWidth() / (float) root.getWidth(), getHeight() / (float) root.getHeight());
    canvas.save();
    canvas.translate(
        (getWidth() - root.getWidth() * scale) / 2, (getHeight() - root.getHeight() * scale) / 2);
    canvas.scale(scale, scale);
    Drawable wall = ui.screen.getBackground();
    Rect old = new Rect(wall.getBounds());
    wall.setBounds(0, 0, root.getWidth(), root.getHeight());
    wall.draw(canvas);
    wall.setBounds(old);
    root.draw(canvas);
    canvas.restore();
  }
}
