package com.cappleapple.combattraces.compat.bettercombat;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.SwingWindow;
import dev.kosmx.playerAnim.core.data.AnimationFormat;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation.AnimationBuilder;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation.StateCollection.State;
import dev.kosmx.playerAnim.core.util.Ease;
import org.junit.jupiter.api.Test;

class BetterCombatSwingTimingTest {
  @Test
  void excludesAuthoredWindupAndFollowThroughEvenWhenAllPartsMove() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 0, 0, 6, -0.7f, 12, 2.2f, 24, 2.7f);
    keys(builder.rightItem.pitch, 0, 0, 6, -0.2f, 12, 0.8f, 24, 1.0f);
    keys(builder.torso.yaw, 0, 0, 6, -0.1f, 12, 1.2f, 24, 1.3f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .4f, false));
  }

  @Test
  void fasterRecoveryCannotDisplaceTheStrokeThatPassesThroughContact() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 12, 0, 16, -1.3f, 24, -1.4f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .4f, false));
  }

  @Test
  void contactAtAKeyframeSelectsTheArrivingStrokeBeforeFastRecovery() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 12, 0, 16, -1.3f, 24, -1.4f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void fadeInBoundaryDoesNotCutOffHeavyStrike() {
    AnimationBuilder builder = animation();
    builder.beginTick = 12;
    keys(builder.rightArm.pitch, 6, -2.5f, 12, -0.4f, 24, -0.3f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void lateDamageTimingDoesNotPreferSlowRecovery() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1.2f, 12, 1.1f, 24, 1.6f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), 1, false));
  }

  @Test
  void coordinatedItemMotionSeparatesStrikeFromAnotherArmTransition() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 12, 0, 20, -1.4f, 24, -1.5f);
    keys(builder.rightItem.pitch, 6, -.6f, 12, .6f, 24, .6f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), 1, false));
  }

  @Test
  void constantArmChannelsFallBackToAnimatedItemWithoutBendChannel() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, .2f, 12, .2f, 24, .2f);
    keys(builder.rightItem.z, 6, 0, 12, -12, 24, -10);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void tinyWristJitterCannotSelectALateOneTickTransition() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, -1, 12, 1, 24, 1.2f);
    keys(builder.rightArm.roll, 6, 0, 22, .0001f, 23, .0002f, 24, .0002f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .9f, false));
  }

  @Test
  void spinIncludesTheRevolutionButExcludesSlowSettling() {
    AnimationBuilder builder = animation();
    keys(
        builder.torso.yaw,
        8,
        1.4f,
        10,
        .1f,
        12,
        -1.2f,
        14,
        -2.7f,
        16,
        2.1f,
        18,
        .7f,
        20,
        .5f,
        22,
        .53f,
        24,
        .53f);
    keys(builder.rightArm.pitch, 8, -.8f, 12, -.79f, 24, -.2f);
    assertWindow(8, 18, BetterCombatSwingTiming.resolve(builder.build(), .5f, true));
  }

  @Test
  void authoredFullTurnRemainsMotionInsteadOfBeingNormalizedToZero() {
    AnimationBuilder builder = animation();
    keys(builder.torso.yaw, 6, 0, 12, (float) (Math.PI * 2), 24, (float) (Math.PI * 2 + .1));
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, true));
    AnimationBuilder arm = animation();
    keys(arm.rightArm.roll, 6, 0, 12, (float) (Math.PI * 2), 24, (float) (Math.PI * 2 + .1));
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(arm.build(), .5f, false));
  }

  @Test
  void torsoOnlyAttackStillHasAFiniteStrike() {
    AnimationBuilder builder = animation();
    keys(builder.torso.pitch, 4, -.2f, 12, 1.4f, 24, 1.5f);
    assertWindow(4, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void missingMotionUsesBoundedFallbackAndInvalidHintFailsClosed() {
    KeyframeAnimation animation = animation().build();
    assertEquals(SwingWindow.around(.5f), BetterCombatSwingTiming.resolve(animation, .5f, false));
    assertNull(BetterCombatSwingTiming.resolve(null, Float.NaN, false));
  }

  @Test
  void aContinuousMultiKeyframeSlashIncludesItsSlowerEnding() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 10, -.3f, 15, .7f, 20, 1.05f, 24, 1.06f);
    keys(builder.rightArm.pitch, 6, -.5f, 10, -.8f, 15, -1.1f, 20, -1.25f, 24, -1.26f);
    assertWindow(6, 20, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void contactInMiddleOfOneStrokeDoesNotDiscardEarlierKeyframes() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.roll, 4, -.5f, 8, 0, 12, 1.0f, 16, 1.5f, 24, 1.52f);
    assertWindow(4, 16, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void torsoMotionCarriesTheStrikeAfterArmMotionSlows() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, -1.2f, 10, .4f, 15, .35f, 24, .35f);
    keys(builder.torso.yaw, 6, -.2f, 10, .4f, 15, 1.5f, 24, 1.51f);
    assertWindow(6, 15, BetterCombatSwingTiming.resolve(builder.build(), .4f, false));
  }

  @Test
  void armYawCanContinueAStrokeWhilePitchEasesOff() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, 1, 10, -1, 15, -.85f, 24, -.84f);
    keys(builder.rightArm.yaw, 6, 1, 10, .9f, 15, -.8f, 24, -.81f);
    assertWindow(6, 15, BetterCombatSwingTiming.resolve(builder.build(), .4f, false));
  }

  @Test
  void stationaryArmCannotHideAnAnimatedSpinningItemFlight() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 4, -.5f, 8, -.8f, 20, -.8f, 24, -1.5f);
    keys(builder.rightItem.z, 8, 0, 14, -40, 20, 0);
    keys(builder.rightItem.pitch, 8, -1, 14, 3, 20, 10);
    assertWindow(8, 20, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void staffFinisherIncludesItemRotationsWithANonSpinHitbox() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 10, 0, 15, .8f, 20, 1.0f, 24, 1.0f);
    keys(builder.rightItem.pitch, 6, -.5f, 20, -12);
    assertWindow(6, 20, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void itemAndTorsoRevolutionsTogetherCoverTheFullCircularAttack() {
    AnimationBuilder builder = animation();
    keys(builder.torso.yaw, 6, 1, 8, -1, 10, -3, 12, 1, 14, -.5f, 16, -.52f);
    keys(builder.rightItem.pitch, 8, 0, 15, -6, 22, -12);
    assertWindow(6, 22, BetterCombatSwingTiming.resolve(builder.build(), .5f, true));
  }

  @Test
  void aLeftOnlyCustomAnimationStillFindsItsStrike() {
    AnimationBuilder builder = animation();
    keys(builder.leftArm.pitch, 6, -1, 12, 1, 24, 1.02f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void theFreeArmDoesNotExtendACompleteRightHandStroke() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, -1, 12, 1, 24, 1.02f);
    keys(builder.leftArm.pitch, 6, 0, 24, 5);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void pausesSeparateAnAttackFromAnotherFastMovement() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.yaw, 6, -1, 12, 1, 16, 1, 20, 2.5f, 24, 2.5f);
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  @Test
  void aLateWristFlourishCannotReplaceTheEarlierContactStroke() {
    AnimationBuilder builder = animation();
    keys(builder.rightArm.pitch, 6, -1.5f, 12, 1.5f, 24, 1.51f);
    keys(builder.rightItem.roll, 6, 0, 16, 0, 24, (float) (Math.PI * 2));
    assertWindow(6, 12, BetterCombatSwingTiming.resolve(builder.build(), .5f, false));
  }

  private static AnimationBuilder animation() {
    AnimationBuilder builder = new AnimationBuilder(AnimationFormat.UNKNOWN);
    builder.beginTick = 4;
    builder.endTick = 24;
    builder.stopTick = 30;
    return builder;
  }

  private static void keys(State state, float... values) {
    for (int i = 0; i < values.length; i += 2) {
      state.addKeyFrame((int) values[i], values[i + 1], Ease.LINEAR);
    }
    state.setEnabled(true);
  }

  private static void assertWindow(int start, int end, SwingWindow window) {
    assertNotNull(window);
    assertEquals(start / 24f, window.start(), 1e-6);
    assertEquals(end / 24f, window.end(), 1e-6);
  }
}
