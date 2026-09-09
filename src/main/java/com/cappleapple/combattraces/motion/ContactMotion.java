package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.*;
import net.minecraft.world.phys.Vec3;

/** Retains the incoming contact gesture through recovery and ordinary hit-packet latency. */
public final class ContactMotion {
  private long attack = Long.MIN_VALUE;
  private boolean thrust;
  private double bestScore;
  private Vec3 tangent = Vec3.ZERO;
  private Vec3 center;
  private AttackHitbox hitbox;

  public MotionAnalysis.Type observe(CombatMotion motion, MotionAnalysis.State state) {
    if (attack != motion.attackId()) {
      attack = motion.attackId();
      thrust = false;
      bestScore = 0;
      tangent = Vec3.ZERO;
      center = null;
      hitbox = null;
    }
    double phase = motion.progress() - motion.hitProgress();
    boolean forward = motion.shape() == AttackShape.FORWARD;
    if (motion.shape() != AttackShape.SWEEP
        && state.type() == MotionAnalysis.Type.THRUST
        && state.speed() > (forward ? .55 : 1)
        && Math.abs(phase) <= (forward ? .3 : .25)) thrust = true;
    // Prefer decisive motion near contact, then hold it instead of following recovery.
    // Speed weighting prevents a nearly stationary windup sample from fixing the cut's angle.
    if (phase >= -.12
        && phase <= .20
        && state.speed() > .05
        && TrailHistory.finite(state.tangent())
        && state.tangent().lengthSqr() > .5) {
      double score = state.speed() / (1 + 64 * phase * phase);
      if (score > bestScore) {
        bestScore = score;
        tangent = state.tangent().normalize();
        center = motion.hitboxCenter();
        hitbox = motion.hitbox();
      }
    }
    return thrust
        ? MotionAnalysis.Type.THRUST
        : state.type() == MotionAnalysis.Type.THRUST && motion.shape() == AttackShape.SWEEP
            ? MotionAnalysis.Type.UNKNOWN
            : state.type();
  }

  public Vec3 tangent(MotionAnalysis.State fallback) {
    return tangent.lengthSqr() > .5 ? tangent : fallback.tangent();
  }

  public AttackHitbox hitbox(CombatMotion fallback) {
    return hitbox == null ? fallback.hitbox() : hitbox;
  }

  public Vec3 center(CombatMotion fallback) {
    return center == null ? fallback.hitboxCenter() : center;
  }
}
