package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.data.AnimationRule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class TrailActivationTest {
  private CombatMotion motion(float progress) {
    return new CombatMotion(
        1,
        ItemStack.EMPTY,
        InteractionHand.MAIN_HAND,
        ResourceLocation.parse("test:swing"),
        progress,
        .5f,
        0,
        1,
        1,
        "sword",
        "");
  }

  @Test
  void configuredMinimumAlsoCapsExplicitWindows() {
    var window = new AnimationRule(.1f, .9f, 1);
    assertFalse(TrailActivation.active(motion(.5f), window, 3, 4));
    assertTrue(TrailActivation.active(motion(.5f), window, 5, 4));
    assertFalse(TrailActivation.active(motion(.5f), window, 5, 6));
  }

  @Test
  void earlyWindupAndFinishedAttackDoNotEmit() {
    assertFalse(TrailActivation.active(motion(.2f), AnimationRule.automatic(), 30, 4));
    assertTrue(TrailActivation.active(motion(.45f), AnimationRule.automatic(), 10, 4));
    assertFalse(TrailActivation.active(motion(1), AnimationRule.automatic(), 30, 4));
  }

  @Test
  void automaticMotionMustAlsoClearMinimumSpeed() {
    assertFalse(TrailActivation.active(motion(.5f), AnimationRule.automatic(), .5, 4));
    assertTrue(TrailActivation.active(motion(.5f), AnimationRule.automatic(), .5, 0));
    assertFalse(TrailActivation.active(motion(.5f), AnimationRule.automatic(), Double.NaN, 4));
  }

  private CombatMotion authored(float progress) {
    var m = motion(progress);
    return new CombatMotion(
        m.attackId(),
        m.weapon(),
        m.hand(),
        m.animation(),
        progress,
        m.hitProgress(),
        m.comboIndex(),
        m.comboLength(),
        m.damageMultiplier(),
        m.category(),
        m.pose(),
        m.shape(),
        null,
        null,
        new SwingWindow(.4f, .55f));
  }

  @Test
  void fastWindupAndFastRecoveryStayDarkIncludingAtZeroSpeedThreshold() {
    for (float progress : new float[] {0, .25f, .399f, .551f, .8f, .99f})
      assertFalse(TrailActivation.active(authored(progress), AnimationRule.automatic(), 100, 0));
    assertTrue(TrailActivation.active(authored(.47f), AnimationRule.automatic(), 100, 0));
  }

  @Test
  void completeAuthoredStrokeIncludesBothBoundaryPoses() {
    assertTrue(TrailActivation.active(authored(.4f), AnimationRule.automatic(), 10, 4));
    assertTrue(TrailActivation.active(authored(.55f), AnimationRule.automatic(), 10, 4));
    assertFalse(TrailActivation.active(authored(.48f), AnimationRule.automatic(), 3, 4));
  }

  @Test
  void olderProvidersAlsoStopBeforeRecovery() {
    assertNull(motion(.5f).swingWindow());
    assertTrue(TrailActivation.active(motion(.6f), AnimationRule.automatic(), 20, 4));
    assertFalse(TrailActivation.active(motion(.7f), AnimationRule.automatic(), 20, 4));
  }

  @Test
  void authoredPackWindowCanCorrectAutomaticEstimateWithoutDisablingSpeedGate() {
    var correction = new AnimationRule(.6f, .7f, 1);
    assertFalse(TrailActivation.active(authored(.5f), correction, 20, 4));
    assertTrue(TrailActivation.active(authored(.65f), correction, 20, 4));
    assertFalse(TrailActivation.active(authored(.65f), correction, 2, 4));
    assertFalse(TrailActivation.active(authored(.8f), correction, 20, 4));
  }

  @Test
  void malformedProviderProgressNeverEmits() {
    for (float progress : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY, 1, 2})
      assertFalse(TrailActivation.active(authored(progress), new AnimationRule(0f, 1f, 1), 20, 0));
    assertFalse(TrailActivation.active(authored(.5f), AnimationRule.automatic(), 20, Double.NaN));
  }

  @Test
  void swingWindowsRejectMalformedBounds() {
    assertThrows(IllegalArgumentException.class, () -> new SwingWindow(-.1f, .5f));
    assertThrows(IllegalArgumentException.class, () -> new SwingWindow(.5f, .5f));
    assertThrows(IllegalArgumentException.class, () -> new SwingWindow(.8f, .3f));
    assertThrows(IllegalArgumentException.class, () -> new SwingWindow(.1f, 1.1f));
    assertThrows(IllegalArgumentException.class, () -> new SwingWindow(Float.NaN, .5f));
    assertNull(SwingWindow.around(Float.NaN));
    assertNull(SwingWindow.around(-1));
    assertFalse(new SwingWindow(.2f, .3f).contains(Float.NaN));
  }

  @Test
  void fallbackWindowsStayInsideAnimationBounds() {
    assertEquals(0, SwingWindow.around(0).start());
    assertEquals(1, SwingWindow.around(1).end());
  }
}
