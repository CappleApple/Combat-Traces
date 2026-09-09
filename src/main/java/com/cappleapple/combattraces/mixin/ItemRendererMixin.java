package com.cappleapple.combattraces.mixin;

import com.cappleapple.combattraces.client.HeldItemCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
  @Inject(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/resources/model/BakedModel;isCustomRenderer()Z"))
  private void combatTraces$capture(
      ItemStack stack,
      ItemDisplayContext display,
      boolean left,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo ci) {
    HeldItemCapture.sample(stack, display, poses.last().pose(), model);
  }
}
