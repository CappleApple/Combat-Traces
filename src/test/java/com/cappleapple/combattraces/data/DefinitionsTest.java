package com.cappleapple.combattraces.data;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DefinitionsTest {
  private Map<ResourceLocation, JsonElement> raw(String path, String json) {
    return Map.of(ResourceLocation.parse("test:" + path), JsonParser.parseString(json));
  }

  @Test
  void twinEmittersAndPrioritySurviveWireRoundTrip() {
    var data =
        raw(
            "weapons/twin",
            "{\"priority\":100,\"items\":[\"minecraft:diamond_sword\"],\"emitters\":[{\"origin\":[0,0.1,0],\"tip\":[0,1,0]},{\"origin\":[0,-0.1,0],\"tip\":[0,-1,0]}]}");
    var decoded = Definitions.compile(DefinitionWire.decode(DefinitionWire.encode(data)));
    assertEquals(2, decoded.weapons().getFirst().emitters().size());
    assertEquals(100, decoded.weapons().getFirst().priority());
  }

  @Test
  void invalidAnimationWindowDoesNotBreakOtherDefinitions() {
    var data =
        new HashMap<>(raw("animations/broken", "{\"trail_window\":{\"start\":0.9,\"end\":0.1}}"));
    data.putAll(raw("trails/good", "{\"lifetime_ms\":99999,\"opacity\":4}"));
    var definitions = Definitions.compile(data);
    assertTrue(definitions.animations().isEmpty());
    var style = definitions.trails().values().iterator().next();
    assertEquals(1, style.lifetime());
    assertEquals(1, style.opacity());
  }

  @Test
  void unknownConditionFailsClosed() {
    assertThrows(
        JsonParseException.class,
        () -> ItemCondition.parse(JsonParser.parseString("{\"made_up\":\"minecraft:test\"}"), 0));
  }

  @Test
  void degenerateEmitterIsRejected() {
    assertThrows(
        JsonParseException.class,
        () ->
            JsonFields.emitters(
                JsonParser.parseString("{\"emitters\":[{\"origin\":[0,0,0],\"tip\":[0,0,0]}]}")
                    .getAsJsonObject()));
  }

  @Test
  void oversizedBundleRejectedBeforeParsing() {
    assertThrows(JsonParseException.class, () -> DefinitionWire.decode("x".repeat(524289)));
  }
}
