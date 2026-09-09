package com.cappleapple.combattraces.network;

import net.minecraft.network.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HitPayload(
    int attacker, int target, float damage, boolean critical, boolean blocked, long tick)
    implements CustomPacketPayload {
  public static final Type<HitPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("combattraces", "hit"));
  public static final StreamCodec<RegistryFriendlyByteBuf, HitPayload> CODEC =
      StreamCodec.of(
          (buf, p) -> {
            buf.writeVarInt(p.attacker);
            buf.writeVarInt(p.target);
            buf.writeFloat(p.damage);
            buf.writeBoolean(p.critical);
            buf.writeBoolean(p.blocked);
            buf.writeVarLong(p.tick);
          },
          buf ->
              new HitPayload(
                  buf.readVarInt(),
                  buf.readVarInt(),
                  buf.readFloat(),
                  buf.readBoolean(),
                  buf.readBoolean(),
                  buf.readVarLong()));

  @Override
  public Type<HitPayload> type() {
    return TYPE;
  }
}
