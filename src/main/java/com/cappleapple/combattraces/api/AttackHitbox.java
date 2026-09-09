package com.cappleapple.combattraces.api;

import net.minecraft.world.phys.Vec3;

/** Full local width/height/depth with its center and unit axes in world space. */
public record AttackHitbox(
    Vec3 center, Vec3 size, Vec3 widthAxis, Vec3 heightAxis, Vec3 depthAxis) {}
