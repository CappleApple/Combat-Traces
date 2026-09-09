package com.cappleapple.combattraces.api;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/** Queried only on the client main/render thread. Higher registration priority wins. */
@FunctionalInterface
public interface CombatMotionProvider {
  Optional<CombatMotion> getActiveMotion(LivingEntity entity, float partialTick);
}
