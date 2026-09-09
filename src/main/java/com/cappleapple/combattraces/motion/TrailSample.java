package com.cappleapple.combattraces.motion;

import net.minecraft.world.phys.Vec3;

/** Doubles preserve sub-pixel weapon movement far from the world origin. Time is seconds. */
public record TrailSample(Vec3 origin, Vec3 tip, double time, float progress) {}
