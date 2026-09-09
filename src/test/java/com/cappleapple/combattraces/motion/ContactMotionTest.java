package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ContactMotionTest {
  private static CombatMotion motion(long id, float progress, AttackShape shape) {
    return new CombatMotion(
        id,
        ItemStack.EMPTY,
        InteractionHand.MAIN_HAND,
        ResourceLocation.parse("combattraces:test"),
        progress,
        .5f,
        0,
        3,
        1,
        "sword",
        "",
        shape);
  }

  private static MotionAnalysis.State state(MotionAnalysis.Type type) {
    return new MotionAnalysis.State(
        new Vec3(0, 0, 5), new Vec3(0, 0, 5), new Vec3(0, 0, 1), Vec3.ZERO, 5, type);
  }

  @Test
  void retainsStabThroughRecoveryUntilTheNextAttack() {
    var gesture = new ContactMotion();
    assertEquals(
        MotionAnalysis.Type.THRUST,
        gesture.observe(motion(1, .48f, AttackShape.FORWARD), state(MotionAnalysis.Type.THRUST)));
    assertEquals(
        MotionAnalysis.Type.THRUST,
        gesture.observe(
            motion(1, .72f, AttackShape.FORWARD), state(MotionAnalysis.Type.HORIZONTAL)));
    assertEquals(
        MotionAnalysis.Type.HORIZONTAL,
        gesture.observe(motion(2, .4f, AttackShape.SWEEP), state(MotionAnalysis.Type.HORIZONTAL)));
  }

  @Test
  void explicitlySweepingAttackDoesNotLatchAnIncidentalAxialMotion() {
    var gesture = new ContactMotion();
    assertNotEquals(
        MotionAnalysis.Type.THRUST,
        gesture.observe(motion(1, .5f, AttackShape.SWEEP), state(MotionAnalysis.Type.THRUST)));
  }

  @Test
  void windupBeforeContactWindowDoesNotLatch() {
    var gesture = new ContactMotion();
    gesture.observe(motion(1, .1f, AttackShape.FORWARD), state(MotionAnalysis.Type.THRUST));
    assertEquals(
        MotionAnalysis.Type.VERTICAL_DOWN,
        gesture.observe(
            motion(1, .5f, AttackShape.FORWARD), state(MotionAnalysis.Type.VERTICAL_DOWN)));
  }

  @Test
  void incomingCutSurvivesDifferentRecoveryDirectionAndResetsForNextAttack() {
    var gesture = new ContactMotion();
    var incoming =
        new MotionAnalysis.State(
            new Vec3(10, -8, 0),
            Vec3.ZERO,
            new Vec3(1, -1, 0).normalize(),
            new Vec3(0, 0, 1),
            12,
            MotionAnalysis.Type.DIAGONAL_DOWN);
    var recovery =
        new MotionAnalysis.State(
            new Vec3(-2, 8, 0),
            Vec3.ZERO,
            new Vec3(-1, 4, 0).normalize(),
            new Vec3(0, 0, 1),
            8,
            MotionAnalysis.Type.VERTICAL_UP);
    gesture.observe(motion(1, .50f, AttackShape.SWEEP), incoming);
    gesture.observe(motion(1, .85f, AttackShape.SWEEP), recovery);
    assertEquals(0, incoming.tangent().distanceTo(gesture.tangent(recovery)), 1e-8);
    gesture.observe(motion(2, .5f, AttackShape.SWEEP), recovery);
    assertEquals(0, recovery.tangent().distanceTo(gesture.tangent(incoming)), 1e-8);
  }

  @Test
  void slowForwardContactLatchesForDelayedHit() {
    var gesture = new ContactMotion();
    var gentle =
        new MotionAnalysis.State(
            new Vec3(0, 0, .7),
            new Vec3(0, 0, .6),
            new Vec3(0, 0, 1),
            Vec3.ZERO,
            .65,
            MotionAnalysis.Type.THRUST);
    assertEquals(
        MotionAnalysis.Type.THRUST, gesture.observe(motion(1, .49f, AttackShape.FORWARD), gentle));
    assertEquals(
        MotionAnalysis.Type.THRUST,
        gesture.observe(motion(1, .85f, AttackShape.FORWARD), MotionAnalysis.State.still()));
  }

  @Test
  void fastEarlyWindupDoesNotChooseTheCutDirection() {
    var gesture = new ContactMotion();
    var windup =
        new MotionAnalysis.State(
            Vec3.ZERO,
            Vec3.ZERO,
            new Vec3(0, 1, 0),
            Vec3.ZERO,
            40,
            MotionAnalysis.Type.VERTICAL_UP);
    gesture.observe(motion(1, .1f, AttackShape.SWEEP), windup);
    var incoming = state(MotionAnalysis.Type.HORIZONTAL);
    gesture.observe(motion(1, .5f, AttackShape.SWEEP), incoming);
    assertEquals(incoming.tangent(), gesture.tangent(windup));
  }
}
