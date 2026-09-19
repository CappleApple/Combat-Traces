package com.cappleapple.combattraces.mixin.validation;

import com.cappleapple.combattraces.validation.DevelopmentClientSafety;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents playback even if a test or resource reload changes the volume option. */
@Mixin(SoundEngine.class)
public abstract class DevelopmentSoundMixin {
  @Inject(method = "play", at = @At("HEAD"), cancellable = true)
  private void combatTraces$muteDevelopmentPlayback(SoundInstance sound, CallbackInfo ci) {
    DevelopmentClientSafety.preventedSounds++;
    ci.cancel();
  }
}
