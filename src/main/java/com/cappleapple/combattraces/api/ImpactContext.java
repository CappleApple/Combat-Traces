package com.cappleapple.combattraces.api;

import com.cappleapple.combattraces.motion.MotionAnalysis;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** All vectors use world space. A live entity target is required to display an impact. */
public record ImpactContext(
    LivingEntity attacker,
    Entity target,
    CombatMotion motion,
    Vec3 position,
    Vec3 tangent,
    Vec3 normal,
    double velocity,
    float strength,
    WeaponClass weaponClass,
    MotionAnalysis.Type motionType,
    List<ResourceLocation> elements,
    ResourceLocation material,
    boolean critical,
    boolean blocked) {}
