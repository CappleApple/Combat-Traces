package com.cappleapple.combattraces.client.particle;

import java.util.function.Supplier;
import net.minecraft.client.particle.Particle;

/** Color is finalized before ParticleEngine.add publishes to vanilla or an async/GPU queue. */
public final class ParticleTint {
  private static final ThreadLocal<Integer> COLOR = new ThreadLocal<>();

  private ParticleTint() {}

  public static Particle create(int color, Supplier<Particle> factory) {
    Integer previous = COLOR.get();
    COLOR.set(color);
    try {
      return factory.get();
    } finally {
      if (previous == null) COLOR.remove();
      else COLOR.set(previous);
    }
  }

  public static void beforePublish(Particle particle) {
    Integer color = COLOR.get();
    if (color != null)
      particle.setColor(
          (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
  }
}
