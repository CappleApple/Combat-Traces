package com.cappleapple.combattraces;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CombatTraces.MOD_ID)
public final class CombatTraces {
  public static final String MOD_ID = "combattraces";

  public CombatTraces(IEventBus bus, ModContainer container) {
    bus.addListener(com.cappleapple.combattraces.network.NetworkHandlers::register);
    container.registerConfig(
        net.neoforged.fml.config.ModConfig.Type.CLIENT,
        com.cappleapple.combattraces.config.ClientConfig.SPEC);
  }
}
