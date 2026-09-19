package com.cappleapple.combattraces.mixin.bettercombat;

import com.cappleapple.combattraces.api.CombatMotion;
import com.cappleapple.combattraces.compat.bettercombat.BetterCombatState;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import java.util.Optional;
import net.bettercombat.logic.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after Better Combat's mixin, whose synchronized animation lifecycle covers remote players
 * too.
 */
@Mixin(value = AbstractClientPlayer.class, priority = 900)
public abstract class BetterCombatPlayerMixin implements BetterCombatState {

  @Unique private CombatMotion combatTraces$attack;
  @Unique private long combatTraces$sequence;
  @Unique private com.cappleapple.combattraces.api.SwingWindow combatTraces$swingWindow;
  @Unique private net.bettercombat.api.WeaponAttributes.Attack combatTraces$geometry;

  @Dynamic("Added by Better Combat")
  @Inject(method = "playAttackAnimation", at = @At("RETURN"), remap = false)
  private void combatTraces$start(
      String name, AnimatedHand hand, float length, float upswing, CallbackInfo ci) {
    var player = (AbstractClientPlayer) (Object) this;
    int combo = ((PlayerAttackProperties) player).getComboCount();
    var attack = PlayerAttackHelper.getCurrentAttack(player, combo);
    var weapon = hand.isOffHand() ? player.getOffhandItem() : player.getMainHandItem();
    var selected = attack == null ? null : attack.attack();
    if (attack != null && !name.equals(selected.animation()))
      for (var candidate : attack.attributes().attacks())
        if (name.equals(candidate.animation())) {
          selected = candidate;
          break;
        }
    var shape =
        selected == null || !name.equals(selected.animation())
            ? com.cappleapple.combattraces.api.AttackShape.UNKNOWN
            : selected.hitbox() == net.bettercombat.api.WeaponAttributes.HitBoxShape.FORWARD_BOX
                ? com.cappleapple.combattraces.api.AttackShape.FORWARD
                : com.cappleapple.combattraces.api.AttackShape.SWEEP;
    combatTraces$geometry = selected != null && name.equals(selected.animation()) ? selected : null;
    // Better Combat's passed upswing is cooldown time; our progress uses keyframe time.
    float contact = (float) net.bettercombat.BetterCombatMod.config.getUpswingMultiplier();
    var animation = ((AttackStackAccessor) this).combatTraces$stack().base.getAnimation();
    combatTraces$swingWindow =
        animation instanceof KeyframeAnimationPlayer keyframes
            ? com.cappleapple.combattraces.compat.bettercombat.BetterCombatSwingTiming.resolve(
                keyframes.getData(),
                contact,
                combatTraces$geometry != null && combatTraces$geometry.angle() > 180)
            : com.cappleapple.combattraces.api.SwingWindow.around(contact);
    combatTraces$attack =
        new CombatMotion(
            ++combatTraces$sequence,
            weapon.copy(),
            hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
            ResourceLocation.parse(name),
            0,
            contact,
            attack == null ? combo : attack.combo().current(),
            attack == null ? 1 : attack.combo().total(),
            selected == null ? 1 : selected.damageMultiplier(),
            attack == null ? "" : attack.attributes().category(),
            attack == null ? "" : attack.attributes().pose(),
            shape);
  }

  @Dynamic("Added by Better Combat")
  @Inject(method = "stopAttackAnimation", at = @At("HEAD"), remap = false)
  private void combatTraces$stop(float length, CallbackInfo ci) {
    combatTraces$attack = null;
  }

  @Override
  public Optional<CombatMotion> combatTraces$motion(float partialTick) {
    var m = combatTraces$attack;
    if (m == null
        || !(((AttackStackAccessor) this).combatTraces$stack().base.getAnimation()
            instanceof KeyframeAnimationPlayer player)
        || !player.isActive()
        || player.getTick() > player.getData().stopTick) return Optional.empty();
    // Keep the post-end clock for boundary interpolation; clamping would label a recovery pose
    // as the authored final pose when the render frame skips over endTick.
    float progress =
        Math.max(
            0,
            (player.getTick() + ((AnimationClockAccessor) player).combatTraces$partialTick())
                / Math.max(1, player.getData().endTick));
    var hitbox =
        com.cappleapple.combattraces.compat.bettercombat.BetterCombatGeometry.hitbox(
            (AbstractClientPlayer) (Object) this, m.weapon(), combatTraces$geometry);
    return Optional.of(
        new CombatMotion(
            m.attackId(),
            m.weapon(),
            m.hand(),
            m.animation(),
            progress,
            m.hitProgress(),
            m.comboIndex(),
            m.comboLength(),
            m.damageMultiplier(),
            m.category(),
            m.pose(),
            m.shape(),
            hitbox == null ? null : hitbox.center(),
            hitbox,
            combatTraces$swingWindow));
  }
}
