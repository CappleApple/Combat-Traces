package com.cappleapple.combattraces.mixin.validation;

import com.cappleapple.combattraces.validation.DevelopmentClientSafety;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Development launcher guard; excluded from the release JAR. */
@Mixin(MouseHandler.class)
public abstract class DevelopmentMouseMixin {
  @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
  private void combatTraces$leaveMouseFree(CallbackInfo ci) {
    DevelopmentClientSafety.preventedMouseGrabs++;
    ci.cancel();
  }
}
