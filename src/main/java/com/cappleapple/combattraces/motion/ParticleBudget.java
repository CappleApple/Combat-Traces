package com.cappleapple.combattraces.motion;

/** Strict per-frame and rolling-one-second bounds, with fixed storage. */
public final class ParticleBudget {
  private final double[] times = new double[2000];
  private int start, size, frameRemaining, secondLimit;
  private double now;

  public void beginFrame(double time, int perSecond, int perFrame) {
    now = time;
    secondLimit = Math.clamp(perSecond, 0, times.length);
    frameRemaining = Math.max(0, perFrame);
    while (size > 0 && now - times[start] >= 1) {
      start = (start + 1) % times.length;
      size--;
    }
  }

  public int take(int requested) {
    int count = Math.max(0, Math.min(requested, Math.min(frameRemaining, secondLimit - size)));
    for (int i = 0; i < count; i++) times[(start + size++) % times.length] = now;
    frameRemaining -= count;
    return count;
  }

  public void clear() {
    start = 0;
    size = 0;
    frameRemaining = 0;
  }
}
