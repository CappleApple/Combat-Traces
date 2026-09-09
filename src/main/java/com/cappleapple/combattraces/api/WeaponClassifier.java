package com.cappleapple.combattraces.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface WeaponClassifier {
  Optional<WeaponClass> classify(LivingEntity entity, CombatMotion motion);
}
