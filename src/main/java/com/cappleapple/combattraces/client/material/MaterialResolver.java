package com.cappleapple.combattraces.client.material;

import com.cappleapple.combattraces.client.ClientDefinitions;
import com.cappleapple.combattraces.data.MaterialDefinition;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class MaterialResolver {
  public static ResourceLocation block(BlockState state) {
    for (var rule : ClientDefinitions.current.materials()) {
      if (rule.blocks().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
          || rule.blockTags().stream()
              .anyMatch(id -> state.is(TagKey.create(Registries.BLOCK, id)))) return rule.id();
    }
    return id(state.liquid() ? "water" : "generic");
  }

  public static ResourceLocation entity(Entity entity, boolean blocked) {
    if (blocked) return id("metal");
    for (var rule : ClientDefinitions.current.materials()) {
      if (rule.entities().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))
          || rule.entityTags().stream()
              .anyMatch(id -> entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id))))
        return rule.id();
      if (entity instanceof LivingEntity living)
        for (var stack : living.getArmorSlots())
          if (rule.armorTags().stream()
              .anyMatch(id -> stack.is(TagKey.create(Registries.ITEM, id)))) return rule.id();
    }
    return id(entity instanceof LivingEntity ? "flesh" : "generic");
  }

  public static MaterialDefinition definition(ResourceLocation id) {
    return ClientDefinitions.current.materials().stream()
        .filter(m -> m.id().equals(id) && m.particle() != null)
        .findFirst()
        .orElse(null);
  }

  public static ResourceLocation id(String name) {
    return ResourceLocation.fromNamespaceAndPath("combattraces", name);
  }
}
