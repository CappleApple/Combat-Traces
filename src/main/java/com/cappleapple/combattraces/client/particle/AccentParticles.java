package com.cappleapple.combattraces.client.particle;

import com.cappleapple.combattraces.config.ClientConfig;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Small shared queue and strict rolling-second budget for all accent sources. The ribbons never use
 * particles.
 */
public final class AccentParticles {
  private record Request(
      ParticleOptions particle, Vec3 position, Vec3 tangent, Vec3 normal, int count, int color) {}

  private static final ArrayDeque<Request> QUEUE = new ArrayDeque<>();
  private static final com.cappleapple.combattraces.motion.ParticleBudget BUDGET =
      new com.cappleapple.combattraces.motion.ParticleBudget();
  public static long spawned;
  private static int impactRemaining = Integer.MAX_VALUE;

  public static void beginImpact() {
    impactRemaining = ClientConfig.PARTICLES_IMPACT.get();
  }

  public static void endImpact() {
    impactRemaining = Integer.MAX_VALUE;
  }

  public static void clear() {
    QUEUE.clear();
    BUDGET.clear();
    spawned = 0;
  }

  public static ParticleOptions type(ResourceLocation id) {
    var value = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
    return value instanceof SimpleParticleType simple ? simple : null;
  }

  public static void queue(
      ParticleOptions particle,
      Vec3 position,
      Vec3 tangent,
      Vec3 normal,
      int count,
      boolean firstPerson) {
    queue(particle, position, tangent, normal, count, firstPerson, 0xffffff);
  }

  public static void queue(
      ParticleOptions particle,
      Vec3 position,
      Vec3 tangent,
      Vec3 normal,
      int count,
      boolean firstPerson,
      int color) {
    var mc = Minecraft.getInstance();
    if (!ClientConfig.PARTICLES.get()
        || particle == null
        || mc.level == null
        || QUEUE.size() >= 128) return;
    if (position.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())
        > Math.pow(ClientConfig.FULL_DISTANCE.get(), 2)) return;
    double expected =
        Math.max(
            0,
            count
                * ClientConfig.PARTICLE_MULTIPLIER.get()
                * (firstPerson ? ClientConfig.FP_PARTICLES.get() : 1));
    int amount =
        Math.clamp(
            (int) Math.floor(expected)
                + (mc.level.random.nextDouble() < expected - Math.floor(expected) ? 1 : 0),
            0,
            ClientConfig.PARTICLES_IMPACT.get());
    amount = Math.min(amount, impactRemaining);
    impactRemaining -= amount;
    if (amount > 0) QUEUE.addLast(new Request(particle, position, tangent, normal, amount, color));
  }

  public static void drain(double now) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) {
      clear();
      return;
    }
    BUDGET.beginFrame(now, ClientConfig.PARTICLES_SECOND.get(), ClientConfig.PARTICLES_FRAME.get());
    while (!QUEUE.isEmpty()) {
      var request = QUEUE.removeFirst();
      if (!ClientConfig.PARTICLES.get()
          || request.position.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())
              > Math.pow(ClientConfig.FULL_DISTANCE.get(), 2)) continue;
      if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) continue;
      if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.DECREASED
          && mc.level.random.nextInt(3) != 0) continue;
      int count = BUDGET.take(request.count);
      for (int i = 0; i < count; i++) {
        var random = mc.level.random;
        var velocity =
            request
                .normal
                .scale(0.045 + random.nextDouble() * 0.055)
                .add(request.tangent.scale(0.03))
                .add(
                    (random.nextDouble() - .5) * .09,
                    (random.nextDouble() - .5) * .09,
                    (random.nextDouble() - .5) * .09);
        var particle =
            ParticleTint.create(
                request.color,
                () ->
                    mc.particleEngine.createParticle(
                        request.particle,
                        request.position.x,
                        request.position.y,
                        request.position.z,
                        velocity.x,
                        velocity.y,
                        velocity.z));
        if (particle != null) spawned++;
      }
    }
  }
}
