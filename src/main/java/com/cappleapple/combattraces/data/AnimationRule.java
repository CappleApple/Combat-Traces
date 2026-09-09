package com.cappleapple.combattraces.data;

public record AnimationRule(Float start, Float end, float sampleMultiplier) {
  public boolean hasWindow() {
    return start != null && end != null;
  }

  public boolean active(float progress) {
    return !hasWindow() || progress >= start && progress <= end;
  }

  public static AnimationRule automatic() {
    return new AnimationRule(null, null, 1);
  }
}
