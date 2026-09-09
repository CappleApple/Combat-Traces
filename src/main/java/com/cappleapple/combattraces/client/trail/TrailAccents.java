package com.cappleapple.combattraces.client.trail;

import com.cappleapple.combattraces.client.particle.AccentParticles;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.data.EffectStyle;
import com.cappleapple.combattraces.motion.TrailSample;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Style accent cadence is time-based, independent of rendering FPS. */
public final class TrailAccents {
  private static final Map<String, Double> NEXT = new HashMap<>();

  public static void clear() {
    NEXT.clear();
  }

  public static void emit(
      LivingEntity owner,
      int emitter,
      boolean first,
      TrailSample sample,
      EffectStyle style,
      String layer) {
    if (!ClientConfig.TRAILS.get()
        || ClientConfig.QUALITY.get() == 0
        || emitter != 0
        || style.particleRate() <= 0) return;
    String key = owner.getId() + ":" + layer;
    if (sample.time() >= NEXT.getOrDefault(key, 0d)) {
      NEXT.put(key, sample.time() + 1 / Math.max(.1, style.particleRate() * 20));
      AccentParticles.queue(
          AccentParticles.type(style.particle()),
          sample.tip(),
          Vec3.ZERO,
          new Vec3(0, 1, 0),
          1,
          first);
    }
    if (NEXT.size() > 512) NEXT.entrySet().removeIf(e -> e.getValue() < sample.time() - 2);
  }
}
