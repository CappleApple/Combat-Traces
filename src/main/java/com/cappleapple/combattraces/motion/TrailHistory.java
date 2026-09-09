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
      } else if (distance < minDistance) return;
      else {
        int count =
            Math.clamp(
                (int) Math.ceil(distance / Math.max(0.001, maxDistance)),
                1,
                Math.max(1, maxSubdivisions));
        for (int i = 1; i < count; i++) {
          double t = (double) i / count;
          append(
              new TrailSample(
                  last.origin().lerp(sample.origin(), t),
                  last.tip().lerp(sample.tip(), t),
                  last.time() + (sample.time() - last.time()) * t,
                  (float) (last.progress() + (sample.progress() - last.progress()) * t)));
        }
      }
    }
    append(sample);
    previous = sample;
  }

  private void append(TrailSample sample) {
    if (size == samples.length) dropOldest();
    samples[(start + size) % samples.length] = sample;
    size++;
  }

  public static boolean finite(Vec3 v) {
    return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
  }
}
