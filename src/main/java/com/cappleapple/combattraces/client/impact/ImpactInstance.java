package com.cappleapple.combattraces.client.impact;

import com.cappleapple.combattraces.api.ImpactContext;
import com.cappleapple.combattraces.data.EffectStyle;
import net.minecraft.world.phys.Vec3;

public record ImpactInstance(
    ImpactContext context, EffectStyle style, double created, float size, Vec3 localPosition) {
  public Vec3 position(float partial) {
    var target = context.target();
    return target == null || target.isRemoved()
        ? context.position()
        : target.getPosition(partial).add(localPosition);
  }
}
