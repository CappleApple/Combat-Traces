# Compatibility checks

## Simply Swords

The development harness uses the installed **Simply Swords 1.70.2 for Minecraft 1.21.1**, Better Combat 2.4.0, and Player Animator 2.0.4. It loads the real item models and resolves animations from Better Combat's active attack definitions.

Covered weapons and combos:

- Diamond longsword: horizontal slash and stab finisher.
- Diamond claymore: inherited Better Combat cut, stab, and overhead slam.
- Diamond greathammer: horizontal swing and downward slam.

No Simply Swords item IDs, model coordinates, or animation paths are built into production emitter inference. Its models are validation fixtures only. Generated items use their extruded alpha-silhouette edges; transparent front/back texture corners are excluded. Crossguard detection locates the blade base, and a separate distal-head profile identifies hammer/mace heads. Explicit pack and provider emitters still take precedence.

## AsyncParticles

Checked against the installed **AsyncParticles 21.1.4.2**, with the user's configuration copied into an isolated development client: asynchronous particle ticking, GPU acceleration, and worker-side renderer updates remain enabled. The user's modpack files are not edited.

Combat Traces' ribbons and impact flashes are ordinary world-render geometry. They are not custom Particle subclasses and do not enter AsyncParticles' worker queues. Accent effects use registered vanilla particle providers.

A scoped tint is applied immediately before ParticleEngine.add. The normal createParticle/add lifecycle and other mods' hooks remain intact. Once a particle enters the engine, Combat Traces does not retain or mutate it. Effect maps, budgets, and rendering remain on the Minecraft client thread; API-submitted impacts are dispatched onto that thread.

The validation harness exercises every built-in accent family under repeated bursts, then reloads resources while AsyncParticles is active. This is evidence for the tested versions and settings, not a guarantee for every renderer, future release, or third-party particle provider.

Upstream background: [AsyncParticles source and compatibility notes](https://github.com/Harveykang/AsyncParticles). Implementation checks also inspected the installed JAR's particle-engine publication and GPU hooks.

## Reproduce the isolated compatibility run

Copy the actual installed test dependencies into a local directory (do not redistribute them):

- Simply Swords 1.70.2
- Architectury 13.0.11
- Fzzy Config 0.7.6
- Simply Tooltips 0.1.5
- Kotlin for Forge 5.12.0
- AsyncParticles 21.1.4.2

Better Combat, Player Animator, and Cloth Config come from the development Gradle dependencies.

```powershell
.\gradlew.bat runClient -PvalidateSimplySwords '-PsimplySwordsDir=.work/simplyswords-mods'
```

Results and recorded motions are written to run-client/simplyswords-validation.txt and run-client/simplyswords-validation-detail.txt. Screenshots include weapon ribbons, per-family hits, an opaque-target depth comparison, and the particle stress scene. Test code and all dependency JARs are excluded from the production artifact.

The 1.0.2 rerun passed 109 checks with the same AsyncParticles settings. It also checks attack-volume height at three aim pitches, retained incoming rotation, entity-only impacts, and windup suppression at a 4-block/second trail threshold. See [saved results](../validation/README.md).

The 1.0.3 rerun passed 126 checks with the same AsyncParticles settings. It verifies hitbox-proportion selection, dominant-axis orientation, bounded variation, and blunt exemptions alongside the earlier height, trail, depth, and reload checks. See [current results](../validation/README.md).
