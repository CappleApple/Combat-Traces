package com.cappleapple.combattraces.network;

import java.util.function.Consumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common-side dispatch contains no references to client-only Minecraft classes. */
public final class NetworkHandlers {
  public static Consumer<DefinitionPayload> definitionReceiver = packet -> {};
  public static Consumer<HitPayload> hitReceiver = packet -> {};

  public static void register(RegisterPayloadHandlersEvent event) {
    event
        .registrar("1")
        .optional()
        .playToClient(
            HitPayload.TYPE, HitPayload.CODEC, (packet, ctx) -> hitReceiver.accept(packet))
        .playToClient(
            DefinitionPayload.TYPE,
            DefinitionPayload.CODEC,
            (packet, ctx) -> definitionReceiver.accept(packet));
  }
}
