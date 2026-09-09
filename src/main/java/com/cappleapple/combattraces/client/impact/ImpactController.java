package com.cappleapple.combattraces.client.impact;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.motion.*;
import com.cappleapple.combattraces.network.HitPayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class ImpactController {
  public static long receivedHits;
  private static final Map<String, Long> DEDUP = new HashMap<>();

  public static void clear() {
    DEDUP.clear();
    receivedHits = 0;
  }

  public static void receive(HitPayload packet) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (!(mc.level.getEntity(packet.attacker()) instanceof LivingEntity attacker)) return;
    var target = mc.level.getEntity(packet.target());
    if (target == null) return;
    String key = packet.attacker() + ":" + packet.target();
    if (Objects.equals(DEDUP.put(key, packet.tick()), packet.tick())) return;
    if (DEDUP.size() > 512) DEDUP.clear();
    double now = VisualClock.now();
    var observation = WeaponMotionTracker.atHit(attacker.getId(), packet.tick(), now).orElse(null);
    var motion =
        observation == null
            ? CombatTracesApi.motion(attacker, 0).orElse(null)
            : observation.motion();
    if (motion == null) return;
    Vec3 center = target.getBoundingBox().getCenter();
    Vec3 source = observation == null ? attacker.getEyePosition() : observation.sample().tip();
    Vec3 outside =
        center.add(
            ImpactMath.unit(
                    source.subtract(center), attacker.position().subtract(center).normalize())
                .scale(target.getBbWidth() + target.getBbHeight() + 1));
    Vec3 hit = target.getBoundingBox().clip(outside, center).orElse(center);
    Vec3 hitboxCenter = observation == null ? motion.hitboxCenter() : observation.hitboxCenter();
    hit = ImpactMath.atHitboxHeight(hit, hitboxCenter);
    Vec3 normal =
        ImpactMath.unit(hit.subtract(center), attacker.position().subtract(center).normalize());
    Vec3 tangent =
        observation == null
            ? attacker.getViewVector(0).cross(new Vec3(0, 1, 0)).normalize()
            : observation.contactTangent();
    double velocity = observation == null ? 0 : observation.state().speed();
    WeaponClass family =
        com.cappleapple.combattraces.client.weapon.WeaponResolver.classify(attacker, motion, null);
    var gesture =
        HitboxImpact.resolve(
            family,
            observation == null ? motion.hitbox() : observation.hitbox(),
            observation == null ? MotionAnalysis.Type.UNKNOWN : observation.contactType(),
            tangent);
    tangent = gesture.tangent();
    var rule = com.cappleapple.combattraces.client.weapon.WeaponResolver.override(motion.weapon());
    var elements =
        com.cappleapple.combattraces.client.element.ElementResolver.resolve(
            attacker, motion, rule == null ? List.of() : rule.elements());
    var strength =
        ImpactMath.strength(
            motion.damageMultiplier(),
            velocity,
            packet.damage(),
            observation == null
                ? 1
                : observation.sample().tip().distanceTo(observation.sample().origin()),
            packet.critical(),
            motion.comboIndex() + 1 == motion.comboLength());
    var ctx =
        new ImpactContext(
            attacker,
            target,
            motion,
            hit,
            tangent,
            normal,
            velocity,
            strength,
            family,
            gesture.type(),
            elements,
            com.cappleapple.combattraces.client.material.MaterialResolver.entity(
                target, packet.blocked()),
            packet.critical(),
            packet.blocked());
    ImpactManager.add(ctx, now);
    receivedHits++;
  }
}
