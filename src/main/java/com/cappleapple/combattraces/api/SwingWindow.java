package com.cappleapple.combattraces.api;

/** Normalized animation interval occupied by the striking motion, excluding setup and recovery. */
public record SwingWindow(float start, float end) {
  public SwingWindow {
    if (!Float.isFinite(start) || !Float.isFinite(end) || start < 0 || end > 1 || start >= end)
      throw new IllegalArgumentException("Swing window must satisfy 0 <= start < end <= 1");
  }

  public boolean contains(float progress) {
    return Float.isFinite(progress) && progress >= start && progress <= end;
  }

  /** Bounded estimate for providers without authored strike timing. Invalid hints fail closed. */
  public static SwingWindow around(float hit) {
    if (!Float.isFinite(hit) || hit < 0 || hit > 1) return null;
    return new SwingWindow(Math.max(0, hit - .12f), Math.min(1, hit + .12f));
  }
}
