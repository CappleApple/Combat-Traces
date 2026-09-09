package com.cappleapple.combattraces.api;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TrailEmitterProvider {
  List<TrailEmitter> emitters(LivingEntity entity, CombatMotion motion);
}
