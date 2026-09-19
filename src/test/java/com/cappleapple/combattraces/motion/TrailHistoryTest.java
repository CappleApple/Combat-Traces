package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class TrailHistoryTest {
  private TrailSample at(double x, double t) {
    return new TrailSample(new Vec3(x, 0, 0), new Vec3(x, 1, 0), t, 0.5f);
  }

  @Test
  void fastMotionIsInterpolatedButBurstIsCapped() {
    var h = new TrailHistory(32);
    h.add(at(0, 1), 0.01, 0.1, 4, 5);
    h.add(at(2, 1.02), 0.01, 0.1, 4, 5);
    assertEquals(5, h.size());
    assertEquals(2, h.get(4).tip().x);
    assertEquals(0.5, h.get(1).tip().x);
  }

  @Test
  void teleportStartsNewStrip() {
    var h = new TrailHistory(24);
    h.add(at(0, 1), 0.01, 0.1, 6, 5);
    h.add(at(20, 1.1), 0.01, 0.1, 6, 5);
    assertEquals(1, h.size());
    assertEquals(20, h.get(0).tip().x);
  }

  @Test
  void lowFpsDoesNotBridgeAcrossLongGap() {
    var h = new TrailHistory(24);
    h.add(at(0, 1), 0.01, 0.1, 6, 5);
    h.add(at(1, 2), 0.01, 0.1, 6, 5);
    assertEquals(1, h.size());
  }

  @Test
  void capAndAgeEvictionAreIndependent() {
    var h = new TrailHistory(4);
    for (int i = 0; i < 20; i++) h.add(at(i * 0.1, 1 + i * 0.02), 0.01, 0.1, 2, 5);
    assertEquals(4, h.size());
    h.prune(1.37);
    assertEquals(1, h.size());
    h.prune(2);
    assertEquals(0, h.size());
  }

  @Test
  void stationaryAndDuplicateFrameDoNotAddSamples() {
    var h = new TrailHistory(24);
    h.add(at(0, 1), 0.01, 0.1, 6, 5);
    h.add(at(0.001, 1.01), 0.01, 0.1, 6, 5);
    h.add(at(1, 1), 0.01, 0.1, 6, 5);
    assertEquals(1, h.size());
  }

  @Test
  void invalidCoordinatesClearHistory() {
    var h = new TrailHistory(24);
    h.add(at(0, 1), 0.01, 0.1, 6, 5);
    h.add(at(Double.NaN, 1.1), 0.01, 0.1, 6, 5);
    assertEquals(0, h.size());
  }

  @Test
  void thrustIsInferredFromBladeAxis() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 1), 1, 0.2f);
    var b = new TrailSample(new Vec3(0, 0, 0.2), new Vec3(0, 0, 1.2), 1.02, 0.3f);
    assertEquals(MotionAnalysis.Type.THRUST, MotionAnalysis.between(a, b, Vec3.ZERO).type());
    assertEquals(10, MotionAnalysis.between(a, b, Vec3.ZERO).speed(), 1e-6);
  }

  @Test
  void upwardAndDownwardSweepsKeepTheirSign() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(0, 0, 1), 1, 0);
    assertEquals(
        MotionAnalysis.Type.DIAGONAL_UP,
        MotionAnalysis.between(
                a, new TrailSample(Vec3.ZERO, new Vec3(0.3, 0.3, 1), 1.05, 0), Vec3.ZERO)
            .type());
    assertEquals(
        MotionAnalysis.Type.DIAGONAL_DOWN,
        MotionAnalysis.between(
                a, new TrailSample(Vec3.ZERO, new Vec3(0.3, -0.3, 1), 1.05, 0), Vec3.ZERO)
            .type());
  }

  @Test
  void generatedStrokeKeepsItsStartAndEndAtCapacity() {
    var h = new TrailHistory(8);
    for (int i = 0; i <= 100; i++) {
      double angle = i * Math.PI / 100;
      h.add(
          new TrailSample(
              Vec3.ZERO, new Vec3(Math.cos(angle), Math.sin(angle), 0), 1 + i * .01, i / 100f),
          .001,
          .2,
          3,
          5,
          true);
    }
    assertEquals(8, h.size());
    assertEquals(0, h.get(0).progress());
    assertEquals(1, h.get(h.size() - 1).progress());
    for (int i = 1; i < h.size(); i++) assertTrue(h.get(i).time() > h.get(i - 1).time());
    assertTrue(java.util.stream.IntStream.range(0, h.size()).anyMatch(i -> h.get(i).tip().y > .9));
  }

  @Test
  void smallGeneratedMovementsAccumulateAndKeepTheFinalPose() {
    var h = new TrailHistory(24);
    for (int i = 0; i <= 100; i++) h.add(at(i * .002, 1 + i * .01), .02, .2, 3, 5, true);
    assertTrue(h.size() > 5);
    assertEquals(0, h.get(0).tip().x);
    assertEquals(.2, h.get(h.size() - 1).tip().x, 1e-7);
    assertEquals(2, h.latest().time());
  }

  @Test
  void twoSmallPosesAreEnoughToKeepAShortStroke() {
    var h = new TrailHistory(24);
    h.add(at(0, 1), .1, .2, 3, 5, true);
    h.add(at(.01, 1.02), .1, .2, 3, 5, true);
    assertEquals(2, h.size());
  }
}
