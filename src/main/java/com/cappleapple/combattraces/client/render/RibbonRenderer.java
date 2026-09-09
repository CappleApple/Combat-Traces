package com.cappleapple.combattraces.client.render;

import com.cappleapple.combattraces.client.trail.TrailManager;
import com.cappleapple.combattraces.config.ClientConfig;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public final class RibbonRenderer {
  private RibbonRenderer() {}

  public static void render(MultiBufferSource.BufferSource buffers, Vec3 camera, double now) {
    if (!ClientConfig.TRAILS.get()) return;
    for (var trail : TrailManager.trails()) {
      if (trail.history.size() < 2) continue;
      var consumer =
          buffers.getBuffer(VfxRenderTypes.get(trail.style.texture(), trail.style.additive()));
      var style = trail.style;
      float width =
          style.width() * (trail.firstPerson ? ClientConfig.FP_WIDTH.get().floatValue() : 1);
      float opacity = trail.firstPerson ? ClientConfig.FP_OPACITY.get().floatValue() : 1;
      float offset = (float) (now * style.scroll() % 1);
      for (int i = 1; i < trail.history.size(); i++) {
        var a = trail.history.get(i - 1);
        var b = trail.history.get(i);
        var a0 = a.origin().subtract(camera);
        var a1 = a.origin().lerp(a.tip(), width).subtract(camera);
        var b0 = b.origin().subtract(camera);
        var b1 = b.origin().lerp(b.tip(), width).subtract(camera);
        // Skip the whole segment inside the near-camera guard instead of making a giant
        // clipped
        // quad.
        if (trail.firstPerson
            && (a0.lengthSqr() < 0.09
                || a1.lengthSqr() < 0.09
                || b0.lengthSqr() < 0.09
                || b1.lengthSqr() < 0.09)) continue;
        float aa = style.alpha((now - a.time()) / ClientConfig.TRAIL_LIFETIME.get()) * opacity;
        float ab = style.alpha((now - b.time()) / ClientConfig.TRAIL_LIFETIME.get()) * opacity;
        float u0 =
            (float) (now - a.time())
                    / (float) (style.lifetime() * ClientConfig.TRAIL_LIFETIME.get())
                + offset;
        float u1 =
            (float) (now - b.time())
                    / (float) (style.lifetime() * ClientConfig.TRAIL_LIFETIME.get())
                + offset;
        float frame = (int) (now * style.fps()) % Math.max(1, style.frames());
        float v0 = frame / style.frames(), v1 = (frame + 1) / style.frames();
        vertex(consumer, a0, u0, v0, aa, style.color());
        vertex(consumer, a1, u0, v1, aa, style.color());
        vertex(consumer, b1, u1, v1, ab, style.color());
        vertex(consumer, b0, u1, v0, ab, style.color());
      }
    }
  }

  public static void vertex(VertexConsumer out, Vec3 p, float u, float v, float alpha, int color) {
    out.addVertex((float) p.x, (float) p.y, (float) p.z)
        .setUv(u, v)
        .setColor(
            (color >> 16 & 255) / 255f,
            (color >> 8 & 255) / 255f,
            (color & 255) / 255f,
            Math.clamp(alpha, 0, 1));
  }
}
