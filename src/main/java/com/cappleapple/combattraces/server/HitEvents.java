package com.cappleapple.combattraces.server;

import com.cappleapple.combattraces.network.HitPayload;
import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "combattraces")
public final class HitEvents {
  private record Critical(int target, long tick) {}

  private static final Map<LivingEntity, Critical> CRITS = new WeakHashMap<>();
  private static final Map<LivingEntity, Critical> BLOCKS = new WeakHashMap<>();

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void critical(CriticalHitEvent event) {
    if (!event.getEntity().level().isClientSide() && event.isCriticalHit())
      CRITS.put(
          event.getEntity(),
          new Critical(event.getTarget().getId(), event.getEntity().level().getGameTime()));
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void blocked(LivingShieldBlockEvent event) {
    if (event.getEntity().level().isClientSide()
        || !event.getBlocked()
        || event.getBlockedDamage() <= 0) return;
    if (event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker) {
      BLOCKS.put(attacker, new Critical(event.getEntity().getId(), attacker.level().getGameTime()));
      send(event.getEntity(), event.getDamageSource(), event.getBlockedDamage(), true);
    }
  }

  @SubscribeEvent
  public static void damage(LivingDamageEvent.Post event) {
    if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0) return;
    var prior = BLOCKS.get(event.getSource().getDirectEntity());
    if (prior != null
        && prior.target == event.getEntity().getId()
        && prior.tick == event.getEntity().level().getGameTime()) return;
    send(event.getEntity(), event.getSource(), event.getNewDamage(), event.getBlockedDamage() > 0);
  }

  private static void send(
      LivingEntity target, DamageSource source, float damage, boolean blocked) {
    if (!(target.level() instanceof ServerLevel level)
        || !(source.getDirectEntity() instanceof LivingEntity attacker)) return;
    var crit = CRITS.get(attacker);
    boolean critical =
        crit != null && crit.target == target.getId() && crit.tick == level.getGameTime();
    var packet =
        new HitPayload(
            attacker.getId(),
            target.getId(),
            Math.clamp(damage, 0, 1024),
            critical,
            blocked,
            level.getGameTime());
    for (var player : level.players())
      if (player.distanceToSqr(target) < 128 * 128 && player.connection.hasChannel(HitPayload.TYPE))
        PacketDistributor.sendToPlayer(player, packet);
  }
}
