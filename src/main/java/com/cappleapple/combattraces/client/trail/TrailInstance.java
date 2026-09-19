package com.cappleapple.combattraces.client.trail;

import com.cappleapple.combattraces.data.EffectStyle;
import com.cappleapple.combattraces.motion.*;
import net.minecraft.world.entity.LivingEntity;

public final class TrailInstance {
  public final LivingEntity owner;
  public final TrailHistory history;
  public final boolean firstPerson;
  public final EffectStyle style;
  public final long attack;
  public double touched;
  public boolean paused;
  public com.cappleapple.combattraces.api.WeaponClass family =
      com.cappleapple.combattraces.api.WeaponClass.GENERIC;
  public boolean thrust, enchanted;
  public net.minecraft.world.phys.Vec3 thrustAxis = net.minecraft.world.phys.Vec3.ZERO;

  public TrailInstance(
      LivingEntity owner, long attack, boolean firstPerson, EffectStyle style, int capacity) {
    this.owner = owner;
    this.attack = attack;
    this.firstPerson = firstPerson;
    this.style = style;
    history = new TrailHistory(capacity);
  }
}
