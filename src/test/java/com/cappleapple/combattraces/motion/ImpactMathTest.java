package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ImpactMathTest {
  @Test
  void diagonalTangentSurvivesProjection() {
    var basis = ImpactMath.orient(new Vec3(1, -1, 0), new Vec3(0, 0, 1), new Vec3(0, 0, 5), 0.25);
    assertTrue(basis.tangent().x > 0);
    assertTrue(basis.tangent().y < 0);
    assertEquals(0, basis.tangent().dot(basis.normal()), 1e-8);
    assertEquals(1, basis.bitangent().length(), 1e-8);
  }

  @Test
  void oppositeNormalsAndParallelTangentStayFinite() {
    var basis = ImpactMath.orient(new Vec3(0, 0, 1), new Vec3(0, 0, 1), new Vec3(0, 0, -1), 0.5);
    assertEquals(1, basis.tangent().length(), 1e-8);
    assertEquals(1, basis.normal().length(), 1e-8);
  }

  @Test
  void extremeInputsCannotCreateHugeImpacts() {
    assertEquals(2, ImpactMath.strength(1e20, 1e20, 1e20, 1e20, true, true));
    assertEquals(1, ImpactMath.strength(Double.NaN, 1, 1, 1, false, false));
    assertTrue(ImpactMath.strength(0, 0, 0, 0, false, false) >= 0.5);
  }

  @Test
  void heightUsesAttackCenterWithoutMovingHorizontalContact() {
    var contact = new Vec3(2, 6, 8);
    assertEquals(new Vec3(2, 3, 8), ImpactMath.atHitboxHeight(contact, new Vec3(-99, 3, 100)));
    assertEquals(contact, ImpactMath.atHitboxHeight(contact, null));
    assertEquals(contact, ImpactMath.atHitboxHeight(contact, new Vec3(0, Double.NaN, 0)));
  }

  @Test
  void billboardKeepsHorizontalVerticalAndDiagonalIncomingAngles() {
    var camera = new Vec3(.3, .15, 1).normalize();
    for (var incoming : new Vec3[] {new Vec3(1, 0, 0), new Vec3(0, -1, 0), new Vec3(1, -1, .2)}) {
      var expected = incoming.subtract(camera.scale(incoming.dot(camera))).normalize();
      var basis = ImpactMath.orient(incoming, camera, camera, 1);
      assertEquals(1, expected.dot(basis.tangent()), 1e-8);
    }
  }
}
