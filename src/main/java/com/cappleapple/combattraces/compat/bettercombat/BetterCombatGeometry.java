package com.cappleapple.combattraces.compat.bettercombat;

import com.cappleapple.combattraces.api.AttackHitbox;
import java.util.Comparator;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.AttackRangeExtensions;
import net.bettercombat.client.collision.*;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Mirrors TargetFinder's volume construction using its public geometry and reach APIs. */
public final class BetterCombatGeometry {
  private BetterCombatGeometry() {}

  public static Vec3 center(Player player, ItemStack weapon, WeaponAttributes.Attack attack) {
    var volume = hitbox(player, weapon, attack);
    return volume == null ? null : volume.center();
  }

  public static AttackHitbox hitbox(
      Player player, ItemStack weapon, WeaponAttributes.Attack attack) {
    if (attack == null || attack.hitbox() == null) return null;
    double range = PlayerAttackHelper.getRangeForItem(player, weapon) * attack.rangeMultiplier();
    var context = new AttackRangeExtensions.Context(player, range);
    for (var modifier :
        AttackRangeExtensions.sources().stream()
            .map(source -> source.apply(context))
            .sorted(Comparator.comparingInt(AttackRangeExtensions.Modifier::operationOrder))
            .toList()) {
      range =
          switch (modifier.operation()) {
            case ADD -> range + modifier.value();
            case MULTIPLY -> range * modifier.value();
          };
    }
    boolean spin = attack.angle() > 180;
    var size = WeaponHitBoxes.createHitbox(attack.hitbox(), range, spin);
    var box =
        new OrientedBoundingBox(
            TargetFinder.getInitialTracingPoint(player), size, player.getXRot(), player.getYRot());
    if (!spin) box.offsetAlongAxisZ(size.z / 2);
    return new AttackHitbox(box.center, size, box.axisX, box.axisY, box.axisZ);
  }
}
