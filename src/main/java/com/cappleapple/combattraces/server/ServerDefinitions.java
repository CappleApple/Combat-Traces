package com.cappleapple.combattraces.server;

import com.cappleapple.combattraces.data.*;
import com.cappleapple.combattraces.network.DefinitionPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "combattraces")
public final class ServerDefinitions {
  public static Definitions current = Definitions.EMPTY;
  private static String json = "{}";
  private static long revision;

  @SubscribeEvent
  public static void reload(AddReloadListenerEvent event) {
    event.addListener(
        new DefinitionReloadListener(
            raw -> {
              current = Definitions.compile(raw);
              json = DefinitionWire.encode(raw);
              revision++;
            }));
  }

  @SubscribeEvent
  public static void sync(OnDatapackSyncEvent event) {
    int count = (json.length() + 8191) / 8192;
    if (count > 64) return;
    event
        .getRelevantPlayers()
        .filter(p -> p.connection.hasChannel(DefinitionPayload.TYPE))
        .forEach(
            player -> {
              for (int i = 0; i < count; i++)
                PacketDistributor.sendToPlayer(
                    player,
                    new DefinitionPayload(
                        revision,
                        i,
                        count,
                        json.substring(i * 8192, Math.min(json.length(), (i + 1) * 8192))));
            });
  }
}
