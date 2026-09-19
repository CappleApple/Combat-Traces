package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class SweptTrailTest {
  @Test
  void rotationKeepsBladeLengthAndEndsOnActualWeapon() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(1, 0, 0), 1, .4f);
    var b = new TrailSample(new Vec3(0, .3, 0), new Vec3(0, 1.3, 0), 1.05, .5f);
    var mid = SweptTrail.interpolate(a, b, .5);
    assertEquals(1, mid.origin().distanceTo(mid.tip()), 1e-7);
    assertEquals(Math.sqrt(.5), mid.tip().x, 1e-7);
    assertEquals(b.tip(), SweptTrail.interpolate(a, b, 1).tip());
    assertEquals(a.origin(), SweptTrail.bladePoint(a, 1, 1));
    assertEquals(a.tip(), SweptTrail.bladePoint(a, 1, 0));
  }

  @Test
  void bothHammerEdgesStayOnTheHead() {
    var s = new TrailSample(new Vec3(-.4, 2, 1), new Vec3(.4, 2, 1), 1, .5f);
    assertEquals(s.origin(), SweptTrail.headPoint(s, 1, 0));
    assertEquals(s.tip(), SweptTrail.headPoint(s, 1, 1));
    assertEquals(2, SweptTrail.headPoint(s, .3, .25).y);
    assertEquals(1, SweptTrail.headPoint(s, .3, .25).z);
  }

  @Test
  void crescentIsPointedAtBothEndsWithPixelSteps() {
    assertEquals(0, SweptTrail.envelope(0));
    assertEquals(0, SweptTrail.envelope(1));
    assertEquals(1, SweptTrail.envelope(.5));
    for (int i = 1; i < 100; i++) {
      double v = SweptTrail.envelope(i / 100d);
      assertEquals(Math.rint(v * 12), v * 12, 1e-8);
      assertEquals(v, SweptTrail.envelope(1 - i / 100d), 1e-8);
    }
  }

  @Test
  void verticalAndAxialStabsHaveNonDegenerateCrossPlanes() {
    for (var axis :
        new Vec3[] {new Vec3(0, 1, 0), new Vec3(0, 0, 1), new Vec3(.7, .7, 0).normalize()}) {
      var u = SweptTrail.perpendicular(axis);
      assertEquals(1, u.length(), 1e-8);
      assertEquals(0, axis.dot(u), 1e-8);
      assertEquals(1, axis.cross(u).length(), 1e-8);
    }
  }

  @Test
  void interpolationStaysFiniteForDegenerateAndHalfTurnSamples() {
    var a = new TrailSample(Vec3.ZERO, new Vec3(1, 0, 0), 1, .5f);
    for (var tip : new Vec3[] {Vec3.ZERO, new Vec3(-1, 0, 0)}) {
      var b = new TrailSample(Vec3.ZERO, tip, 1.05, .6f);
      assertTrue(TrailHistory.finite(SweptTrail.interpolate(a, b, .5).tip()));
    }
  }

  @Test
  void curvedSubdivisionsRemainBoundedAtLowFrameRates() {
    var h = new TrailHistory(16);
    h.add(new TrailSample(Vec3.ZERO, new Vec3(1, 0, 0), 1, .4f), .01, .03, 6, 5, true);
    h.add(new TrailSample(Vec3.ZERO, new Vec3(0, 1, 0), 1.05, .5f), .01, .03, 6, 5, true);
    assertEquals(7, h.size());
    for (int i = 0; i < h.size(); i++)
      assertEquals(1, h.get(i).origin().distanceTo(h.get(i).tip()), 1e-8);
  }
}
