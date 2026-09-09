package com.cappleapple.combattraces.client;

import net.minecraft.client.Minecraft;

/** Pausing freezes effects. Client world replacement resets all consumers. */
public final class VisualClock {
  private static double time, last = System.nanoTime() * 1e-9;

  public static double now() {
    double current = System.nanoTime() * 1e-9;
    if (!Minecraft.getInstance().isPaused()) time += Math.max(0, current - last);
    last = current;
    return time;
  }
}
