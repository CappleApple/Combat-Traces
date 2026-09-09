package com.cappleapple.combattraces.client.impact;

import com.cappleapple.combattraces.client.VisualClock;
import com.cappleapple.combattraces.network.HitPayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Used only without the optional server channel. Hurt feedback confirms local candidate targets.
 */
public final class ClientHitFallback {
  private record Pending(int attacker, int target, int initialHurt, double expires, long tick) {}

  private static final List<Pending> PENDING = new ArrayList<>();

  public static void clear() {
    PENDING.clear();
  }

  public static void candidates(LivingEntity attacker, List<Entity> targets) {
    var mc = Minecraft.getInstance();
    if (mc.getConnection() == null || mc.getConnection().hasChannel(HitPayload.TYPE)) return;
    for (var target : targets)
      if (target instanceof LivingEntity living && PENDING.size() < 32)
        PENDING.add(
            new Pending(
                attacker.getId(),
                target.getId(),
                living.hurtTime,
                VisualClock.now() + 0.35,
                attacker.level().getGameTime()));
  }

  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) {
      clear();
      return;
    }
    double now = VisualClock.now();
    PENDING.removeIf(
        p -> {
          var target = mc.level.getEntity(p.target);
          if (target instanceof LivingEntity living && living.hurtTime > p.initialHurt) {
            ImpactController.receive(new HitPayload(p.attacker, p.target, 4, false, false, p.tick));
            return true;
          }
          return now > p.expires || target == null;
        });
  }
}
