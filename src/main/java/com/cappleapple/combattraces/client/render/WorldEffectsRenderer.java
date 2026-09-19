package com.cappleapple.combattraces.client.render;

import com.cappleapple.combattraces.client.VisualClock;
import com.cappleapple.combattraces.client.trail.TrailManager;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class WorldEffectsRenderer {
  private static final ByteBufferBuilder STORAGE = new ByteBufferBuilder(262144);
  private static final MultiBufferSource.BufferSource BUFFERS =
      MultiBufferSource.immediate(STORAGE);

  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
      // AFTER_LEVEL is outside LevelRenderer's camera-matrix scope.
      var modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
      boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
      float[] color = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
      modelView.pushMatrix();
      modelView.mul(event.getModelViewMatrix());
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
      try {
        net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        if (com.cappleapple.combattraces.config.ClientConfig.IMPACT_DEPTH_PRIORITY.get())
          com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        double now = VisualClock.now();
        com.cappleapple.combattraces.client.impact.ImpactManager.prune(now);
        ImpactRenderer.render(
            BUFFERS,
            event.getCamera().getPosition(),
            now,
            event.getPartialTick().getGameTimeDeltaPartialTick(false));
        BUFFERS.endBatch();
      } finally {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(
            color[0], color[1], color[2], color[3]);
        modelView.popMatrix();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        if (depth) com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        else com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
      }
      return;
    }
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    double now = VisualClock.now();
    TrailManager.prune(now);
    RibbonRenderer.render(BUFFERS, event.getCamera().getPosition(), now);
    com.cappleapple.combattraces.client.WeaponMotionTracker.prune(now);
    com.cappleapple.combattraces.client.HeldItemCapture.prune(now);
    BUFFERS.endBatch();
    com.cappleapple.combattraces.client.particle.AccentParticles.drain(now);
  }
}
