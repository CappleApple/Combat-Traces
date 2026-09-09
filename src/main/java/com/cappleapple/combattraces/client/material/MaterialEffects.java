package com.cappleapple.combattraces.client.material;

import com.cappleapple.combattraces.api.ImpactContext;
import com.cappleapple.combattraces.client.particle.AccentParticles;
import com.cappleapple.combattraces.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;

public final class MaterialEffects {
  public static void impact(ImpactContext context) {
    if (!ClientConfig.MATERIALS.get()) return;
    var material = MaterialResolver.definition(context.material());
    if (material == null) return;
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    boolean first = context.attacker() == mc.player && mc.options.getCameraType().isFirstPerson();
    int count = Math.round(material.count() * context.strength());
    ParticleOptions particle = AccentParticles.type(material.particle());
    if (context.target() == null
        && !context.material().getPath().equals("metal")
        && !context.material().getPath().equals("water")) {
      var state =
          mc.level.getBlockState(
              BlockPos.containing(context.position().subtract(context.normal().scale(0.03))));
      if (!state.isAir()) particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
    }
    AccentParticles.queue(
        particle,
        context.position(),
        context.tangent(),
        context.normal(),
        count,
        first,
        material.color());
    if (context.blocked() || (context.critical() && ClientConfig.CRITICAL.get()))
      AccentParticles.queue(
          ParticleTypes.ELECTRIC_SPARK,
          context.position(),
          context.tangent(),
          context.normal(),
          3,
          first);
  }
}
