package com.cappleapple.combattraces.mixin.bettercombat;

import net.bettercombat.client.animation.AttackAnimationSubStack;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractClientPlayer.class, priority = 900)
public interface AttackStackAccessor {
  @Accessor(value = "attackAnimation", remap = false)
  AttackAnimationSubStack combatTraces$stack();
}
