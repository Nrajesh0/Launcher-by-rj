package dev.dothome.widgets;

import android.app.usage.*;
import android.content.Context;
import java.util.*;

/** Derives screen-off breaks from local system screen events, without polling or a service. */
final class AwayTimeStats {
  final boolean available;
  final long current, longest;
  final long[] daily;

  AwayTimeStats(boolean available, long current, long longest, long[] daily) {
    this.available = available;
    this.current = current;
    this.longest = longest;
    this.daily = daily;
  }

  static AwayTimeStats unavailable() {
    return new AwayTimeStats(false, 0, 0, new long[7]);
  }

  static AwayTimeStats read(Context context, long now) {
    Calendar start = Calendar.getInstance();
    start.setTimeInMillis(now);
    start.set(Calendar.HOUR_OF_DAY, 0);
    start.set(Calendar.MINUTE, 0);
    start.set(Calendar.SECOND, 0);
    start.set(Calendar.MILLISECOND, 0);
    long[] boundaries = new long[8];
    start.add(Calendar.DAY_OF_YEAR, -6);
    for (int i = 0; i < 8; i++) {
      boundaries[i] = start.getTimeInMillis();
      start.add(Calendar.DAY_OF_YEAR, 1);
    }
    UsageEvents events =
        context.getSystemService(UsageStatsManager.class).queryEvents(boundaries[0], now);
    UsageEvents.Event event = new UsageEvents.Event();
    long off = -1, longest = 0;
    boolean observed = false;
    long[] totals = new long[7];
    while (events.hasNextEvent()) {
      events.getNextEvent(event);
      if (event.getEventType() == UsageEvents.Event.SCREEN_NON_INTERACTIVE
          || event.getEventType() == UsageEvents.Event.SCREEN_INTERACTIVE) observed = true;
      if (event.getEventType() == UsageEvents.Event.SCREEN_NON_INTERACTIVE && off < 0)
        off = event.getTimeStamp();
      else if (event.getEventType() == UsageEvents.Event.SCREEN_INTERACTIVE && off >= 0) {
        long end = Math.min(now, event.getTimeStamp());
        longest = Math.max(longest, Math.max(0, end - off));
        accumulate(totals, boundaries, off, end);
        off = -1;
      }
    }
    if (off >= 0) {
      longest = Math.max(longest, now - off);
      accumulate(totals, boundaries, off, now);
    }
    if (!observed) return unavailable();
    return new AwayTimeStats(true, off < 0 ? 0 : Math.max(0, now - off), longest, totals);
  }

  static void accumulate(long[] totals, long[] boundaries, long start, long end) {
    for (int n = 0; n < 7; n++)
      totals[n] += Math.max(0, Math.min(end, boundaries[n + 1]) - Math.max(start, boundaries[n]));
  }

  static String duration(long value) {
    long minutes = Math.max(0, value) / 60000;
    return minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
  }
}
