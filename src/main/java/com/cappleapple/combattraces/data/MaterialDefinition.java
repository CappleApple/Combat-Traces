package com.cappleapple.combattraces.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record MaterialDefinition(
    ResourceLocation id,
    int priority,
    List<ResourceLocation> blockTags,
    List<ResourceLocation> blocks,
    List<ResourceLocation> armorTags,
    List<ResourceLocation> entityTags,
    List<ResourceLocation> entities,
    ResourceLocation particle,
    int color,
    int count) {}
