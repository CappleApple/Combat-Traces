package com.cappleapple.combattraces.mixin.bettercombat;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = KeyframeAnimationPlayer.class, remap = false)
public interface AnimationClockAccessor {
  @Accessor("tickDelta")
  float combatTraces$partialTick();
}
