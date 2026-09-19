package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;

/** Fixed-capacity ring; a frame can never allocate an unbounded interpolation burst. */
public final class TrailHistory {
  private final TrailSample[] samples;
  private int start, size;
  private TrailSample previous;

  public TrailHistory(int capacity) {
    samples = new TrailSample[Math.clamp(capacity, 2, 64)];
  }

  public int size() {
    return size;
  }

  public TrailSample get(int index) {
    if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    return samples[(start + index) % samples.length];
  }

  public TrailSample latest() {
    return previous;
  }

  public void clear() {
    java.util.Arrays.fill(samples, null);
    start = 0;
    size = 0;
    previous = null;
  }

  public void dropOldest() {
    if (size > 0) {
      samples[start] = null;
      start = (start + 1) % samples.length;
      size--;
    }
  }

  public void prune(double before) {
    while (size > 0 && get(0).time() < before) dropOldest();
  }

  public void add(
      TrailSample sample,
      double minDistance,
      double maxDistance,
      int maxSubdivisions,
      double discontinuity) {
    add(sample, minDistance, maxDistance, maxSubdivisions, discontinuity, false);
  }

  public void add(
      TrailSample sample,
      double minDistance,
      double maxDistance,
      int maxSubdivisions,
      double discontinuity,
      boolean curved) {
    if (!finite(sample.origin()) || !finite(sample.tip()) || !Double.isFinite(sample.time())) {
      clear();
      return;
    }
    var last = previous;
    if (last != null && sample.time() <= last.time()) return;
    if (last != null) {
      double distance =
          Math.max(last.tip().distanceTo(sample.tip()), last.origin().distanceTo(sample.origin()));
      if (distance > discontinuity || sample.time() - last.time() > 0.3) {
        clear();
        last = null;
      } else if (distance < minDistance
          && (!curved
              || size > 1
                  && Math.max(
                          get(size - 2).tip().distanceTo(sample.tip()),
                          get(size - 2).origin().distanceTo(sample.origin()))
                      < minDistance)) {
        // Keep the live endpoint current without spending the entire history on tiny movements.
        if (curved && size > 1) {
          samples[(start + size - 1) % samples.length] = sample;
          previous = sample;
        }
        return;
      } else {
        int count =
            Math.clamp(
                (int) Math.ceil(distance / Math.max(0.001, maxDistance)),
                1,
                Math.max(1, maxSubdivisions));
        for (int i = 1; i < count; i++) {
          double t = (double) i / count;
          append(
              curved
                  ? SweptTrail.interpolate(last, sample, t)
                  : new TrailSample(
                      last.origin().lerp(sample.origin(), t),
                      last.tip().lerp(sample.tip(), t),
                      last.time() + (sample.time() - last.time()) * t,
                      (float) (last.progress() + (sample.progress() - last.progress()) * t)),
              curved);
        }
      }
    }
    append(sample, curved);
    previous = sample;
  }

  private void append(TrailSample sample, boolean preserveStroke) {
    if (size == samples.length) {
      if (preserveStroke) simplify(sample);
      else dropOldest();
    }
    samples[(start + size) % samples.length] = sample;
    size++;
  }

  /**
   * Preserve the stroke's first pose and remove the least significant interior pose at capacity.
   */
  private void simplify(TrailSample incoming) {
    int remove = 1;
    double best = Double.POSITIVE_INFINITY;
    for (int i = 1; i < size; i++) {
      var a = get(i - 1);
      var b = get(i);
      var c = i + 1 < size ? get(i + 1) : incoming;
      double span = c.time() - a.time();
      double t = span <= 0 ? .5 : (b.time() - a.time()) / span;
      var expected = SweptTrail.interpolate(a, c, Math.clamp(t, 0, 1));
      double error =
          expected.origin().distanceToSqr(b.origin()) + expected.tip().distanceToSqr(b.tip());
      // Prefer discarding tightly packed poses when geometric errors are indistinguishable.
      double score = error + span * span * 1e-8;
      if (score < best) {
        best = score;
        remove = i;
      }
    }
    for (int i = remove; i + 1 < size; i++) samples[(start + i) % samples.length] = get(i + 1);
    samples[(start + size - 1) % samples.length] = null;
    size--;
  }

  public static boolean finite(Vec3 v) {
    return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
  }
}
