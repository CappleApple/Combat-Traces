package com.cappleapple.combattraces.api;

import net.minecraft.world.phys.Vec3;

/** Model-space points in block units, before the baked model's display transform. */
public record TrailEmitter(Vec3 origin, Vec3 tip) {}
