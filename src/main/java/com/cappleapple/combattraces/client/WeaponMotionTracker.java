package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.motion.*;
import java.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class WeaponMotionTracker {
  public record Key(int entity, InteractionHand hand, int emitter) {}

  public record Observation(
      CombatMotion motion,
      TrailSample sample,
      MotionAnalysis.State state,
      double gameTick,
      MotionAnalysis.Type contactType,
      Vec3 contactTangent,
      Vec3 hitboxCenter,
      AttackHitbox hitbox) {}

  private static final Map<Key, ArrayDeque<Observation>> HISTORY = new HashMap<>();
  private static final Map<Key, Double> ANGULAR = new HashMap<>();
  private static final Map<Key, ContactMotion> CONTACT = new HashMap<>();
  private static final Map<Key, Vec3> BODY = new HashMap<>();

  public static Observation record(
      LivingEntity owner, CombatMotion motion, int emitter, TrailSample sample, float partial) {
    var key = new Key(owner.getId(), motion.hand(), emitter);
    var queue = HISTORY.computeIfAbsent(key, k -> new ArrayDeque<>());
    var before = queue.peekLast();
    var pos = owner.getPosition(partial);
    var previousBody = BODY.put(key, pos);
    if (before != null
        && (before.motion.attackId() != motion.attackId()
            || sample.time() - before.sample.time() > 0.3)) {
      queue.clear();
      ANGULAR.remove(key);
      CONTACT.remove(key);
      before = null;
    }
    var state =
        MotionAnalysis.between(
            before == null ? null : before.sample,
            sample,
            previousBody == null ? Vec3.ZERO : pos.subtract(previousBody),
            motion.shape());
    var contact = CONTACT.computeIfAbsent(key, k -> new ContactMotion());
    var contactType = contact.observe(motion, state);
    if (before != null && sample.time() - before.sample.time() >= 0.001) {
      var a = before.sample.tip().subtract(before.sample.origin()).normalize();
      var b = sample.tip().subtract(sample.origin()).normalize();
      double angle = ANGULAR.getOrDefault(key, 0d) + Math.acos(Math.clamp(a.dot(b), -1, 1));
      ANGULAR.put(key, angle);
      if (angle > Math.PI * 1.4)
        state =
            new MotionAnalysis.State(
                state.tipVelocity(),
                state.originVelocity(),
                state.tangent(),
                state.planeNormal(),
                state.speed(),
                MotionAnalysis.Type.SPIN);
    }
    var observation =
        new Observation(
            motion,
            sample,
            state,
            owner.level().getGameTime() + partial,
            contactType,
            contact.tangent(state),
            contact.center(motion),
            contact.hitbox(motion));
    if (before != null && sample.time() - before.sample.time() < 0.001) return before;
    queue.addLast(observation);
    while (queue.size() > 32) queue.removeFirst();
    return observation;
  }

  public static Observation latest(int entity, InteractionHand hand, int emitter) {
    var queue = HISTORY.get(new Key(entity, hand, emitter));
    return queue == null ? null : queue.peekLast();
  }

  public static Optional<Observation> atHit(int entity, long gameTick, double now) {
    return HISTORY.entrySet().stream()
        .filter(e -> e.getKey().entity == entity)
        .flatMap(e -> e.getValue().stream())
        .filter(
            o ->
                now - o.sample.time() < 0.6
                    && (o.motion.hitbox() != null || o.state.speed() > 0.05))
        .min(Comparator.comparingDouble(o -> Math.abs(o.gameTick - gameTick)));
  }

  public static void prune(double now) {
    HISTORY
        .entrySet()
        .removeIf(
            e -> {
              if (now - e.getValue().getLast().sample.time() > 1) {
                BODY.remove(e.getKey());
                CONTACT.remove(e.getKey());
                ANGULAR.remove(e.getKey());
                return true;
              }
              return false;
            });
  }

  public static void clear() {
    HISTORY.clear();
    BODY.clear();
    CONTACT.clear();
    ANGULAR.clear();
  }
}
