package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;

public final class ImpactMath {
  public record Basis(Vec3 tangent, Vec3 bitangent, Vec3 normal) {}

  private ImpactMath() {}

  public static com.cappleapple.combattraces.api.WeaponClass impactFamily(
      com.cappleapple.combattraces.api.WeaponClass family, MotionAnalysis.Type motion) {
    return family != com.cappleapple.combattraces.api.WeaponClass.BLUNT
            && motion == MotionAnalysis.Type.THRUST
        ? com.cappleapple.combattraces.api.WeaponClass.PIERCE
        : family;
  }

  /** Only the height comes from the attack volume; horizontal contact stays approximate. */
  public static Vec3 atHitboxHeight(Vec3 contact, Vec3 hitboxCenter) {
    return hitboxCenter != null && TrailHistory.finite(hitboxCenter)
        ? new Vec3(contact.x, hitboxCenter.y, contact.z)
        : contact;
  }

  public static Basis orient(Vec3 tangent, Vec3 surfaceNormal, Vec3 toCamera, double bias) {
    Vec3 n = unit(surfaceNormal, new Vec3(0, 0, 1));
    Vec3 camera = unit(toCamera, n);
    // Equivalent two-sided normals must agree before blending, or opposing normals cancel out.
    if (n.dot(camera) < 0) n = n.scale(-1);
    n = unit(n.lerp(camera, Math.clamp(bias, 0, 1)), camera);
    Vec3 t = tangent.subtract(n.scale(tangent.dot(n)));
    if (t.lengthSqr() < 1e-8)
      t = (Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(n);
    t = unit(t, new Vec3(1, 0, 0));
    return new Basis(t, n.cross(t).normalize(), n);
  }

  public static Vec3 unit(Vec3 v, Vec3 fallback) {
    return TrailHistory.finite(v) && v.lengthSqr() > 1e-10 ? v.normalize() : fallback;
  }

  public static float strength(
      double damageMultiplier,
      double velocity,
      double baseDamage,
      double size,
      boolean critical,
      boolean finisher) {
    double score =
        0.4
            + Math.sqrt(Math.max(0, damageMultiplier)) * 0.22
            + Math.min(velocity, 30) * 0.016
            + Math.sqrt(Math.max(0, baseDamage)) * 0.065
            + Math.min(size, 3) * 0.08
            + (critical ? 0.18 : 0)
            + (finisher ? 0.08 : 0);
    return Double.isFinite(score) ? (float) Math.clamp(score, 0.5, 2) : 1;
  }
}
