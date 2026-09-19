package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.CombatMotion;
import com.cappleapple.combattraces.api.SwingWindow;
import com.cappleapple.combattraces.data.AnimationRule;

public final class TrailActivation {
  private TrailActivation() {}

  public static boolean active(
      CombatMotion motion, AnimationRule window, double speed, double minimum) {
    if (!Double.isFinite(speed)
        || !Double.isFinite(minimum)
        || speed < Math.max(.01, minimum)
        || !Float.isFinite(motion.progress())
        || motion.progress() < 0
        || motion.progress() >= 1) return false;
    var swing = window(motion, window);
    return swing != null && swing.contains(motion.progress());
  }

  public static SwingWindow window(CombatMotion motion, AnimationRule rule) {
    if (rule.hasWindow()) {
      float start = rule.start(), end = rule.end();
      if (!Float.isFinite(start) || !Float.isFinite(end) || start < 0 || end > 1 || start >= end)
        return null;
      return new SwingWindow(start, end);
    }
    return motion.swingWindow() == null
        ? SwingWindow.around(motion.hitProgress())
        : motion.swingWindow();
  }
}
