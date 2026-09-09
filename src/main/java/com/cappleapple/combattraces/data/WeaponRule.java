package com.cappleapple.combattraces.data;

import com.cappleapple.combattraces.api.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

public record WeaponRule(
    ResourceLocation id,
    int priority,
    List<ResourceLocation> items,
    List<ResourceLocation> tags,
    WeaponClass weaponClass,
    List<ResourceLocation> elements,
    ResourceLocation trail,
    List<TrailEmitter> emitters,
    float impactScale) {
  public boolean matches(ItemStack stack) {
    return items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))
        || tags.stream().anyMatch(id -> stack.is(TagKey.create(Registries.ITEM, id)));
  }
}
