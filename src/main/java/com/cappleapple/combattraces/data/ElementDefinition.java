package com.cappleapple.combattraces.data;

import net.minecraft.resources.ResourceLocation;

public record ElementDefinition(
    ResourceLocation id,
    int priority,
    ItemCondition condition,
    ResourceLocation trail,
    ResourceLocation impact,
    ResourceLocation particle,
    int color) {}
