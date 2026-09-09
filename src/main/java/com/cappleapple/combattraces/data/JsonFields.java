package com.cappleapple.combattraces.data;

import com.cappleapple.combattraces.api.TrailEmitter;
import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class JsonFields {
  private JsonFields() {}

  public static String string(JsonObject j, String k, String fallback) {
    return j.has(k) ? j.get(k).getAsString() : fallback;
  }

  public static double number(JsonObject j, String k, double fallback, double min, double max) {
    double v = j.has(k) ? j.get(k).getAsDouble() : fallback;
    if (!Double.isFinite(v)) throw new JsonParseException(k + " must be finite");
    return Math.clamp(v, min, max);
  }

  public static ResourceLocation id(String value) {
    var id = ResourceLocation.tryParse(value);
    if (id == null) throw new JsonParseException("Invalid resource id " + value);
    return id;
  }

  public static ResourceLocation optionalId(JsonObject j, String k) {
    return j.has(k) ? id(j.get(k).getAsString()) : null;
  }

  public static List<ResourceLocation> ids(JsonObject j, String k) {
    if (!j.has(k)) return List.of();
    var out = new ArrayList<ResourceLocation>();
    if (j.getAsJsonArray(k).size() > 128) throw new JsonParseException(k + " has too many entries");
    for (var e : j.getAsJsonArray(k)) out.add(id(e.getAsString()));
    return List.copyOf(out);
  }

  public static Vec3 point(JsonElement e) {
    var a = e.getAsJsonArray();
    if (a.size() != 3) throw new JsonParseException("Point must have three coordinates");
    double x = a.get(0).getAsDouble(), y = a.get(1).getAsDouble(), z = a.get(2).getAsDouble();
    if (!Double.isFinite(x + y + z)
        || Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) > 16)
      throw new JsonParseException("Emitter coordinate outside +/-16");
    return new Vec3(x, y, z);
  }

  public static List<TrailEmitter> emitters(JsonObject j) {
    if (!j.has("emitters")) return List.of();
    var result = new ArrayList<TrailEmitter>();
    if (j.getAsJsonArray("emitters").size() > 32)
      throw new JsonParseException("At most 32 emitters per weapon");
    for (var e : j.getAsJsonArray("emitters")) {
      var obj = e.getAsJsonObject();
      var emitter = new TrailEmitter(point(obj.get("origin")), point(obj.get("tip")));
      if (emitter.origin().distanceToSqr(emitter.tip()) < 1e-6)
        throw new JsonParseException("Emitter must have nonzero length");
      result.add(emitter);
    }
    return List.copyOf(result);
  }
}
