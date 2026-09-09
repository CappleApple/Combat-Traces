package com.cappleapple.combattraces.client.element;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.client.impact.ImpactManager;
import com.cappleapple.combattraces.client.particle.AccentParticles;
import com.cappleapple.combattraces.client.trail.TrailManager;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.TrailSample;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class ElementEffects {
  public static void trails(
      LivingEntity entity,
      CombatMotion motion,
      int emitter,
      boolean first,
      TrailSample sample,
      List<ResourceLocation> elements,
      float density) {
    if (!ClientConfig.TRAILS.get()
        || ClientConfig.QUALITY.get() == 0
        || !ClientConfig.ELEMENTS.get()
        || entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
            > Math.pow(ClientConfig.FULL_DISTANCE.get(), 2)) return;
    for (var id : elements) {
      var definition =
          ClientDefinitions.current.elements().stream()
              .filter(e -> e.id().equals(id))
              .findFirst()
              .orElse(null);
      if (definition == null || definition.trail() == null) continue;
      var style = ClientDefinitions.current.trail(definition.trail()).tinted(definition.color());
      TrailManager.sample(entity, motion, emitter, first, sample, style, id.toString(), density);
      com.cappleapple.combattraces.client.trail.TrailAccents.emit(
          entity, emitter, first, sample, style, id.toString());
    }
  }

  public static void impact(ImpactContext ctx, double now) {
    if (!ClientConfig.ELEMENTS.get()
        || ctx.position()
                .distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
            > Math.pow(ClientConfig.FULL_DISTANCE.get(), 2)) return;
    boolean first =
        ctx.attacker() == Minecraft.getInstance().player
            && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    int layers = 0;
    for (var id : ctx.elements()) {
      if (layers++ >= ClientConfig.ELEMENT_LAYERS.get()) break;
      var definition =
          ClientDefinitions.current.elements().stream()
              .filter(e -> e.id().equals(id))
              .findFirst()
              .orElse(null);
      if (definition == null) continue;
      if (definition.impact() != null)
        ImpactManager.layer(
            ctx,
            ClientDefinitions.current.impact(definition.impact()),
            now,
            ctx.strength() * 1.02f);
      AccentParticles.queue(
          AccentParticles.type(definition.particle()),
          ctx.position(),
          ctx.tangent(),
          ctx.normal(),
          Math.round(3 * ctx.strength()),
          first,
          definition.color());
    }
  }
}
