package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.WeaponClass;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MultiEmitterGeometryTest {
  @Test
  void twinbladeHasSeparateOutwardBladesAndLeavesGripClear() {
    var shape =
        EmitterAnalysis.analyze(staff(true), WeaponClass.SLASH, WeaponTopology.DOUBLE_BLADE)
            .orElseThrow();
    assertEquals(2, shape.emitters().size());
    var top = shape.emitters().get(0);
    var bottom = shape.emitters().get(1);
    assertTrue(top.origin().y > .12 && top.tip().y > .9);
    assertTrue(bottom.origin().y < -.12 && bottom.tip().y < -.9);
    assertTrue(top.tip().y > top.origin().y);
    assertTrue(bottom.tip().y < bottom.origin().y);
    assertTrue(top.tip().distanceTo(bottom.tip()) > 1.8);
    assertEquals(shape.emitter(), top);
  }

  @Test
  void quarterstaffCapsCrossEachTerminalAndDoNotFillTheShaft() {
    var shape =
        EmitterAnalysis.analyze(staff(false), WeaponClass.BLUNT, WeaponTopology.DOUBLE_BLUNT)
            .orElseThrow();
    assertEquals(2, shape.emitters().size());
    for (var emitter : shape.emitters()) {
      assertEquals(emitter.origin().y, emitter.tip().y, .01);
      assertTrue(Math.abs(emitter.tip().y) > .75);
      assertTrue(emitter.origin().distanceTo(emitter.tip()) >= .075);
      assertTrue(emitter.origin().distanceTo(emitter.tip()) < .3);
    }
    assertTrue(shape.emitters().get(0).tip().y > 0);
    assertTrue(shape.emitters().get(1).tip().y < 0);
  }

  @Test
  void tiltedDoubleBladeRetainsBothEndsInItsModelPlane() {
    var points = staff(true).stream().map(MultiEmitterGeometryTest::tilt).toList();
    var shape =
        EmitterAnalysis.analyze(points, WeaponClass.SLASH, WeaponTopology.DOUBLE_BLADE)
            .orElseThrow();
    Vec3 axis = tilt(new Vec3(0, 1, 0));
    assertEquals(2, shape.emitters().size());
    assertTrue(shape.emitters().get(0).tip().dot(axis) > .9);
    assertTrue(shape.emitters().get(1).tip().dot(axis) < -.9);
    for (var emitter : shape.emitters())
      assertTrue(Math.abs(emitter.tip().dot(tilt(new Vec3(0, 0, 1)))) < .04);
  }

  @Test
  void chakramRimClosesAroundItsHoleWithoutRadialSpokes() {
    var shape =
        EmitterAnalysis.analyze(ring(false), WeaponClass.SLASH, WeaponTopology.CIRCULAR)
            .orElseThrow();
    assertEquals(8, shape.emitters().size());
    for (int i = 0; i < shape.emitters().size(); i++) {
      var edge = shape.emitters().get(i);
      assertEquals(edge.origin(), shape.emitters().get((i + 1) % shape.emitters().size()).tip());
      assertEquals(1, edge.tip().length(), .02);
      assertTrue(edge.origin().lerp(edge.tip(), .5).length() > .9);
      assertTrue(edge.origin().distanceTo(edge.tip()) > .5);
    }
  }

  @Test
  void tiltedChakramRimFitsActualModelPlaneAndRemainsBounded() {
    var points = ring(true).stream().map(MultiEmitterGeometryTest::tilt).toList();
    var shape =
        EmitterAnalysis.analyze(points, WeaponClass.SLASH, WeaponTopology.CIRCULAR).orElseThrow();
    Vec3 normal = tilt(new Vec3(0, 0, 1));
    assertEquals(MultiEmitterGeometry.RIM_SEGMENTS, shape.emitters().size());
    for (var emitter : shape.emitters()) {
      assertEquals(0, emitter.tip().dot(normal), .01);
      assertTrue(emitter.tip().length() > .95 && emitter.tip().length() < 1.1);
    }
  }

  @Test
  void CircularHintDoesNotTurnThinOrThickUnknownGeometryIntoARing() {
    assertTrue(
        EmitterAnalysis.analyze(staff(false), WeaponClass.SLASH, WeaponTopology.CIRCULAR)
            .isEmpty());
    var cube = new ArrayList<Vec3>();
    for (int x : new int[] {-1, 1})
      for (int y : new int[] {-1, 1}) for (int z : new int[] {-1, 1}) cube.add(new Vec3(x, y, z));
    assertTrue(EmitterAnalysis.analyze(cube, WeaponClass.SLASH, WeaponTopology.CIRCULAR).isEmpty());
  }

  @Test
  void ordinaryBladeAnalysisStillProducesOneEmitter() {
    var points = staff(true).stream().filter(p -> p.y >= 0).toList();
    assertEquals(
        1,
        EmitterAnalysis.analyze(points, WeaponClass.SLASH, WeaponTopology.SINGLE)
            .orElseThrow()
            .emitters()
            .size());
  }

  @Test
  void invalidModelsReturnNoSpecialEmitters() {
    for (var type :
        List.of(
            WeaponTopology.DOUBLE_BLADE, WeaponTopology.DOUBLE_BLUNT, WeaponTopology.CIRCULAR)) {
      assertTrue(EmitterAnalysis.analyze(List.of(), WeaponClass.SLASH, type).isEmpty());
      assertTrue(
          EmitterAnalysis.analyze(
                  List.of(new Vec3(Double.NaN, 0, 0), Vec3.ZERO), WeaponClass.SLASH, type)
              .isEmpty());
    }
  }

  private static List<Vec3> staff(boolean blades) {
    var points = new ArrayList<Vec3>();
    for (int i = -100; i <= 100; i++) {
      double y = i / 100d;
      double width = !blades ? .04 : Math.abs(y) < .2 ? .03 : Math.abs(y) < .3 ? .18 : .07;
      for (int x : new int[] {-1, 1})
        for (int z : new int[] {-1, 1}) points.add(new Vec3(x * width, y, z * .02));
    }
    return points;
  }

  private static List<Vec3> ring(boolean spiked) {
    var points = new ArrayList<Vec3>();
    for (int i = 0; i < 96; i++)
      for (double radius : new double[] {.6, 1})
        points.add(
            new Vec3(radius * Math.cos(i * Math.PI / 48), radius * Math.sin(i * Math.PI / 48), 0));
    if (spiked)
      for (int i = 0; i < 8; i++)
        points.add(new Vec3(1.08 * Math.cos(i * Math.PI / 4), 1.08 * Math.sin(i * Math.PI / 4), 0));
    return points;
  }

  private static Vec3 tilt(Vec3 v) {
    return new Vec3(
        (v.x + v.y) / Math.sqrt(2),
        (v.y - v.x) / 2 - v.z / Math.sqrt(2),
        (v.y - v.x) / 2 + v.z / Math.sqrt(2));
  }
}
