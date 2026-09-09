package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.compat.bettercombat.BetterCombatIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = "combattraces", dist = Dist.CLIENT)
public final class CombatTracesClient {
  public CombatTracesClient(IEventBus bus, net.neoforged.fml.ModContainer container) {
    container.registerExtensionPoint(
        net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
        net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientCommands::register);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DebugRenderer::hud);
    bus.addListener(
        (net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) ->
            event.registerReloadListener(
                new com.cappleapple.combattraces.data.DefinitionReloadListener(
                    ClientDefinitions::localReload)));
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientState::logout);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientState::tick);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(DebugRenderer::render);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FrameState::begin);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
        com.cappleapple.combattraces.client.impact.ClientHitFallback::tick);
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
        com.cappleapple.combattraces.client.render.WorldEffectsRenderer::render);
    bus.addListener(
        (FMLClientSetupEvent event) ->
            event.enqueueWork(
                () -> {
                  com.cappleapple.combattraces.network.NetworkHandlers.definitionReceiver =
                      ClientDefinitions::accept;
                  com.cappleapple.combattraces.network.NetworkHandlers.hitReceiver =
                      com.cappleapple.combattraces.client.impact.ImpactController::receive;
                  com.cappleapple.combattraces.api.CombatTracesApi.installImpactSink(
                      context ->
                          net.minecraft.client.Minecraft.getInstance()
                              .execute(
                                  () -> {
                                    var mc = net.minecraft.client.Minecraft.getInstance();
                                    if (mc.level != null && context.attacker().level() == mc.level)
                                      com.cappleapple.combattraces.client.impact.ImpactManager.add(
                                          context, VisualClock.now());
                                  }));
                  if (ModList.get().isLoaded("bettercombat")) BetterCombatIntegration.register();
                }));
  }
}
