package com.cappleapple.combattraces.mixin;

import com.cappleapple.combattraces.client.HeldItemCapture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemContextMixin {
  @WrapMethod(method = "renderItem")
  private void combatTraces$context(
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext display,
      boolean left,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      Operation<Void> original) {
    HeldItemCapture.push(entity);
    try {
      original.call(entity, stack, display, left, poses, buffers, light);
    } finally {
      HeldItemCapture.pop();
    }
  }
}
