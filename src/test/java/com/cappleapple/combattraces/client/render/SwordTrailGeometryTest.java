package com.cappleapple.combattraces.client.render;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.WeaponClass;
import com.cappleapple.combattraces.client.trail.TrailInstance;
import com.cappleapple.combattraces.data.EffectStyle;
import com.cappleapple.combattraces.motion.TrailSample;
import com.cappleapple.combattraces.validation.TrailGeometryValidation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class SwordTrailGeometryTest {
  private TrailInstance trace(Vec3 camera, Vec3... tips) {
    var trail = new TrailInstance(null, 1, false, EffectStyle.trail(), 32);
    trail.family = WeaponClass.SLASH;
    for (int i = 0; i < tips.length; i++) {
      Vec3 origin = camera.add(.25 + i * .12, .8 + i * .06, .2);
      trail.history.add(
          new TrailSample(origin, tips[i].add(camera), 1 + i * .04, .5f), .001, 5, 1, 10, true);
    }
    return trail;
  }

  @Test
  void diagonalRiseKeepsItsVerticalAndHorizontalTravel() {
    var t = trace(Vec3.ZERO, new Vec3(-1, .7, 1), new Vec3(0, 1.7, 1.6), new Vec3(1, 2.8, 1.8));
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
    var v = TrailGeometryValidation.vertices(t, Vec3.ZERO, 1);
    assertEquals(2.1, v.get(15).y - v.getFirst().y, 1e-6);
  }

  @Test
  void overheadSlashIsNotTiltedOrMovedToAnUpperBodyCircle() {
    var t = trace(Vec3.ZERO, new Vec3(.4, 3, 0), new Vec3(.5, 2, 1.6), new Vec3(.6, .3, 2));
    t.family = WeaponClass.CLEAVE;
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
  }

  @Test
  void horizontalSlashStaysAtTheActualBladeHeight() {
    var t = trace(Vec3.ZERO, new Vec3(-1, .6, 1), new Vec3(0, .6, 2), new Vec3(1, .6, 1));
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
  }

  @Test
  void ChangingPlaneAndWristRotationArePreserved() {
    var t =
        trace(
            Vec3.ZERO,
            new Vec3(-1, .5, .3),
            new Vec3(-.7, 1.5, 1.8),
            new Vec3(.5, 2.4, 1.3),
            new Vec3(1.8, 1.4, .6),
            new Vec3(1, .3, -.8));
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
  }

  @Test
  void mirroredOffhandMotionIsNotReoriented() {
    var t = trace(Vec3.ZERO, new Vec3(1, .7, 1), new Vec3(0, 1.7, 1.6), new Vec3(-1, 2.8, 1.8));
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
  }

  @Test
  void cameraSubtractionPreservesWorldBorderPrecision() {
    var camera = new Vec3(29999000, 88, -29999000);
    var t = trace(camera, new Vec3(-1, .7, 1), new Vec3(0, 1.7, 1.6), new Vec3(1, 2.8, 1.8));
    assertTrue(TrailGeometryValidation.followsBlade(t, camera));
  }

  @Test
  void interpolatedLowFrameRateStripStillJoinsEachCapturedTip() {
    var t = trace(Vec3.ZERO, new Vec3(-1, 1, 1), new Vec3(0, 2, 1), new Vec3(1, 2.5, 1));
    t.history.add(
        new TrailSample(new Vec3(.8, 1, .2), new Vec3(2, 1, 1), 1.15, .6f), .001, .08, 6, 10, true);
    assertTrue(t.history.size() > 4);
    assertTrue(TrailGeometryValidation.followsBlade(t, Vec3.ZERO));
  }

  @Test
  void aShortStrikeRendersWithOnlyTwoCapturedPoses() {
    var t = trace(Vec3.ZERO, new Vec3(-1, 1, 1), new Vec3(1, 2, 1));
    assertEquals(12, TrailGeometryValidation.vertices(t, Vec3.ZERO, 1).size());
    t.family = WeaponClass.BLUNT;
    assertEquals(12, TrailGeometryValidation.vertices(t, Vec3.ZERO, 1).size());
    t.family = WeaponClass.PIERCE;
    t.thrust = true;
    assertEquals(24, TrailGeometryValidation.vertices(t, Vec3.ZERO, 1).size());
  }
}
