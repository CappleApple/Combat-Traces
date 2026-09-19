package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.SwingWindow;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class StrokeSamplerTest {
  private final SwingWindow window = new SwingWindow(.4f, .6f);

  private TrailSample at(float progress, double time) {
    return new TrailSample(new Vec3(progress, 0, 0), new Vec3(progress, 1, 0), time, progress);
  }

  @Test
  void reconstructsBothEndsWhenEntireStrikeFallsBetweenFrames() {
    var s = new StrokeSampler();
    assertTrue(s.advance(1, at(.3f, 1), window, 30, 4, 5).isEmpty());
    var emitted = s.advance(1, at(.7f, 1.05), window, 30, 4, 5);
    assertEquals(2, emitted.size());
    assertEquals(.4f, emitted.getFirst().progress());
    assertEquals(.6f, emitted.getLast().progress());
    assertEquals(1, emitted.getFirst().tip().distanceTo(emitted.getFirst().origin()), 1e-6);
    assertTrue(emitted.getFirst().time() > 1 && emitted.getLast().time() < 1.05);
  }

  @Test
  void seededEntryIncludesBoundaryInsteadOfStartingAtFirstLateFrame() {
    var s = new StrokeSampler();
    s.advance(1, at(.3f, 1), window, 1, 4, 5);
    var emitted = s.advance(1, at(.46f, 1.05), window, 8, 4, 5);
    assertEquals(.4f, emitted.getFirst().progress());
    assertEquals(.46f, emitted.getLast().progress());
  }

  @Test
  void minimumSpeedStartsStrokeButDecelerationDoesNotPunchHoles() {
    var s = new StrokeSampler();
    assertTrue(s.advance(1, at(.42f, 1), window, 2, 4, 5).isEmpty());
    assertFalse(s.advance(1, at(.46f, 1.02), window, 5, 4, 5).isEmpty());
    assertFalse(s.advance(1, at(.5f, 1.04), window, .2, 4, 5).isEmpty());
    assertEquals(.6f, s.advance(1, at(.65f, 1.06), window, .1, 4, 5).getLast().progress());
    assertTrue(s.advance(1, at(.8f, 1.08), window, 30, 4, 5).isEmpty());
  }

  @Test
  void slowAttackNeverBypassesConfiguredStartingThreshold() {
    var s = new StrokeSampler();
    for (int i = 0; i <= 10; i++)
      assertTrue(s.advance(1, at(i / 10f, 1 + i * .02), window, 3, 4, 5).isEmpty());
  }

  @Test
  void nextAttackCannotInheritThePreviousSpeedLatch() {
    var s = new StrokeSampler();
    s.advance(1, at(.5f, 1), window, 12, 4, 5);
    assertTrue(s.advance(2, at(.5f, 1.02), window, 1, 4, 5).isEmpty());
  }

  @Test
  void longGapAndTeleportNeverReconstructAMissingSweep() {
    var s = new StrokeSampler();
    s.advance(1, at(.3f, 1), window, 20, 4, 5);
    assertTrue(s.advance(1, at(.7f, 1.5), window, 20, 4, 5).isEmpty());
    s.advance(2, at(.3f, 2), window, 20, 4, 5);
    assertTrue(
        s.advance(
                2,
                new TrailSample(new Vec3(50, 0, 0), new Vec3(50, 1, 0), 2.05, .7f),
                window,
                20,
                4,
                5)
            .isEmpty());
  }

  @Test
  void finalFrameCanCloseWindowAtAnimationEnd() {
    var s = new StrokeSampler();
    var late = new SwingWindow(.7f, 1);
    s.advance(1, at(.8f, 1), late, 10, 4, 5);
    assertEquals(1, s.advance(1, at(1, 1.05), late, 0, 4, 5).getLast().progress());
    assertTrue(s.advance(1, at(1, 1.1), late, 10, 4, 5).isEmpty());
  }

  @Test
  void duplicateAndInvalidFramesDoNotCreateGeometry() {
    var s = new StrokeSampler();
    s.advance(1, at(.5f, 1), window, 10, 4, 5);
    assertTrue(s.advance(1, at(.5f, 1), window, 10, 4, 5).isEmpty());
    assertTrue(s.advance(1, at(Float.NaN, 1.05), window, 10, 4, 5).isEmpty());
    assertTrue(s.advance(1, at(.5f, 1.1), window, Double.NaN, 4, 5).isEmpty());
  }

  @Test
  void postEndClockClipsBeforeTheObservedRecoveryPose() {
    var s = new StrokeSampler();
    var late = new SwingWindow(.7f, 1);
    s.advance(1, at(.9f, 1), late, 10, 4, 5);
    var endpoint = s.advance(1, at(1.1f, 1.05), late, 2, 4, 5).getLast();
    assertEquals(1, endpoint.progress());
    assertEquals(1, endpoint.tip().x, 1e-6);
    assertEquals(1.025, endpoint.time(), 1e-6);
    assertTrue(s.advance(1, at(1.2f, 1.1), late, 30, 4, 5).isEmpty());
  }
}
