package com.cappleapple.combattraces.mixin.bettercombat;

import com.cappleapple.combattraces.compat.bettercombat.BetterCombatTrails;
import java.util.List;
import net.bettercombat.api.fx.*;
import net.bettercombat.client.particle.SlashParticleUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SlashParticleUtil.class, remap = false)
public abstract class BetterCombatTrailMixin {
  @Inject(
      method =
          "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;ZFLjava/util/List;Lnet/bettercombat/api/fx/TrailAppearance;)V",
      at = @At("HEAD"),
      cancellable = true)
  private static void combatTraces$replace(
      AbstractClientPlayer player,
      boolean offhand,
      float range,
      List<ParticlePlacement> placements,
      TrailAppearance appearance,
      CallbackInfo ci) {
    if (BetterCombatTrails.replaces(player, offhand)) {
      BetterCombatTrails.suppressedSpawns++;
      ci.cancel();
    }
  }
}
