package com.cappleapple.combattraces.mixin.validation;

import com.cappleapple.combattraces.validation.DevelopmentClientSafety;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops low-level cursor capture requests before GLFW changes the cursor. */
@Mixin(InputConstants.class)
public abstract class DevelopmentCursorMixin {
  @Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
  private static void combatTraces$normalCursorOnly(
      long window, int mode, double x, double y, CallbackInfo ci) {
    if (mode != GLFW.GLFW_CURSOR_NORMAL) {
      DevelopmentClientSafety.preventedCursorChanges++;
      ci.cancel();
    }
  }
}
