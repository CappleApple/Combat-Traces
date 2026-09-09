package com.cappleapple.combattraces.data;

import static com.cappleapple.combattraces.data.JsonFields.*;

import com.cappleapple.combattraces.api.WeaponClass;
import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/** Immutable, validated definition snapshot. Invalid entries cannot break the rest of a reload. */
public record Definitions(
    Map<ResourceLocation, EffectStyle> trails,
    Map<ResourceLocation, EffectStyle> impacts,
    List<WeaponRule> weapons,
    List<WeaponRule> classifications,
    List<ElementDefinition> elements,
    List<MaterialDefinition> materials,
    Map<ResourceLocation, AnimationRule> animations) {
  public static final Definitions EMPTY =
      new Definitions(Map.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
  public static final Set<String> SECTIONS =
      Set.of(
          "trails", "impacts", "weapons", "classifications", "elements", "materials", "animations");

  public static Definitions compile(Map<ResourceLocation, JsonElement> raw) {
    var trails = new HashMap<ResourceLocation, EffectStyle>();
    var impacts = new HashMap<ResourceLocation, EffectStyle>();
    var weapons = new ArrayList<WeaponRule>();
    var classes = new ArrayList<WeaponRule>();
    var elements = new ArrayList<ElementDefinition>();
    var materials = new ArrayList<MaterialDefinition>();
    var animations = new HashMap<ResourceLocation, AnimationRule>();
    raw.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .limit(2048)
        .forEach(
            entry -> {
              var key = entry.getKey();
              int slash = key.getPath().indexOf('/');
              if (slash < 0) return;
              String section = key.getPath().substring(0, slash);
              var id =
                  ResourceLocation.fromNamespaceAndPath(
                      key.getNamespace(), key.getPath().substring(slash + 1));
              try {
                var j = entry.getValue().getAsJsonObject();
                switch (section) {
                  case "trails" -> trails.put(id, style(j, true));
                  case "impacts" -> impacts.put(id, style(j, false));
                  case "weapons", "classifications" -> {
                    var rule =
                        new WeaponRule(
                            id,
                            (int) number(j, "priority", 0, -10000, 10000),
                            ids(j, "items"),
                            ids(j, "item_tags"),
                            j.has("weapon_class")
                                ? WeaponClass.parse(j.get("weapon_class").getAsString())
                                : null,
                            ids(j, "elements"),
                            optionalId(j, "trail"),
                            emitters(j),
                            (float) number(j, "impact_scale", 1, 0.1, 3));
                    if (rule.items().isEmpty() && rule.tags().isEmpty())
                      throw new JsonParseException("Rule requires items or item_tags");
                    (section.equals("weapons") ? weapons : classes).add(rule);
                  }
                  case "animations" -> {
                    var animation = j.has("animation") ? id(j.get("animation").getAsString()) : id;
                    Float start = null, end = null;
                    if (j.has("trail_window")) {
                      var w = j.getAsJsonObject("trail_window");
                      start = (float) number(w, "start", 0, 0, 1);
                      end = (float) number(w, "end", 1, 0, 1);
                      if (start > end)
                        throw new JsonParseException("Trail window start exceeds end");
                    }
                    animations.put(
                        animation,
                        new AnimationRule(
                            start, end, (float) number(j, "sample_multiplier", 1, 0.25, 3)));
                  }
                  case "elements" -> {
                    var elementId = j.has("id") ? id(j.get("id").getAsString()) : id;
                    elements.add(
                        new ElementDefinition(
                            elementId,
                            (int) number(j, "priority", 0, -10000, 10000),
                            ItemCondition.parse(j.get("conditions"), 0),
                            j.has("trail") ? optionalId(j, "trail") : elementId,
                            j.has("impact")
                                ? optionalId(j, "impact")
                                : ResourceLocation.fromNamespaceAndPath(
                                    elementId.getNamespace(), elementId.getPath() + "_overlay"),
                            optionalId(j, "particle"),
                            color(j)));
                  }
                  case "materials" ->
                      materials.add(
                          new MaterialDefinition(
                              j.has("material") ? id(j.get("material").getAsString()) : id,
                              (int) number(j, "priority", 0, -10000, 10000),
                              ids(j, "block_tags"),
                              ids(j, "blocks"),
                              ids(j, "armor_tags"),
                              ids(j, "entity_tags"),
                              ids(j, "entities"),
                              optionalId(j, "particle"),
                              color(j),
                              (int) number(j, "particle_count", 4, 0, 32)));
                  default -> {}
                }
              } catch (RuntimeException ex) {
                LoggerFactory.getLogger("Combat Traces")
                    .warn("Ignoring invalid definition {}: {}", key, ex.getMessage());
              }
            });
    weapons.sort(Comparator.comparingInt(WeaponRule::priority).reversed());
    classes.sort(Comparator.comparingInt(WeaponRule::priority).reversed());
    elements.sort(Comparator.comparingInt(ElementDefinition::priority).reversed());
    materials.sort(Comparator.comparingInt(MaterialDefinition::priority).reversed());
    return new Definitions(
        Map.copyOf(trails),
        Map.copyOf(impacts),
        List.copyOf(weapons),
        List.copyOf(classes),
        List.copyOf(elements),
        List.copyOf(materials),
        Map.copyOf(animations));
  }

  public static int color(JsonObject j) {
    return j.has("color")
        ? Integer.parseUnsignedInt(j.get("color").getAsString().replace("#", ""), 16) & 0xffffff
        : 0xffffff;
  }

  public static EffectStyle style(JsonObject j, boolean trail) {
    var texture =
        id(
            string(
                j,
                "texture",
                "combattraces:textures/vfx/"
                    + (trail ? "trails/slash" : "impacts/generic")
                    + ".png"));
    return new EffectStyle(
        texture,
        string(j, "render_mode", "translucent").equals("additive"),
        number(j, "lifetime_ms", trail ? 180 : 180, 30, 1000) / 1000,
        (float) number(j, "width_multiplier", 1, 0.05, 4),
        (float)
            Math.clamp(
                number(j, "opacity", 0.8, 0, 1) * number(j, "intensity_multiplier", 1, 0, 3), 0, 1),
        (float) number(j, "uv_scroll_speed", 0, -10, 10),
        string(j, "fade_curve", "ease_out"),
        (float) number(j, "scale", 1, 0.1, 3),
        (float) number(j, "camera_bias", 0.25, 0, 1),
        color(j),
        (int) number(j, "frames", 1, 1, 64),
        (float) number(j, "fps", 20, 1, 120),
        optionalId(j, "particle"),
        (float) number(j, "particle_rate", 0, 0, 4));
  }

  public EffectStyle trail(ResourceLocation id) {
    return trails.getOrDefault(id, EffectStyle.trail());
  }

  public EffectStyle impact(ResourceLocation id) {
    return impacts.getOrDefault(
        id,
        new EffectStyle(
            ResourceLocation.fromNamespaceAndPath(
                "combattraces", "textures/vfx/impacts/generic.png"),
            true,
            0.18,
            1,
            0.85f,
            0,
            "ease_out",
            1,
            0.25f,
            0xffffff,
            1,
            20,
            null,
            0));
  }
}
