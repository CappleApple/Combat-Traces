package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.data.*;
import com.cappleapple.combattraces.network.DefinitionPayload;
import com.google.gson.JsonElement;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class ClientDefinitions {
  public static Definitions current = Definitions.EMPTY;
  private static Map<ResourceLocation, JsonElement> local = Map.of(), server = Map.of();
  private static long revision = -1;
  public static long appliedRevision = -1;
  private static String[] chunks;
  private static double started;

  public static void localReload(Map<ResourceLocation, JsonElement> raw) {
    local = raw;
    rebuild();
  }

  public static void disconnect() {
    server = Map.of();
    chunks = null;
    revision = -1;
    appliedRevision = -1;
    rebuild();
  }

  public static void accept(DefinitionPayload packet) {
    if (packet.count() < 1
        || packet.count() > 64
        || packet.index() < 0
        || packet.index() >= packet.count()) return;
    double now = System.nanoTime() * 1e-9;
    if (chunks == null || revision != packet.revision() || now - started > 10) {
      if (packet.index() != 0) return;
      revision = packet.revision();
      chunks = new String[packet.count()];
      started = now;
    }
    if (chunks.length != packet.count()) {
      chunks = null;
      return;
    }
    chunks[packet.index()] = packet.json();
    if (Arrays.stream(chunks).allMatch(Objects::nonNull)) {
      try {
        server = DefinitionWire.decode(String.join("", chunks));
        appliedRevision = revision;
        rebuild();
      } catch (RuntimeException ex) {
        org.slf4j.LoggerFactory.getLogger("Combat Traces")
            .warn("Invalid synchronized definitions: {}", ex.getMessage());
      }
      chunks = null;
    }
  }

  private static void rebuild() {
    var raw = new HashMap<>(local);
    raw.putAll(server);
    current = Definitions.compile(raw);
    ClientState.clearEffects();
    com.cappleapple.combattraces.client.weapon.WeaponResolver.clear();
    com.cappleapple.combattraces.client.render.VfxRenderTypes.clearCache();
  }
}
