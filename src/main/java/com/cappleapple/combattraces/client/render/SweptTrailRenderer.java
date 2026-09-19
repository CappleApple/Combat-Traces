package com.cappleapple.combattraces.client.render;

import com.cappleapple.combattraces.api.WeaponClass;
import com.cappleapple.combattraces.client.trail.TrailInstance;
import com.cappleapple.combattraces.config.ClientConfig;
import com.cappleapple.combattraces.motion.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Original, untextured pixel-stepped geometry. Never enters the particle engine. */
public final class SweptTrailRenderer {
  private SweptTrailRenderer() {}

  public static void render(
      TrailInstance trail, MultiBufferSource.BufferSource buffers, Vec3 camera, double now) {
    var out = buffers.getBuffer(VfxRenderTypes.swept(trail.style.additive()));
    float width =
        trail.style.width() * (trail.firstPerson ? ClientConfig.FP_WIDTH.get().floatValue() : 1);
    float opacity = trail.firstPerson ? ClientConfig.FP_OPACITY.get().floatValue() : 1;
    renderGeometry(out, trail, camera, now, width, opacity, ClientConfig.TRAIL_LIFETIME.get());
  }

  /** Internal geometry entry point shared by the scene renderer and development validation. */
  public static void renderGeometry(
      VertexConsumer out,
      TrailInstance trail,
      Vec3 camera,
      double now,
      float width,
      float opacity,
      double lifetimeMultiplier) {
    var history = trail.history;
    if (history.size() < 2) return;
    double first = history.get(0).time(), last = history.get(history.size() - 1).time();
    double duration = last - first;
    if (duration <= .001) return;
    // Fade the whole crescent after motion stops; fading the old end every frame erased most of it.
    float alpha = trail.style.alpha((now - last) / lifetimeMultiplier) * opacity;
    int edge = tint(trail.style.color(), trail.enchanted ? 0x99e6ff : 0xffffff);
    int body = tint(trail.style.color(), trail.enchanted ? 0x66bdd9 : 0xaaaaaa);
    boolean blunt = trail.family == WeaponClass.BLUNT;
    // The blade samples already include the animated item's complete world transform.
    // A collision-volume plane cannot represent diagonal or changing-plane weapon motion.
    for (int i = 1; i < history.size(); i++) {
      var a = history.get(i - 1);
      var b = history.get(i);
      // One quantized width per strip produces actual steps instead of interpolated smooth edges.
      double wa = SweptTrail.envelope(((a.time() + b.time()) * .5 - first) / duration) * width;
      double wb = wa;
      if (trail.thrust && !blunt) {
        thrust(out, a, b, wa, wb, trail.thrustAxis, camera, alpha, edge, body, trail.firstPerson);
      } else if (blunt) {
        band(out, a, b, wa, wb, 0, .16, true, camera, alpha * .7f, edge, trail.firstPerson);
        band(out, a, b, wa, wb, .16, .84, true, camera, alpha * .25f, body, trail.firstPerson);
        band(out, a, b, wa, wb, .84, 1, true, camera, alpha * .7f, edge, trail.firstPerson);
      } else {
        band(out, a, b, wa, wb, 0, .14, false, camera, alpha, edge, trail.firstPerson);
        band(out, a, b, wa, wb, .14, .38, false, camera, alpha * .48f, body, trail.firstPerson);
        band(out, a, b, wa, wb, .38, .85, false, camera, alpha * .16f, body, trail.firstPerson);
      }
    }
  }

  private static void band(
      VertexConsumer out,
      TrailSample a,
      TrailSample b,
      double wa,
      double wb,
      double from,
      double to,
      boolean blunt,
      Vec3 camera,
      float alpha,
      int color,
      boolean first) {
    quad(
        out,
        point(a, wa, from, blunt),
        point(a, wa, to, blunt),
        point(b, wb, to, blunt),
        point(b, wb, from, blunt),
        camera,
        alpha,
        color,
        first);
  }

  private static Vec3 point(TrailSample s, double width, double across, boolean blunt) {
    return blunt ? SweptTrail.headPoint(s, width, across) : SweptTrail.bladePoint(s, width, across);
  }

  private static void thrust(
      VertexConsumer out,
      TrailSample a,
      TrailSample b,
      double wa,
      double wb,
      Vec3 direction,
      Vec3 camera,
      float alpha,
      int edge,
      int body,
      boolean first) {
    Vec3 axis = direction.lengthSqr() > .5 ? direction : b.tip().subtract(b.origin()).normalize();
    Vec3 u = SweptTrail.perpendicular(axis), v = axis.cross(u).normalize();
    double ra = a.origin().distanceTo(a.tip()) * .22 * wa;
    double rb = b.origin().distanceTo(b.tip()) * .22 * wb;
    for (Vec3 cross : new Vec3[] {u.add(v).normalize(), u.subtract(v).normalize()}) {
      quad(
          out,
          a.tip().subtract(cross.scale(ra)),
          a.tip().add(cross.scale(ra)),
          b.tip().add(cross.scale(rb)),
          b.tip().subtract(cross.scale(rb)),
          camera,
          alpha * .26f,
          body,
          first);
      for (int sign : new int[] {-1, 1}) {
        Vec3 side = cross.scale(sign);
        quad(
            out,
            a.tip().add(side.scale(ra * .72)),
            a.tip().add(side.scale(ra)),
            b.tip().add(side.scale(rb)),
            b.tip().add(side.scale(rb * .72)),
            camera,
            alpha * .8f,
            edge,
            first);
      }
    }
  }

  private static void quad(
      VertexConsumer out,
      Vec3 a,
      Vec3 b,
      Vec3 c,
      Vec3 d,
      Vec3 camera,
      float alpha,
      int color,
      boolean first) {
    a = a.subtract(camera);
    b = b.subtract(camera);
    c = c.subtract(camera);
    d = d.subtract(camera);
    if (first
        && (a.lengthSqr() < .09
            || b.lengthSqr() < .09
            || c.lengthSqr() < .09
            || d.lengthSqr() < .09)) return;
    vertex(out, a, alpha, color);
    vertex(out, b, alpha, color);
    vertex(out, c, alpha, color);
    vertex(out, d, alpha, color);
  }

  private static void vertex(VertexConsumer out, Vec3 p, float alpha, int color) {
    out.addVertex((float) p.x, (float) p.y, (float) p.z)
        .setColor(
            (color >> 16 & 255) / 255f,
            (color >> 8 & 255) / 255f,
            (color & 255) / 255f,
            Math.clamp(alpha, 0, 1));
  }

  private static int tint(int a, int b) {
    return ((a >> 16 & 255) * (b >> 16 & 255) / 255 << 16)
        | ((a >> 8 & 255) * (b >> 8 & 255) / 255 << 8)
        | ((a & 255) * (b & 255) / 255);
  }
}
