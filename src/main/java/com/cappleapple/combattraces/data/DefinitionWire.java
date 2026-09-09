package com.cappleapple.combattraces.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class DefinitionWire {
  public static String encode(Map<ResourceLocation, JsonElement> definitions) {
    var json = new JsonObject();
    definitions.forEach((id, value) -> json.add(id.toString(), value));
    return json.toString();
  }

  public static Map<ResourceLocation, JsonElement> decode(String text) {
    if (text.length() > 524288) throw new JsonParseException("Definition bundle too large");
    var obj = JsonParser.parseString(text).getAsJsonObject();
    if (obj.size() > 2048) throw new JsonParseException("Too many definitions");
    Map<ResourceLocation, JsonElement> result = new TreeMap<>();
    obj.entrySet().forEach(entry -> result.put(JsonFields.id(entry.getKey()), entry.getValue()));
    return Map.copyOf(result);
  }
}
