package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.client.impact.ImpactManager;
import com.cappleapple.combattraces.client.trail.TrailManager;
import com.cappleapple.combattraces.config.ClientConfig;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.client.event.*;

public final class DebugRenderer {
  public static boolean enabled = Boolean.getBoolean("combattraces.validate");

  private DebugRenderer() {}

  public static boolean active() {
    return enabled || ClientConfig.DEBUG.get();
  }

  public static void render(RenderLevelStageEvent event) {
    if (!active() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    var mc = Minecraft.getInstance();
    var camera = event.getCamera().getPosition();
    var poses = new PoseStack();
    var buffers = mc.renderBuffers().bufferSource();
    var out = buffers.getBuffer(RenderType.lines());
    double now = VisualClock.now();
    for (var sample : HeldItemCapture.LATEST.values()) {
      if (now - sample.time() > 0.3) continue;
      marker(poses, out, sample.origin().subtract(camera), 0, 1, 0);
      marker(poses, out, sample.tip().subtract(camera), 1, .2f, 0);
      line(poses, out, sample.origin().subtract(camera), sample.tip().subtract(camera), 1, 1, 0);
    }
    for (var trail : TrailManager.trails())
      for (int i = 1; i < trail.history.size(); i++)
        line(
            poses,
            out,
            trail.history.get(i - 1).tip().subtract(camera),
            trail.history.get(i).tip().subtract(camera),
            .3f,
            .75f,
            1);
    if (DebugState.state != null && mc.player != null) {
      var sample = HeldItemCapture.LATEST.get(mc.player.getId());
      if (sample != null)
        line(
            poses,
            out,
            sample.tip().subtract(camera),
            sample.tip().add(DebugState.state.tipVelocity().scale(.06)).subtract(camera),
            0,
            1,
            1);
    }
    if (DebugState.impact != null) {
      var ctx = DebugState.impact;
      marker(poses, out, ctx.position().subtract(camera), 1, 0, 1);
      line(
          poses,
          out,
          ctx.position().subtract(camera),
          ctx.position().add(ctx.normal().scale(.65)).subtract(camera),
          1,
          0,
          1);
      line(
          poses,
          out,
          ctx.position().subtract(camera),
          ctx.position().add(ctx.tangent().scale(.65)).subtract(camera),
          0,
          1,
          1);
    }
    buffers.endBatch(RenderType.lines());
  }

  public static void hud(RenderGuiEvent.Post event) {
    if (!active()) return;
    var mc = Minecraft.getInstance();
    var lines = new ArrayList<String>();
    lines.add(
        "Combat Traces | trails "
            + TrailManager.trails().size()
            + " | samples "
            + TrailManager.trails().stream().mapToInt(t -> t.history.size()).sum()
            + " | impacts "
            + ImpactManager.active().size());
    if (DebugState.weapon != null)
      lines.add(
          "Class: "
              + DebugState.weapon.weaponClass()
              + " | emitters: "
              + DebugState.weapon.emitters().size()
              + " | elements: "
              + (DebugState.weapon.elements().isEmpty()
                  ? "physical"
                  : DebugState.weapon.elements()));
    if (DebugState.motion != null)
      lines.add(
          "Animation: "
              + DebugState.motion.animation()
              + " | "
              + String.format(Locale.ROOT, "%.2f", DebugState.motion.progress())
              + " | "
              + DebugState.motion.hand());
    if (DebugState.state != null)
      lines.add(
          "Motion: "
              + DebugState.state.type()
              + " | speed: "
              + String.format(Locale.ROOT, "%.1f", DebugState.state.speed())
              + " blocks/s");
    if (DebugState.impact != null)
      lines.add(
          "Material: "
              + DebugState.impact.material()
              + " | strength: "
              + String.format(Locale.ROOT, "%.2f", DebugState.impact.strength())
              + " | blocked: "
              + DebugState.impact.blocked());
    int y = 6;
    for (var line : lines) {
      event.getGuiGraphics().fill(4, y - 2, mc.font.width(line) + 10, y + 10, 0xa0000000);
      event.getGuiGraphics().drawString(mc.font, line, 7, y, 0xffedecdd);
      y += 13;
    }
  }

  private static void marker(
      PoseStack pose, VertexConsumer out, Vec3 p, float r, float g, float b) {
    LevelRenderer.renderLineBox(
        pose, out, new AABB(p.subtract(.025, .025, .025), p.add(.025, .025, .025)), r, g, b, 1);
  }

  private static void line(
      PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, float r, float g, float blue) {
    Vec3 n = b.subtract(a).normalize();
    if (n.lengthSqr() < 1e-8) return;
    out.addVertex(pose.last(), (float) a.x, (float) a.y, (float) a.z)
        .setColor(r, g, blue, 1)
        .setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
    out.addVertex(pose.last(), (float) b.x, (float) b.y, (float) b.z)
        .setColor(r, g, blue, 1)
        .setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
  }
}
