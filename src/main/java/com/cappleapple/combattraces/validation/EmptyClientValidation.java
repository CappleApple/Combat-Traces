package com.cappleapple.combattraces.validation;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "combattraces", value = Dist.CLIENT)
public final class EmptyClientValidation {
  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    if (!Boolean.getBoolean("combattraces.validateEmpty")) return;
    var mc = Minecraft.getInstance();
    if (mc.screen != null && mc.getOverlay() == null) {
      System.out.println("COMBAT TRACES CLIENT WITHOUT BETTER COMBAT PASS");
      mc.stop();
    }
  }
}
