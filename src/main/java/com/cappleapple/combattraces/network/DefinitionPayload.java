package com.cappleapple.combattraces.network;

import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DefinitionPayload(long revision, int index, int count, String json)
    implements CustomPacketPayload {
  public static final Type<DefinitionPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("combattraces", "definitions"));
  public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionPayload> CODEC =
      StreamCodec.of(
          (b, p) -> {
            b.writeLong(p.revision);
            b.writeVarInt(p.index);
            b.writeVarInt(p.count);
            b.writeUtf(p.json, 8192);
          },
          b ->
              new DefinitionPayload(b.readLong(), b.readVarInt(), b.readVarInt(), b.readUtf(8192)));

  @Override
  public Type<DefinitionPayload> type() {
    return TYPE;
  }
}
