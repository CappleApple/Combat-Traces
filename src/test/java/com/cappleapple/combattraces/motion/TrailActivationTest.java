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
}
