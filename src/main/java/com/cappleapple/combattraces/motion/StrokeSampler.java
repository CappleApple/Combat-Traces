package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.SwingWindow;
import java.util.List;

/** Clips rendered motion to a strike and keeps a started stroke continuous through deceleration. */
public final class StrokeSampler {
  private long attack = Long.MIN_VALUE;
  private TrailSample previous;
  private SwingWindow window;
  private boolean started;

  public double lastTime() {
    return previous == null ? Double.NEGATIVE_INFINITY : previous.time();
  }

  public List<TrailSample> advance(
      long attackId,
      TrailSample sample,
      SwingWindow bounds,
      double speed,
      double minimum,
      double discontinuity) {
    if (!finite(sample) || bounds == null || !Double.isFinite(speed) || !Double.isFinite(minimum)) {
      previous = null;
      started = false;
      return List.of();
    }
    var before = previous;
    boolean reset =
        attack != attackId
            || !bounds.equals(window)
            || before == null
            || sample.progress() < before.progress()
            || sample.time() - before.time() > .3
            || sample.origin().distanceTo(before.origin()) > discontinuity
            || sample.tip().distanceTo(before.tip()) > discontinuity;
    attack = attackId;
    window = bounds;
    if (reset) {
      before = null;
      started = false;
    }
    if (before != null && sample.time() <= before.time()) return List.of();
    previous = sample;
    if (before == null) {
      started = bounds.contains(sample.progress()) && speed >= Math.max(.01, minimum);
      return started ? List.of(sample) : List.of();
    }
    float from = Math.max(before.progress(), bounds.start());
    float to = Math.min(sample.progress(), bounds.end());
    if (from > to || before.progress() >= bounds.end() || sample.progress() < bounds.start()) {
      if (sample.progress() > bounds.end()) started = false;
      return List.of();
    }
    if (!started && speed < Math.max(.01, minimum)) return List.of();
    boolean seed = !started;
    started = sample.progress() < bounds.end();
    if (sample.progress() == before.progress()) return seed ? List.of(sample) : List.of();
    var end = at(before, sample, to);
    // The entry and exit can both occur between two frames. Keep both boundary poses.
    return seed && from < to ? List.of(at(before, sample, from), end) : List.of(end);
  }

  private static TrailSample at(TrailSample before, TrailSample after, float progress) {
    if (progress == before.progress()) return before;
    if (progress == after.progress()) return after;
    double t = (progress - before.progress()) / (after.progress() - before.progress());
    var interpolated = SweptTrail.interpolate(before, after, Math.clamp(t, 0, 1));
    return new TrailSample(
        interpolated.origin(), interpolated.tip(), interpolated.time(), progress);
  }

  private static boolean finite(TrailSample sample) {
    return sample != null
        && TrailHistory.finite(sample.origin())
        && TrailHistory.finite(sample.tip())
        && Double.isFinite(sample.time())
        && Float.isFinite(sample.progress())
        && sample.progress() >= 0;
  }
}
