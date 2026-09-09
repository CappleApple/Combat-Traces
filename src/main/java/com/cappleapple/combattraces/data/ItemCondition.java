package com.cappleapple.combattraces.data;

import com.cappleapple.combattraces.api.CombatMotion;
import com.google.gson.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ItemCondition {
  boolean test(LivingEntity entity, ItemStack stack, CombatMotion motion);

  static ItemCondition parse(JsonElement raw, int depth) {
    if (depth > 8) throw new JsonParseException("Condition nesting exceeds eight");
    if (raw == null) return (e, s, m) -> false;
    var j = raw.getAsJsonObject();
    if (j.size() != 1) throw new JsonParseException("Condition must contain exactly one operator");
    String op = j.keySet().iterator().next();
    var value = j.get(op);
    if (op.equals("or") || op.equals("and")) {
      if (value.getAsJsonArray().size() > 32)
        throw new JsonParseException("Too many condition clauses");
      var conditions = new ArrayList<ItemCondition>();
      for (var v : value.getAsJsonArray()) conditions.add(parse(v, depth + 1));
      return (e, s, m) ->
          op.equals("or")
              ? conditions.stream().anyMatch(c -> c.test(e, s, m))
              : conditions.stream().allMatch(c -> c.test(e, s, m));
    }
    if (op.equals("not")) {
      var child = parse(value, depth + 1);
      return (e, s, m) -> !child.test(e, s, m);
    }
    if (op.equals("category")) {
      String category = value.getAsString();
      return (e, s, m) -> category.equalsIgnoreCase(m.category());
    }
    ResourceLocation id = JsonFields.id(value.getAsString());
    return switch (op) {
      case "item", "item_id" -> (e, s, m) -> BuiltInRegistries.ITEM.getKey(s.getItem()).equals(id);
      case "item_tag" -> {
        var tag = TagKey.create(Registries.ITEM, id);
        yield (e, s, m) -> s.is(tag);
      }
      case "enchantment" ->
          (e, s, m) ->
              s.getEnchantments().keySet().stream()
                  .anyMatch(h -> h.is(id) && s.getEnchantments().getLevel(h) > 0);
      case "component" ->
          (e, s, m) ->
              BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(id).map(s::has).orElse(false);
      case "effect" ->
          (e, s, m) -> e.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().is(id));
      default -> throw new JsonParseException("Unknown condition " + op);
    };
  }
}
