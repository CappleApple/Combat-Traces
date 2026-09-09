package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.*;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class WeaponContactTest {
  @Test
  void hammerRibbonSpansTheHeadInsteadOfTheShaft() {
    var points = new ArrayList<Vec3>();
    box(points, -.04, 0, -.04, .04, 1.7, .04);
    box(points, -.45, 1.6, -.16, .45, 2, .16);
    var e = EmitterAnalysis.analyze(points, WeaponClass.BLUNT).orElseThrow().emitter();
    assertTrue(e.origin().y > 1.55 && e.tip().y > 1.55, e.toString());
    assertEquals(.9, e.origin().distanceTo(e.tip()), .12);
    assertTrue(Math.abs(e.origin().x - e.tip().x) > .75);
  }

  @Test
  void swordBladeStartsAboveGuardAndReachesItsActualTip() {
    var points = new ArrayList<Vec3>();
    box(points, -.035, 0, -.02, .035, .4, .02);
    box(points, -.25, .4, -.035, .25, .47, .035);
    box(points, -.055, .47, -.02, .055, 2, .02);
    var e = EmitterAnalysis.analyze(points, WeaponClass.SLASH).orElseThrow().emitter();
    assertEquals(.47, e.origin().y, .065);
    assertEquals(2, e.tip().y, .025);
    assertTrue(e.tip().distanceTo(e.origin()) > 1.45);
  }

  @Test
  void forwardBladeTranslationIsAThrust() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 2), 1, .45f);
    var b = new TrailSample(new Vec3(0, 0, .2), new Vec3(0, 0, 2.2), 1.05, .5f);
    assertEquals(MotionAnalysis.Type.THRUST, MotionAnalysis.between(a, b, Vec3.ZERO).type());
    assertEquals(
        WeaponClass.PIERCE, ImpactMath.impactFamily(WeaponClass.SLASH, MotionAnalysis.Type.THRUST));
  }

  @Test
  void pullingTheSwordBackIsNotAStab() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 2), 1, .45f);
    var b = new TrailSample(new Vec3(0, 0, -.2), new Vec3(0, 0, 1.8), 1.05, .5f);
    assertNotEquals(
        MotionAnalysis.Type.THRUST,
        MotionAnalysis.between(a, b, Vec3.ZERO, AttackShape.FORWARD).type());
  }

  @Test
  void forwardHitboxDoesNotTurnARotatingSlamIntoAThrust() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 1, 1), 1, .45f);
    var b = new TrailSample(new Vec3(0, -.03, 0), new Vec3(0, .4, 1.35), 1.05, .5f);
    assertNotEquals(
        MotionAnalysis.Type.THRUST,
        MotionAnalysis.between(a, b, Vec3.ZERO, AttackShape.FORWARD).type());
    assertEquals(
        WeaponClass.BLUNT, ImpactMath.impactFamily(WeaponClass.BLUNT, MotionAnalysis.Type.THRUST));
  }

  @Test
  void shortAngledForwardTranslationIsNowDetectedAsAStab() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 2), 1, .45f);
    var b = new TrailSample(new Vec3(.07, 0, .045), new Vec3(.075, 0, 2.05), 1.1, .5f);
    assertEquals(
        MotionAnalysis.Type.THRUST,
        MotionAnalysis.between(a, b, Vec3.ZERO, AttackShape.FORWARD).type());
    assertNotEquals(
        MotionAnalysis.Type.THRUST,
        MotionAnalysis.between(a, b, Vec3.ZERO, AttackShape.UNKNOWN).type());
  }

  @Test
  void sidewaysSweepWithForwardHitboxIsNotAStab() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 2), 1, .45f);
    var b = new TrailSample(new Vec3(.2, 0, .005), new Vec3(.2, 0, 2.005), 1.05, .5f);
    assertNotEquals(
        MotionAnalysis.Type.THRUST,
        MotionAnalysis.between(a, b, Vec3.ZERO, AttackShape.FORWARD).type());
  }

  private static void box(
      List<Vec3> out, double x0, double y0, double z0, double x1, double y1, double z1) {
    for (int i = 0; i <= 100; i++) {
      double t = i / 100d;
      for (double x : new double[] {x0, x1})
        for (double z : new double[] {z0, z1}) out.add(new Vec3(x, y0 + (y1 - y0) * t, z));
      for (double y : new double[] {y0, y1})
        for (double z : new double[] {z0, z1}) out.add(new Vec3(x0 + (x1 - x0) * t, y, z));
      for (double x : new double[] {x0, x1})
        for (double y : new double[] {y0, y1}) out.add(new Vec3(x, y, z0 + (z1 - z0) * t));
    }
  }
}
