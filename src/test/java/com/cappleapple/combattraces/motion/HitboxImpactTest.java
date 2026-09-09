package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.*;
import java.util.HashSet;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HitboxImpactTest {
  private static final Vec3 X = new Vec3(1, 0, 0), Y = new Vec3(0, 1, 0), Z = new Vec3(0, 0, 1);

  private static AttackHitbox box(double width, double height, double depth) {
    return new AttackHitbox(Vec3.ZERO, new Vec3(width, height, depth), X, Y, Z);
  }

  @Test
  void deepestVolumeIsAStabEvenWithStationaryOrContradictoryMotion() {
    var volume = box(2, 2, 4);
    for (var fallback :
        new MotionAnalysis.Type[] {
          MotionAnalysis.Type.UNKNOWN,
          MotionAnalysis.Type.HORIZONTAL,
          MotionAnalysis.Type.VERTICAL_DOWN
        }) {
      var hit = HitboxImpact.resolve(WeaponClass.SLASH, volume, fallback, X);
      assertEquals(MotionAnalysis.Type.THRUST, hit.type());
      assertEquals(Z, hit.tangent());
      assertEquals(WeaponClass.PIERCE, HitboxImpact.family(WeaponClass.SLASH, volume, fallback));
    }
  }

  @Test
  void wideAndTallVolumesUseTheirOwnAxesAndOverrideAThrustFallback() {
    var wide = box(8, 1, 4);
    var tall = box(1, 8, 4);
    assertEquals(
        MotionAnalysis.Type.HORIZONTAL,
        HitboxImpact.resolve(WeaponClass.PIERCE, wide, MotionAnalysis.Type.THRUST, Z).type());
    assertEquals(
        X, HitboxImpact.resolve(WeaponClass.PIERCE, wide, MotionAnalysis.Type.THRUST, Z).tangent());
    assertEquals(
        WeaponClass.SLASH,
        HitboxImpact.family(WeaponClass.PIERCE, wide, MotionAnalysis.Type.THRUST));
    assertEquals(
        MotionAnalysis.Type.VERTICAL_DOWN,
        HitboxImpact.resolve(WeaponClass.CLEAVE, tall, MotionAnalysis.Type.THRUST, Z).type());
    assertEquals(
        Y, HitboxImpact.resolve(WeaponClass.CLEAVE, tall, MotionAnalysis.Type.THRUST, Z).tangent());
    assertEquals(
        WeaponClass.CLEAVE,
        HitboxImpact.family(WeaponClass.CLEAVE, tall, MotionAnalysis.Type.THRUST));
  }

  @Test
  void spinAndEqualDimensionTiesRemainSlashes() {
    assertEquals(HitboxImpact.Kind.HORIZONTAL, HitboxImpact.kind(box(8, 1, 8)));
    assertEquals(HitboxImpact.Kind.VERTICAL, HitboxImpact.kind(box(1, 8, 8)));
    assertEquals(HitboxImpact.Kind.HORIZONTAL, HitboxImpact.kind(box(4, 4, 4)));
  }

  @Test
  void turningTheVolumeDoesNotChangeWidthIntoDepth() {
    var volume = new AttackHitbox(Vec3.ZERO, new Vec3(8, 1, 4), Z, Y, X.scale(-1));
    assertEquals(HitboxImpact.Kind.HORIZONTAL, HitboxImpact.kind(volume));
    assertEquals(
        Z,
        HitboxImpact.resolve(WeaponClass.SLASH, volume, MotionAnalysis.Type.THRUST, X).tangent());
  }

  @Test
  void bluntVolumesNeverBecomeStabsOrSlashesAndReceiveNoAngleVariation() {
    var basis = ImpactMath.orient(X, Z, Z, 1);
    for (var volume : new AttackHitbox[] {box(1, 1, 4), box(8, 1, 4), box(1, 8, 4)}) {
      var hit = HitboxImpact.resolve(WeaponClass.BLUNT, volume, MotionAnalysis.Type.VERTICAL_UP, Y);
      assertEquals(MotionAnalysis.Type.VERTICAL_UP, hit.type());
      assertEquals(Y, hit.tangent());
      assertEquals(
          WeaponClass.BLUNT,
          HitboxImpact.family(WeaponClass.BLUNT, volume, MotionAnalysis.Type.THRUST));
      assertSame(basis, HitboxImpact.vary(basis, WeaponClass.BLUNT, volume, 42));
    }
  }

  @Test
  void missingOrInvalidGeometryPreservesTheFallback() {
    for (var volume :
        new AttackHitbox[] {
          null,
          box(0, 2, 4),
          box(Double.NaN, 2, 4),
          new AttackHitbox(Vec3.ZERO, new Vec3(1, 2, 4), Vec3.ZERO, Y, Z)
        }) {
      assertEquals(HitboxImpact.Kind.UNKNOWN, HitboxImpact.kind(volume));
      var hit = HitboxImpact.resolve(WeaponClass.SLASH, volume, MotionAnalysis.Type.THRUST, Z);
      assertEquals(MotionAnalysis.Type.THRUST, hit.type());
      assertEquals(Z, hit.tangent());
      assertEquals(WeaponClass.PIERCE, HitboxImpact.family(WeaponClass.SLASH, volume, hit.type()));
    }
  }

  @Test
  void variationIsStableBoundedAndOrthonormalInTheSpritePlane() {
    var basis = ImpactMath.orient(X, new Vec3(.9, .3, .2), new Vec3(.9, .3, .2), 1);
    var angles = new HashSet<Double>();
    for (long seed = 0; seed < 200; seed++) {
      double angle = HitboxImpact.variationDegrees(seed);
      assertTrue(Math.abs(angle) <= 7);
      assertEquals(angle, HitboxImpact.variationDegrees(seed));
      angles.add(angle);
      var rotated = HitboxImpact.vary(basis, WeaponClass.SLASH, box(8, 1, 4), seed);
      assertTrue(rotated.tangent().dot(basis.tangent()) >= Math.cos(Math.toRadians(7)) - 1e-8);
      assertEquals(1, rotated.tangent().length(), 1e-8);
      assertEquals(0, rotated.tangent().dot(rotated.bitangent()), 1e-8);
      assertEquals(basis.normal(), rotated.normal());
    }
    assertTrue(angles.size() > 190);
    assertSame(basis, HitboxImpact.vary(basis, WeaponClass.SLASH, box(1, 1, 4), 42));
  }
}
