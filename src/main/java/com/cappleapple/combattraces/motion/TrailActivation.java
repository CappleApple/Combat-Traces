package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.CombatMotion;
import com.cappleapple.combattraces.data.AnimationRule;

public final class TrailActivation {
  private TrailActivation() {}

  public static boolean active(
      CombatMotion motion, AnimationRule window, double speed, double minimum) {
    if (!Double.isFinite(speed) || speed < Math.max(.01, minimum) || motion.progress() >= 1)
      return false;
    // Explicit windows can refine timing, but never bypass the user's speed requirement.
    return window.hasWindow()
        ? window.active(motion.progress())
        : motion.progress() >= Math.max(0, motion.hitProgress() - .12);
  }
}
