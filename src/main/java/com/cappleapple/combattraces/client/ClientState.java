package com.cappleapple.combattraces.client;

import com.cappleapple.combattraces.client.impact.*;
import com.cappleapple.combattraces.client.trail.TrailManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientState {
  private static ClientLevel level;

  public static void clearEffects() {
    DebugState.clear();
    HeldItemCapture.clear();
    WeaponMotionTracker.clear();
    TrailManager.clear();
    com.cappleapple.combattraces.client.trail.TrailAccents.clear();
    ImpactManager.clear();
    ImpactController.clear();
    ClientHitFallback.clear();
    com.cappleapple.combattraces.client.particle.AccentParticles.clear();
  }

  public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
    ClientDefinitions.disconnect();
    level = null;
  }

  public static void tick(ClientTickEvent.Post event) {
    var current = Minecraft.getInstance().level;
    if (current != level) {
      clearEffects();
      level = current;
    }
  }
}
