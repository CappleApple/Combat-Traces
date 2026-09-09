package com.cappleapple.combattraces.client.trail;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.data.EffectStyle;
import com.cappleapple.combattraces.motion.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public final class TrailManager {
  public record Key(
      int entity, boolean offhand, long attack, int emitter, boolean firstPerson, String layer) {}

  private static final Map<Key, TrailInstance> TRAILS = new LinkedHashMap<>();

  public static Collection<TrailInstance> trails() {
    return TRAILS.values();
  }

  public static void clear() {
    TRAILS.clear();
  }

  public static void sample(
      LivingEntity entity,
      CombatMotion motion,
      int emitter,
      boolean firstPerson,
      TrailSample sample,
      EffectStyle style,
      String layer,
      float sampleMultiplier) {
    if (!ClientConfig.TRAILS.get() || ClientConfig.QUALITY.get() == 0) return;
    var mc = Minecraft.getInstance();
    double distance =
        Math.sqrt(entity.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()));
    if (distance > Math.min(ClientConfig.TRAIL_DISTANCE.get(), ClientConfig.DISTANCE.get())) return;
    boolean reduced =
        distance > ClientConfig.FULL_DISTANCE.get() || ClientConfig.QUALITY.get() == 1;
    var key =
        new Key(
            entity.getId(),
            motion.hand() == net.minecraft.world.InteractionHand.OFF_HAND,
            motion.attackId(),
            emitter,
            firstPerson,
            layer);
    var trail = TRAILS.get(key);
    if (trail == null) {
      while (TRAILS.size() >= ClientConfig.MAX_TRAILS.get()) {
        var worst =
            TRAILS.entrySet().stream()
                .max(Comparator.comparingDouble(e -> priority(e.getValue(), mc)))
                .orElseThrow();
        double incoming =
            entity == mc.player
                ? -1000
                : entity.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition());
        if (incoming > priority(worst.getValue(), mc)) return;
        TRAILS.remove(worst.getKey());
      }
      int cap = Math.max(4, ClientConfig.SAMPLES.get() / (reduced ? 2 : 1));
      trail = new TrailInstance(entity, motion.attackId(), firstPerson, style, cap);
      TRAILS.put(key, trail);
    }
    if (trail.paused) {
      trail.history.clear();
      trail.paused = false;
    }
    trail.touched = sample.time();
    trail.history.add(
        sample,
        ClientConfig.MIN_DISTANCE.get() * (reduced ? 2 : 1) / sampleMultiplier,
        ClientConfig.MAX_DISTANCE.get() * (reduced ? 2 : 1) / sampleMultiplier,
        ClientConfig.SUBDIVISIONS.get(),
        ClientConfig.DISCONTINUITY.get());
    enforceBudget(mc);
  }

  /** Let the old strip fade; a resumed fast movement starts a separate strip. */
  public static void pause(LivingEntity entity, CombatMotion motion, int emitter, boolean first) {
    for (var entry : TRAILS.entrySet()) {
      var key = entry.getKey();
      if (key.entity() == entity.getId()
          && key.attack() == motion.attackId()
          && key.offhand() == (motion.hand() == net.minecraft.world.InteractionHand.OFF_HAND)
          && key.emitter() == emitter
          && key.firstPerson() == first) entry.getValue().paused = true;
    }
  }

  public static void prune(double now) {
    TRAILS
        .values()
        .removeIf(
            t -> {
              t.history.prune(now - t.style.lifetime() * ClientConfig.TRAIL_LIFETIME.get());
              return t.owner.isRemoved() || t.history.size() == 0;
            });
  }

  private static double priority(TrailInstance t, Minecraft mc) {
    return t.owner == mc.player
        ? -1000 - t.touched * 0.0001
        : t.owner.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) - t.touched * 0.0001;
  }

  private static void enforceBudget(Minecraft mc) {
    int count = TRAILS.values().stream().mapToInt(t -> t.history.size()).sum();
    if (count <= ClientConfig.TOTAL_SAMPLES.get()) return;
    var sorted = new ArrayList<>(TRAILS.values());
    sorted.sort(Comparator.comparingDouble((TrailInstance t) -> priority(t, mc)).reversed());
    for (var trail : sorted)
      while (count > ClientConfig.TOTAL_SAMPLES.get() && trail.history.size() > 0) {
        trail.history.dropOldest();
        count--;
      }
  }
}
