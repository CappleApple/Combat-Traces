package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;

public final class MotionAnalysis {
  public enum Type {
    HORIZONTAL,
    VERTICAL_DOWN,
    VERTICAL_UP,
    DIAGONAL_DOWN,
    DIAGONAL_UP,
    THRUST,
    SPIN,
    UNKNOWN
  }

  public record State(
      Vec3 tipVelocity,
      Vec3 originVelocity,
      Vec3 tangent,
      Vec3 planeNormal,
      double speed,
      Type type) {
    public static State still() {
      return new State(Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 0, Type.UNKNOWN);
    }
  }

  private MotionAnalysis() {}

  public static State between(TrailSample before, TrailSample after, Vec3 bodyMovement) {
    return between(
        before, after, bodyMovement, com.cappleapple.combattraces.api.AttackShape.UNKNOWN);
  }

  public static State between(
      TrailSample before,
      TrailSample after,
      Vec3 bodyMovement,
      com.cappleapple.combattraces.api.AttackShape shape) {
    if (before == null || after == null) return State.still();
    double dt = after.time() - before.time();
    if (dt < 1e-5 || dt > 0.3) return State.still();
    var tip = after.tip().subtract(before.tip()).subtract(bodyMovement).scale(1 / dt);
    var origin = after.origin().subtract(before.origin()).subtract(bodyMovement).scale(1 / dt);
    var tangent = tip.normalize();
    var blade = after.tip().subtract(after.origin()).normalize();
    double speed = (tip.length() + origin.length()) * 0.5;
    Type type = Type.UNKNOWN;
    if (speed > 0.01) {
      double thrust = tangent.dot(blade);
      boolean forward = shape == com.cappleapple.combattraces.api.AttackShape.FORWARD;
      double threshold = forward ? .50 : .88;
      // A thrust translates BOTH ends along the blade. A retraction or rotating slam does
      // not.
      if (thrust > threshold
          && origin.normalize().dot(blade) > (forward ? .30 : .55)
          && tip.subtract(origin).length() < speed * (forward ? 1.25 : .9)) type = Type.THRUST;
      else if (Math.abs(tangent.y) < 0.28) type = Type.HORIZONTAL;
      else if (Math.abs(tangent.y) > 0.85)
        type = tangent.y > 0 ? Type.VERTICAL_UP : Type.VERTICAL_DOWN;
      else type = tangent.y > 0 ? Type.DIAGONAL_UP : Type.DIAGONAL_DOWN;
    }
    return new State(
        tip, origin, tangent, blade.cross(tangent).normalize(), Math.min(speed, 100), type);
  }

  public static boolean spinning(TrailHistory history) {
    double angular = 0;
    for (int i = 1; i < history.size(); i++) {
      var a = history.get(i - 1);
      var b = history.get(i);
      angular +=
          Math.acos(
              Math.clamp(
                  a.tip()
                      .subtract(a.origin())
                      .normalize()
                      .dot(b.tip().subtract(b.origin()).normalize()),
                  -1,
                  1));
    }
    return angular > Math.PI * 1.4;
  }
}
