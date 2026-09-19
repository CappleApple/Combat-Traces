package com.cappleapple.combattraces.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Immutable attack snapshot. Progress is animation time, not wall-clock attack cooldown. */
public record CombatMotion(
    long attackId,
    ItemStack weapon,
    InteractionHand hand,
    ResourceLocation animation,
    float progress,
    float hitProgress,
    int comboIndex,
    int comboLength,
    double damageMultiplier,
    String category,
    String pose,
    AttackShape shape,
    Vec3 hitboxCenter,
    AttackHitbox hitbox,
    SwingWindow swingWindow) {
  /** Retains the full-hitbox constructor used by existing motion providers. */
  public CombatMotion(
      long attackId,
      ItemStack weapon,
      InteractionHand hand,
      ResourceLocation animation,
      float progress,
      float hitProgress,
      int comboIndex,
      int comboLength,
      double damageMultiplier,
      String category,
      String pose,
      AttackShape shape,
      Vec3 hitboxCenter,
      AttackHitbox hitbox) {
    this(
        attackId,
        weapon,
        hand,
        animation,
        progress,
        hitProgress,
        comboIndex,
        comboLength,
        damageMultiplier,
        category,
        pose,
        shape,
        hitboxCenter,
        hitbox,
        null);
  }

  /** Retains the center-only constructor used by existing motion providers. */
  public CombatMotion(
      long attackId,
      ItemStack weapon,
      InteractionHand hand,
      ResourceLocation animation,
      float progress,
      float hitProgress,
      int comboIndex,
      int comboLength,
      double damageMultiplier,
      String category,
      String pose,
      AttackShape shape,
      Vec3 hitboxCenter) {
    this(
        attackId,
        weapon,
        hand,
        animation,
        progress,
        hitProgress,
        comboIndex,
        comboLength,
        damageMultiplier,
        category,
        pose,
        shape,
        hitboxCenter,
        null);
  }

  /** Providers without attack geometry retain the existing contact-height fallback. */
  public CombatMotion(
      long attackId,
      ItemStack weapon,
      InteractionHand hand,
      ResourceLocation animation,
      float progress,
      float hitProgress,
      int comboIndex,
      int comboLength,
      double damageMultiplier,
      String category,
      String pose,
      AttackShape shape) {
    this(
        attackId,
        weapon,
        hand,
        animation,
        progress,
        hitProgress,
        comboIndex,
        comboLength,
        damageMultiplier,
        category,
        pose,
        shape,
        null);
  }

  /** Source-compatible constructor for providers that only supply motion. */
  public CombatMotion(
      long attackId,
      ItemStack weapon,
      InteractionHand hand,
      ResourceLocation animation,
      float progress,
      float hitProgress,
      int comboIndex,
      int comboLength,
      double damageMultiplier,
      String category,
      String pose) {
    this(
        attackId,
        weapon,
        hand,
        animation,
        progress,
        hitProgress,
        comboIndex,
        comboLength,
        damageMultiplier,
        category,
        pose,
        AttackShape.UNKNOWN);
  }
}
