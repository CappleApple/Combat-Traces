package com.cappleapple.combattraces.client;

import java.util.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class FrameState {
  private static final Set<Long> CAPTURED = new HashSet<>();
  private static CameraType cameraType;
  private static int cameraEntity = -1;

  public static void begin(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
    CAPTURED.clear();
    var mc = Minecraft.getInstance();
    var type = mc.options.getCameraType();
    int entity = mc.getCameraEntity() == null ? -1 : mc.getCameraEntity().getId();
    if (cameraType != type || cameraEntity != entity) ClientState.clearEffects();
    cameraType = type;
    cameraEntity = entity;
  }

  public static boolean capture(int entity, boolean left) {
    return CAPTURED.add(((long) entity << 1) | (left ? 1 : 0));
  }
}
