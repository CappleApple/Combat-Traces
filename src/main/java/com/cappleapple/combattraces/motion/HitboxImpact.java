package com.cappleapple.combattraces.motion;

import com.cappleapple.combattraces.api.*;
import net.minecraft.world.phys.Vec3;

/** Impact classification uses local volume proportions, never its rotated world AABB. */
public final class HitboxImpact {
  public enum Kind {
    UNKNOWN,
    STAB,
    HORIZONTAL,
    VERTICAL
  }

  public record Gesture(MotionAnalysis.Type type, Vec3 tangent) {}

  public static final double MAX_VARIATION_DEGREES = 7;

  private HitboxImpact() {}

  public static Kind kind(AttackHitbox box) {
    if (box == null
        || !finite(box.size())
        || !finite(box.center())
        || box.size().x <= 0
        || box.size().y <= 0
        || box.size().z <= 0
        || !axis(box.widthAxis())
        || !axis(box.heightAxis())
        || !axis(box.depthAxis())) return Kind.UNKNOWN;
    var size = box.size();
    if (size.z > size.x && size.z > size.y) return Kind.STAB;
    // Tied depth/width or depth/height occurs in spin boxes and is not a stab.
    return size.x >= size.y ? Kind.HORIZONTAL : Kind.VERTICAL;
  }

  public static Gesture resolve(
      WeaponClass family,
      AttackHitbox box,
      MotionAnalysis.Type fallbackType,
      Vec3 fallbackTangent) {
    if (family == WeaponClass.BLUNT) return new Gesture(fallbackType, fallbackTangent);
    return switch (kind(box)) {
      case STAB -> new Gesture(MotionAnalysis.Type.THRUST, box.depthAxis().normalize());
      case HORIZONTAL -> new Gesture(MotionAnalysis.Type.HORIZONTAL, box.widthAxis().normalize());
      case VERTICAL -> new Gesture(MotionAnalysis.Type.VERTICAL_DOWN, box.heightAxis().normalize());
      case UNKNOWN -> new Gesture(fallbackType, fallbackTangent);
    };
  }

  public static WeaponClass family(
      WeaponClass weapon, AttackHitbox box, MotionAnalysis.Type fallbackType) {
    if (weapon == WeaponClass.BLUNT) return WeaponClass.BLUNT;
    return switch (kind(box)) {
      case STAB -> WeaponClass.PIERCE;
      case HORIZONTAL, VERTICAL ->
          weapon == WeaponClass.CLEAVE ? WeaponClass.CLEAVE : WeaponClass.SLASH;
      case UNKNOWN -> ImpactMath.impactFamily(weapon, fallbackType);
    };
  }

  /** Rotate within the sprite plane, so foreshortening cannot amplify the small variation. */
  public static ImpactMath.Basis vary(
      ImpactMath.Basis basis, WeaponClass family, AttackHitbox box, long seed) {
    var kind = kind(box);
    if (family == WeaponClass.BLUNT || (kind != Kind.HORIZONTAL && kind != Kind.VERTICAL))
      return basis;
    double angle = Math.toRadians(variationDegrees(seed));
    double cos = Math.cos(angle), sin = Math.sin(angle);
    return new ImpactMath.Basis(
        basis.tangent().scale(cos).add(basis.bitangent().scale(sin)),
        basis.bitangent().scale(cos).subtract(basis.tangent().scale(sin)),
        basis.normal());
  }

  public static double variationDegrees(long seed) {
    // Stateless mixing: stable per hit and identical for all its composed layers.
    long mixed = seed + 0x9e3779b97f4a7c15L;
    mixed = (mixed ^ (mixed >>> 30)) * 0xbf58476d1ce4e5b9L;
    mixed = (mixed ^ (mixed >>> 27)) * 0x94d049bb133111ebL;
    mixed ^= mixed >>> 31;
    return ((mixed >>> 11) * 0x1.0p-53 * 2 - 1) * MAX_VARIATION_DEGREES;
  }

  private static boolean finite(Vec3 v) {
    return v != null && TrailHistory.finite(v);
  }

  private static boolean axis(Vec3 v) {
    return finite(v) && v.lengthSqr() > 1e-8;
  }
}
