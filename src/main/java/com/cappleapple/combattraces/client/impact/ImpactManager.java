package com.cappleapple.combattraces.client.impact;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.data.EffectStyle;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ImpactManager {
  private static final List<ImpactInstance> ACTIVE = new ArrayList<>();

  public static Collection<ImpactInstance> active() {
    return ACTIVE;
  }

  public static void clear() {
    ACTIVE.clear();
  }

  public static void add(ImpactContext ctx, double now) {
    if (!entityContact(ctx)) return;
    ctx = CombatTracesApi.modifyImpact(ctx);
    if (!entityContact(ctx)) return;
    if (com.cappleapple.combattraces.client.DebugRenderer.active())
      com.cappleapple.combattraces.client.DebugState.impact = ctx;
    if (!ClientConfig.IMPACTS.get()
        || ctx.position()
                .distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
            > Math.pow(ClientConfig.DISTANCE.get(), 2)) return;
    com.cappleapple.combattraces.client.particle.AccentParticles.beginImpact();
    try {
      compose(ctx, now);
    } finally {
      com.cappleapple.combattraces.client.particle.AccentParticles.endImpact();
    }
  }

  private static void compose(ImpactContext ctx, double now) {
    String family =
        com.cappleapple.combattraces.motion.HitboxImpact.family(
                ctx.weaponClass(), ctx.motion().hitbox(), ctx.motionType())
            .path();
    if (family.equals("magic")) family = "generic";
    var style =
        com.cappleapple.combattraces.client.ClientDefinitions.current.impact(
            ResourceLocation.fromNamespaceAndPath("combattraces", family));
    layer(ctx, style, now, ctx.strength() * (ctx.blocked() ? 0.55f : 1));
    com.cappleapple.combattraces.client.element.ElementEffects.impact(ctx, now);
    com.cappleapple.combattraces.client.material.MaterialEffects.impact(ctx);
    if (ctx.blocked()
        || ctx.weaponClass() == WeaponClass.BLUNT
        || (ctx.critical() && ClientConfig.CRITICAL.get())) {
      var ring =
          new EffectStyle(
              ResourceLocation.fromNamespaceAndPath(
                  "combattraces", "textures/vfx/impacts/ring.png"),
              true,
              0.14,
              1,
              0.6f,
              0,
              "ease_out",
              1,
              0.5f,
              0xffffdd,
              1,
              20,
              null,
              0);
      layer(ctx, ring, now, ctx.strength() * 0.65f);
    }
  }

  public static void layer(ImpactContext ctx, EffectStyle style, double now, float size) {
    if (!entityContact(ctx)) return;
    while (ACTIVE.size() >= ClientConfig.MAX_IMPACTS.get()) {
      var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      var worst =
          ACTIVE.stream()
              .max(Comparator.comparingDouble(i -> priority(i.context(), camera)))
              .orElseThrow();
      if (priority(ctx, camera) > priority(worst.context(), camera)) return;
      ACTIVE.remove(worst);
    }
    Vec3 local = ctx.position().subtract(ctx.target().position());
    var rule =
        com.cappleapple.combattraces.client.weapon.WeaponResolver.override(ctx.motion().weapon());
    ACTIVE.add(
        new ImpactInstance(
            ctx,
            style,
            now,
            Math.clamp(size * (rule == null ? 1 : rule.impactScale()), 0.2f, 3),
            local));
  }

  private static boolean entityContact(ImpactContext ctx) {
    var level = Minecraft.getInstance().level;
    return ctx != null
        && ctx.target() != null
        && !ctx.target().isRemoved()
        && level != null
        && ctx.target().level() == level
        && ctx.attacker().level() == level;
  }

  private static double priority(ImpactContext ctx, Vec3 camera) {
    return ctx.attacker() == Minecraft.getInstance().player
        ? -1000
        : ctx.position().distanceToSqr(camera);
  }

  public static void prune(double now) {
    ACTIVE.removeIf(
        i ->
            !entityContact(i.context())
                || now - i.created() > i.style().lifetime() * ClientConfig.IMPACT_LIFETIME.get());
  }
}
