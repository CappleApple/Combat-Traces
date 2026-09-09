package com.cappleapple.combattraces.client.element;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.ClientDefinitions;
import com.cappleapple.combattraces.config.ClientConfig;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class ElementResolver {
  public static List<ResourceLocation> resolve(
      LivingEntity entity, CombatMotion motion, List<ResourceLocation> explicit) {
    if (!ClientConfig.ELEMENTS.get() || ClientConfig.ELEMENT_LAYERS.get() == 0) return List.of();
    Set<ResourceLocation> values = new LinkedHashSet<>(explicit);
    for (var provider : CombatTracesApi.elementProviders())
      values.addAll(provider.elements(entity, motion));
    for (var definition : ClientDefinitions.current.elements())
      if (definition.condition().test(entity, motion.weapon(), motion)) values.add(definition.id());
    values.remove(ResourceLocation.fromNamespaceAndPath("combattraces", "physical"));
    return values.stream()
        .filter(Objects::nonNull)
        .limit(ClientConfig.ELEMENT_LAYERS.get())
        .toList();
  }
}
