package com.cappleapple.combattraces.api;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;

public final class CombatTracesApi {
  private record Entry(int priority, CombatMotionProvider provider) {}

  private static final List<Entry> MOTION = new ArrayList<>();
  private static final List<WeaponClassifier> CLASSIFIERS =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static final List<TrailEmitterProvider> EMITTERS =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static final List<ElementProvider> ELEMENTS =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static final List<ImpactModifier> MODIFIERS =
      new java.util.concurrent.CopyOnWriteArrayList<>();

  public static void registerWeaponClassifier(WeaponClassifier provider) {
    CLASSIFIERS.add(Objects.requireNonNull(provider));
  }

  public static void registerEmitterProvider(TrailEmitterProvider provider) {
    EMITTERS.add(Objects.requireNonNull(provider));
  }

  public static void registerElementProvider(ElementProvider provider) {
    ELEMENTS.add(Objects.requireNonNull(provider));
  }

  public static void registerImpactModifier(ImpactModifier provider) {
    MODIFIERS.add(Objects.requireNonNull(provider));
  }

  public static List<WeaponClassifier> classifiers() {
    return List.copyOf(CLASSIFIERS);
  }

  public static List<TrailEmitterProvider> emitterProviders() {
    return List.copyOf(EMITTERS);
  }

  public static List<ElementProvider> elementProviders() {
    return List.copyOf(ELEMENTS);
  }

  public static ImpactContext modifyImpact(ImpactContext context) {
    for (var modifier : MODIFIERS) context = Objects.requireNonNull(modifier.modify(context));
    return context;
  }

  private static volatile java.util.function.Consumer<ImpactContext> impactSink = context -> {};

  public static void submitImpact(ImpactContext context) {
    impactSink.accept(Objects.requireNonNull(context));
  }

  public static void installImpactSink(java.util.function.Consumer<ImpactContext> sink) {
    impactSink = Objects.requireNonNull(sink);
  }

  private CombatTracesApi() {}

  public static synchronized void registerMotionProvider(
      int priority, CombatMotionProvider provider) {
    MOTION.add(new Entry(priority, Objects.requireNonNull(provider)));
    MOTION.sort(Comparator.comparingInt(Entry::priority).reversed());
  }

  public static Optional<CombatMotion> motion(LivingEntity entity, float partialTick) {
    for (var entry : MOTION) {
      var result = entry.provider.getActiveMotion(entity, partialTick);
      if (result.isPresent()) return result;
    }
    return Optional.empty();
  }
}
