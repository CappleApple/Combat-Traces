package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.weapon.WeaponResolver;
import com.cappleapple.combattraces.motion.MotionAnalysis;

public final class DebugState {
  public static CombatMotion motion;
  public static WeaponResolver.Resolved weapon;
  public static MotionAnalysis.State state;
  public static ImpactContext impact;

  public static void clear() {
    motion = null;
    weapon = null;
    state = null;
    impact = null;
  }
}
