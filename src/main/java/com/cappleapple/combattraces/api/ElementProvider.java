package com.cappleapple.combattraces.api;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface ElementProvider {
  List<ResourceLocation> elements(LivingEntity entity, CombatMotion motion);
}
