package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.trail.TrailManager;
import com.cappleapple.combattraces.client.weapon.WeaponResolver;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.data.AnimationRule;
import com.cappleapple.combattraces.motion.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;

public final class HeldItemCapture {
  private static final Deque<LivingEntity> CONTEXT = new ArrayDeque<>();
  public static final Map<Integer, TrailSample> LATEST = new HashMap<>();

  private record Key(int entity, boolean offhand, boolean firstPerson, int emitter) {}

  private static final Map<Key, StrokeSampler> STROKES = new HashMap<>();

  public static void clear() {
    LATEST.clear();
    STROKES.clear();
  }

  public static void prune(double now) {
    LATEST.entrySet().removeIf(e -> now - e.getValue().time() > 1);
    STROKES.values().removeIf(s -> now - s.lastTime() > 1);
  }

  public static void push(LivingEntity entity) {
    CONTEXT.push(entity);
  }

  public static void pop() {
    CONTEXT.pop();
  }

  public static void sample(
      ItemStack stack, ItemDisplayContext display, Matrix4f pose, BakedModel model) {
    if (CONTEXT.isEmpty()
        || !(display.firstPerson()
            || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) return;
    var mc = Minecraft.getInstance();
    var entity = CONTEXT.peek();
    if (entity.level() != mc.level || mc.isPaused() || mc.getCameraEntity() == null) return;
    var camera = mc.gameRenderer.getMainCamera();
    if (entity.distanceToSqr(camera.getPosition()) > Math.pow(ClientConfig.DISTANCE.get(), 2))
      return;
    float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
    var motion = CombatTracesApi.motion(entity, partial).orElse(null);
    if (motion == null) return;
    boolean left =
        display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    if ((left == (entity.getMainArm() == HumanoidArm.LEFT))
        != (motion.hand() == InteractionHand.MAIN_HAND)) return;
    if (!ItemStack.isSameItem(stack, motion.weapon()) || !FrameState.capture(entity.getId(), left))
      return;
    boolean first = entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    boolean visible =
        entity == mc.player
            ? (first ? ClientConfig.FIRST_PERSON.get() : ClientConfig.THIRD_PERSON.get())
            : ClientConfig.OTHERS.get();
    var resolved = WeaponResolver.resolve(entity, motion, model);
    var window =
        ClientDefinitions.current
            .animations()
            .getOrDefault(motion.animation(), AnimationRule.automatic());
    // Standard first-person hands are view-space; Player Animator's third-person-model pass is
    // already world-space.
    Matrix4f transform =
        display.firstPerson() ? new Matrix4f().rotation(camera.rotation()).mul(pose) : pose;
    double now = VisualClock.now();
    var samples = new ArrayList<TrailSample>(resolved.emitters().size());
    double speed = 0;
    for (int i = 0; i < resolved.emitters().size(); i++) {
      var emitter = resolved.emitters().get(i);
      var origin =
          PoseTransforms.toWorld(
              transform, emitter.origin().add(0.5, 0.5, 0.5), camera.getPosition());
      var tip =
          PoseTransforms.toWorld(transform, emitter.tip().add(0.5, 0.5, 0.5), camera.getPosition());
      var sample = new TrailSample(origin, tip, now, motion.progress());
      var observation = WeaponMotionTracker.record(entity, motion, i, sample, partial);
      samples.add(sample);
      speed =
          Math.max(
              speed,
              Math.max(
                  observation.state().tipVelocity().length(),
                  observation.state().originVelocity().length()));
      if (i == 0) {
        LATEST.put(entity.getId(), sample);
        if (DebugRenderer.active() && entity == mc.player) {
          DebugState.motion = motion;
          DebugState.weapon = resolved;
          DebugState.state = observation.state();
        }
      }
    }
    var bounds = TrailActivation.window(motion, window);
    for (int i = 0; i < samples.size(); i++) {
      var key = new Key(entity.getId(), motion.hand() == InteractionHand.OFF_HAND, first, i);
      if (!visible) {
        STROKES.remove(key);
        TrailManager.pause(entity, motion, i, first);
        continue;
      }
      if (!STROKES.containsKey(key) && STROKES.size() >= 2048) prune(now);
      if (!STROKES.containsKey(key) && STROKES.size() >= 2048) continue;
      var sampler = STROKES.computeIfAbsent(key, k -> new StrokeSampler());
      var emitted =
          sampler.advance(
              motion.attackId(),
              samples.get(i),
              bounds,
              speed,
              ClientConfig.SPEED_THRESHOLD.get(),
              ClientConfig.DISCONTINUITY.get());
      var style = ClientDefinitions.current.trail(resolved.trail());
      for (var sample : emitted) {
        TrailManager.sample(
            entity, motion, i, first, sample, style, "base", window.sampleMultiplier());
        com.cappleapple.combattraces.client.element.ElementEffects.trails(
            entity,
            motion,
            i,
            first,
            sample,
            resolved.elements(),
            window.sampleMultiplier(),
            false);
      }
      // Reconstructed boundary poses add geometry, not delayed accent bursts in recovery.
      if (!emitted.isEmpty() && bounds != null && bounds.contains(motion.progress())) {
        var sample = emitted.getLast();
        com.cappleapple.combattraces.client.trail.TrailAccents.emit(
            entity, i, first, sample, style, "base", samples.size());
        com.cappleapple.combattraces.client.element.ElementEffects.accents(
            entity, i, first, sample, resolved.elements(), samples.size());
      }
      if (bounds == null || motion.progress() >= bounds.end())
        TrailManager.pause(entity, motion, i, first);
    }
  }
}
