# Extension API

Package: `com.cappleapple.combattraces.api`.

Register providers during client setup on the main thread. All motion, model, particle, and renderer access remains on the client's main/render thread. Common API types do not reference Minecraft client renderer classes.

## Motion providers

```java
CombatTracesApi.registerMotionProvider(100, (entity, partialTick) -> {
    // Return your integration's current animation state, or Optional.empty().
    return yourIntegration.activeMotion(entity, partialTick);
});
```

Providers with higher priority are queried first. Better Combat registers at priority 0.

A `CombatMotion` contains an attack identity, held stack, hand, animation ID, normalized animation progress, hit-timing hint, combo information, damage multiplier, category, and pose metadata. Use a new attack ID for each distinct attack. Attack-end/cancel should return empty.

The included renderer automatically captures held items rendered through Minecraft's `ItemInHandRenderer` / `ItemRenderer` path. Custom renderers with extra animated bones must supply suitable emitters or their own transform-aware integration.

## Classification and emitter providers

- `registerWeaponClassifier(WeaponClassifier)`: returns an optional built-in `WeaponClass`.
- `registerEmitterProvider(TrailEmitterProvider)`: returns a list of centered model-space emitters; an empty list allows the next resolver.
- `TrailEmitterModel`: optional interface implemented by a custom baked model to expose emitter metadata directly.

Explicit datapack mappings take precedence. Provider registration order is used within each extension list.

## Elements and impact modifiers

- `registerElementProvider(ElementProvider)`: returns active element IDs from custom components, Curios state, spells, or another mod.
- `registerImpactModifier(ImpactModifier)`: transforms an immutable `ImpactContext` before composition.
- `submitImpact(ImpactContext)`: emits a cosmetic impact supplied by another combat provider.

An impact context includes the attacker, required live entity target, motion snapshot, world-space contact position/tangent/normal, visual speed/strength, weapon family, motion type, element IDs, material ID, and critical/blocked flags.

Build the context on the client main/render thread. Submission dispatches to that thread; do not read live entities from worker threads. Null, removed, and stale-world targets are rejected before composition. Do not use this API to apply gameplay damage. The renderer still applies its distance and effect budgets. `installImpactSink` is a bootstrap hook owned by Combat Traces and should not be replaced by integrations.

## Integration boundaries

`compat/bettercombat/` supplies active attack state and the client-only fallback hit candidates. Conditional mixins read Better Combat's already-synchronized animation lifecycle and Player Animator's active clock; no reflection or animation-name lookup table is involved.

The public classifier uses broad categories and registered extensions. Hit confirmation packets carry only attacker ID, target ID, damage/critical/blocked metadata, and a game tick. They never carry per-frame transforms, animation paths, or client-provided gameplay changes.

API consumers should be client-gated. Use `ModList.get().isLoaded("combattraces")` before referencing optional integration classes, or put integration code in a separately gated class.

## Attack-volume hints and threading

CombatMotion now optionally accepts AttackShape.UNKNOWN, SWEEP, or FORWARD. The original constructor remains available and defaults to UNKNOWN. These are gesture hints; emitter geometry and animation paths are still sampled from the rendered weapon. A forward hitbox alone never converts a blunt impact into a stab.

submitImpact dispatches work onto the client thread and rejects stale-world contexts. Construct a stable context before submitting; do not read live Minecraft world state from particle worker threads. Providers are queried from the normal client rendering lifecycle.

CombatMotion additionally accepts an optional world-space `hitboxCenter`. Supply the center of the current attack volume for vertical impact placement. Both older constructors remain available and use null, preserving the existing contact-height fallback for providers without volume metadata. No additional hitbox data is sent in gameplay packets.

The full constructor additionally accepts `AttackHitbox(center, size, widthAxis, heightAxis, depthAxis)`. Size is the full local width/height/depth in block units, while the center and unit axes use world space. Supply dimensions before yaw/pitch rotation. All three older constructors remain available and leave the full hitbox null. Invalid or missing geometry falls back to the previous motion inference; valid geometry governs non-blunt impact selection. Blunt weapon classification always wins.
