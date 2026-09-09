package com.cappleapple.combattraces.mixin;

import com.cappleapple.combattraces.client.particle.ParticleTint;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserves the complete createParticle/add lifecycle and other mods' hooks. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
  @WrapOperation(
      method = "createParticle",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
  private void combatTraces$initializeBeforePublication(
      ParticleEngine engine, Particle particle, Operation<Void> original) {
    ParticleTint.beforePublish(particle);
    original.call(engine, particle);
  }
}
