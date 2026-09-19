package com.cappleapple.combattraces.client.weapon;

import com.cappleapple.combattraces.api.*;
import com.cappleapple.combattraces.client.ClientDefinitions;
import com.cappleapple.combattraces.data.*;
import com.cappleapple.combattraces.motion.WeaponTopology;
import java.util.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class WeaponResolver {
  public record Resolved(
      WeaponClass weaponClass,
      List<TrailEmitter> emitters,
      List<ResourceLocation> elements,
      ResourceLocation trail,
      float impactScale) {}

  public static void clear() {
    ModelEmitters.clear();
  }

  public static WeaponRule override(ItemStack stack) {
    return ClientDefinitions.current.weapons().stream()
        .filter(r -> r.matches(stack))
        .findFirst()
        .orElse(null);
  }

  public static Resolved resolve(LivingEntity entity, CombatMotion motion, BakedModel model) {
    var rule = override(motion.weapon());
    var family = classify(entity, motion, model);
    List<TrailEmitter> emitters = rule == null ? List.of() : rule.emitters();
    if (emitters.isEmpty())
      for (var provider : CombatTracesApi.emitterProviders()) {
        emitters = provider.emitters(entity, motion);
        if (!emitters.isEmpty()) break;
      }
    if (emitters.isEmpty()) emitters = ModelEmitters.metadata(motion.weapon(), model);
    if (emitters.isEmpty())
      emitters =
          ModelEmitters.analyze(
                  motion.weapon(),
                  model,
                  family,
                  ModelEmitters.topology(motion.weapon(), motion.category()))
              .map(s -> s.emitters())
              .orElse(List.of());
    if (emitters.isEmpty()) emitters = List.of(fallback(family));
    ResourceLocation trail =
        rule != null && rule.trail() != null
            ? rule.trail()
            : id(
                switch (family) {
                  case BLUNT -> "blunt";
                  case CLEAVE -> "heavy_slash";
                  default -> "slash";
                });
    return new Resolved(
        family,
        emitters.stream().limit(32).toList(),
        com.cappleapple.combattraces.client.element.ElementResolver.resolve(
            entity, motion, rule == null ? List.of() : rule.elements()),
        trail,
        rule == null ? 1 : rule.impactScale());
  }

  public static WeaponClass classify(LivingEntity entity, CombatMotion motion, BakedModel model) {
    var stack = motion.weapon();
    var rule = override(stack);
    if (rule != null && rule.weaponClass() != null) return rule.weaponClass();
    for (var tag : ClientDefinitions.current.classifications())
      if (tag.priority() >= 0 && tag.matches(stack) && tag.weaponClass() != null)
        return tag.weaponClass();
    for (var classifier : CombatTracesApi.classifiers()) {
      var value = classifier.classify(entity, motion);
      if (value.isPresent()) return value.get();
    }
    var topology = ModelEmitters.topology(stack, motion.category());
    if (topology == WeaponTopology.DOUBLE_BLUNT) return WeaponClass.BLUNT;
    if (topology == WeaponTopology.DOUBLE_BLADE || topology == WeaponTopology.CIRCULAR)
      return WeaponClass.SLASH;
    String category = Objects.toString(motion.category(), "").toLowerCase(Locale.ROOT);
    if (category.contains("claymore") || category.contains("greatsword")) return WeaponClass.CLEAVE;
    if (category.contains("axe")) return WeaponClass.CLEAVE;
    if (category.contains("hammer") || category.contains("mace") || category.contains("staff"))
      return WeaponClass.BLUNT;
    if (category.contains("spear") || category.contains("rapier") || category.contains("trident"))
      return WeaponClass.PIERCE;
    if (category.contains("claw")) return WeaponClass.CLAW;
    if (category.contains("whip")) return WeaponClass.WHIP;
    if (category.contains("sword")
        || category.contains("katana")
        || category.contains("dagger")
        || category.contains("blade")) return WeaponClass.SLASH;
    for (var tag : ClientDefinitions.current.classifications())
      if (tag.priority() < 0 && tag.matches(stack) && tag.weaponClass() != null)
        return tag.weaponClass();
    if (stack.getItem() instanceof SwordItem) return WeaponClass.SLASH;
    if (stack.getItem() instanceof AxeItem) return WeaponClass.CLEAVE;
    if (stack.getItem() instanceof MaceItem) return WeaponClass.BLUNT;
    if (stack.getItem() instanceof TridentItem) return WeaponClass.PIERCE;
    if (model != null
        && ModelEmitters.analyze(stack, model).map(s -> s.aspect() > 2.4).orElse(false))
      return WeaponClass.SLASH;
    return WeaponClass.GENERIC;
  }

  private static TrailEmitter fallback(WeaponClass c) {
    return c == WeaponClass.PIERCE
        ? new TrailEmitter(new Vec3(0, 0.1, 0), new Vec3(0, 1.25, 0))
        : new TrailEmitter(new Vec3(-0.1, -0.1, 0), new Vec3(0.45, 0.45, 0));
  }

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath("combattraces", path);
  }
}
