package com.cappleapple.combattraces.validation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Muted, noncapturing local test clients. The class and its mixins are excluded from releases. */
@EventBusSubscriber(modid = "combattraces", value = Dist.CLIENT)
public final class DevelopmentClientSafety {
  public static long preventedMouseGrabs, preventedCursorChanges, preventedSounds;
  private static boolean reported;
  private static ClientLevel reportedLevel;

  private DevelopmentClientSafety() {}

  @SubscribeEvent
  public static void tick(ClientTickEvent.Pre event) {
    if (FMLLoader.isProduction() || !Boolean.getBoolean("combattraces.developmentSafeClient"))
      return;
    var mc = Minecraft.getInstance();
    mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0d);
    boolean captured = mc.mouseHandler.isMouseGrabbed();
    int cursor = GLFW.glfwGetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR);
    float volume = mc.options.getSoundSourceVolume(SoundSource.MASTER);
    if (captured || cursor != GLFW.GLFW_CURSOR_NORMAL || volume != 0)
      throw new IllegalStateException(
          "COMBAT TRACES CLIENT SAFETY FAIL: master="
              + volume
              + " mouseCaptured="
              + captured
              + " cursorMode="
              + cursor);
    if (!reported || mc.level != reportedLevel) {
      reported = true;
      reportedLevel = mc.level;
      System.out.println(
          "COMBAT TRACES CLIENT SAFETY PASS: master=0.0 mouseCaptured=false cursor=NORMAL scene="
              + (mc.level == null ? "menu" : "world")
              + " preventedMouseGrabs="
              + preventedMouseGrabs
              + " preventedCursorChanges="
              + preventedCursorChanges
              + " preventedSounds="
              + preventedSounds);
    }
  }
}
