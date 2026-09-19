package com.cappleapple.combattraces.compat.bettercombat;

import com.cappleapple.combattraces.api.CombatTracesApi;
import com.cappleapple.combattraces.client.*;
import com.cappleapple.combattraces.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;

/** Only replace authored particles when this client can capture this player's current attack. */
public final class BetterCombatTrails {
  public static long suppressedSpawns;

  private BetterCombatTrails() {}

  public static boolean replaces(AbstractClientPlayer player, boolean offhand) {
    var mc = Minecraft.getInstance();
    if (!ClientConfig.REPLACE_BETTER_COMBAT.get()
        || !ClientConfig.TRAILS.get()
        || ClientConfig.QUALITY.get() == 0
        || player.level() != mc.level) return false;
    boolean first = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    boolean visible =
        player == mc.player
            ? (first ? ClientConfig.FIRST_PERSON.get() : ClientConfig.THIRD_PERSON.get())
            : ClientConfig.OTHERS.get();
    if (!visible
        || player.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())
            > Math.pow(Math.min(ClientConfig.DISTANCE.get(), ClientConfig.TRAIL_DISTANCE.get()), 2))
      return false;
    var motion = CombatTracesApi.motion(player, 0).orElse(null);
    var hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    if (motion == null || motion.hand() != hand) return false;
    var observed = WeaponMotionTracker.latest(player.getId(), hand, 0);
    return observed != null
        && observed.motion().attackId() == motion.attackId()
        && VisualClock.now() - observed.sample().time() < .2;
  }
}
