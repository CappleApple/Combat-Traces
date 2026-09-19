package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;

/** Geometry inferred from consecutive rendered weapon spans, independent of animation names. */
public final class SweptTrail {
  private SweptTrail() {}

  /** Interpolate rotation without shortening the blade at low frame rates. */
  public static TrailSample interpolate(TrailSample a, TrailSample b, double t) {
    Vec3 av = a.tip().subtract(a.origin()), bv = b.tip().subtract(b.origin());
    double al = av.length(), bl = bv.length();
    Vec3 origin = a.origin().lerp(b.origin(), t);
    if (al < 1e-6 || bl < 1e-6)
      return new TrailSample(
          origin,
          a.tip().lerp(b.tip(), t),
          lerp(a.time(), b.time(), t),
          (float) lerp(a.progress(), b.progress(), t));
    Vec3 an = av.scale(1 / al), bn = bv.scale(1 / bl);
    double dot = Math.clamp(an.dot(bn), -1, 1);
    Vec3 direction;
    if (dot > .9995) direction = an.lerp(bn, t).normalize();
    else if (dot < -.98) {
      // A near half-turn between frames has no trustworthy rotation plane.
      return new TrailSample(
          origin,
          a.tip().lerp(b.tip(), t),
          lerp(a.time(), b.time(), t),
          (float) lerp(a.progress(), b.progress(), t));
    } else {
      double angle = Math.acos(dot), sin = Math.sin(angle);
      direction =
          an.scale(Math.sin((1 - t) * angle) / sin).add(bn.scale(Math.sin(t * angle) / sin));
    }
    return new TrailSample(
        origin,
        origin.add(direction.scale(lerp(al, bl, t))),
        lerp(a.time(), b.time(), t),
        (float) lerp(a.progress(), b.progress(), t));
  }

  /** Stepped silhouette: pointed ends, broad middle, no smooth alpha gradient across the blade. */
  public static double envelope(double position) {
    if (position <= 0 || position >= 1) return 0;
    return Math.ceil(Math.pow(Math.sin(Math.PI * position), .65) * 12) / 12;
  }

  public static Vec3 bladePoint(TrailSample sample, double width, double across) {
    return sample.tip().lerp(sample.origin(), Math.clamp(width * across, 0, 1.5));
  }

  public static Vec3 headPoint(TrailSample sample, double width, double across) {
    return sample.origin().lerp(sample.tip(), .5 + (across - .5) * width);
  }

  /** Stable perpendicular axes for a crossed thrust wake, including straight-up attacks. */
  public static Vec3 perpendicular(Vec3 axis) {
    Vec3 unit = axis.normalize();
    return unit.cross(Math.abs(unit.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize();
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }
}
