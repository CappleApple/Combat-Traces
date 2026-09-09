package com.cappleapple.combattraces.client.render;

import com.cappleapple.combattraces.client.impact.ImpactManager;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.HitboxImpact;
import com.cappleapple.combattraces.motion.ImpactMath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public final class ImpactRenderer {
  public static void render(
      MultiBufferSource.BufferSource buffers, Vec3 camera, double now, float partial) {
    if (!ClientConfig.IMPACTS.get()) return;
    for (var impact : ImpactManager.active()) {
      var style = impact.style();
      var ctx = impact.context();
      var center = impact.position(partial);
      if (center.distanceToSqr(camera) > Math.pow(ClientConfig.DISTANCE.get(), 2)) continue;
      var basis =
          ImpactMath.orient(ctx.tangent(), camera.subtract(center), camera.subtract(center), 1);
      long variationSeed =
          Double.doubleToLongBits(impact.created())
              ^ ((long) ctx.attacker().getId() << 32)
              ^ ctx.target().getId();
      basis = HitboxImpact.vary(basis, ctx.weaponClass(), ctx.motion().hitbox(), variationSeed);
      double size = impact.size() * style.scale() * ClientConfig.IMPACT_SCALE.get() * 0.5;
      double age = (now - impact.created()) / ClientConfig.IMPACT_LIFETIME.get();
      // A small expansion reads as impact energy without simulating a persistent decal.
      size *= 0.85 + 0.25 * Math.clamp(age / style.lifetime(), 0, 1);
      if (center.distanceToSqr(camera) < 0.16) continue;
      size = Math.min(size, center.distanceTo(camera) * 0.6);
      var x = basis.tangent().scale(size);
      var y = basis.bitangent().scale(size);
      var p = center.add(basis.normal().scale(0.015)).subtract(camera);
      var out = buffers.getBuffer(VfxRenderTypes.impact(style.texture(), style.additive()));
      float alpha = style.alpha(age);
      float frame = (int) (age * style.fps()) % Math.max(1, style.frames());
      float v0 = frame / style.frames(), v1 = (frame + 1) / style.frames();
      RibbonRenderer.vertex(out, p.subtract(x).subtract(y), 0, v1, alpha, style.color());
      RibbonRenderer.vertex(out, p.add(x).subtract(y), 1, v1, alpha, style.color());
      RibbonRenderer.vertex(out, p.add(x).add(y), 1, v0, alpha, style.color());
      RibbonRenderer.vertex(out, p.subtract(x).add(y), 0, v0, alpha, style.color());
    }
  }
}
