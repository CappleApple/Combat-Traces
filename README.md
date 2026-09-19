# Combat Traces

Combat Traces adds weapon trails and directional hit effects that follow the motion of the weapon you are actually swinging.

Rather than defining a trail path for every attack animation, the mod samples the rendered item transform and builds pixel-stepped sweeps from that motion, including diagonal and overhead cuts. With Better Combat, this means custom Player Animator attacks can usually produce trails without a Combat Traces-specific animation file.

Built for Minecraft 1.21.1 / NeoForge 21.1.248+.

## What it adds

- Motion-following weapon trails for swords, axes, hammers, polearms, twinblades, and other custom models.
- Separate trails for both striking ends of twinblades, warglaives, and quarterstaffs, plus chakram rim trails.
- Strike-phase trails with a configurable starting speed that remain continuous as the weapon slows.
- Geometry-based blade/head inference with datapack overrides when automatic detection is not enough.
- Directional slash, cleave, blunt, pierce, claw, whip, and generic/magic impact styles.
- Element overlays for fire, frost, lightning, poison, arcane, holy, and shadow that follow the same generated geometry as the weapon trail.
- Material/armor responses for entity impacts, including shields.
- First- and third-person rendering with configurable width/opacity and distance LOD.
- Resource-packable impact textures and optional textured ribbon styles.
- Datapack-driven weapon, material, element, and effect mappings.
- A public API for other animation/combat systems to provide motion and impact data.

Terrain contact does not create hit effects; impacts are tied to entity hits.

## Better Combat

The included motion provider targets Better Combat + Player Animator.

Combat Traces reads the held item's rendered transform after the animation and item display transform have been applied. The result is that a datapack/custom Better Combat swing can naturally change the trail shape without needing a second set of trail coordinates.

Better Combat is optional at load time. Without it, Combat Traces waits for another registered motion provider.

## Server support

Combat Traces can be used client-side for trails and approximate local impacts, but installing it on the server enables the most reliable hit information:

- confirmed entity-hit attribution;
- shield/critical metadata; and
- synchronized datapack definitions.

Clients without Combat Traces can still join a server that has it. Weapon transforms are not streamed over the network; each client renders trails from the animation it already sees.

## Weapon geometry

For ordinary models, Combat Traces can infer useful emitter lines from the visible item geometry. Swords can trace the blade, while hammer-like weapons can use the head rather than treating the entire item as a blade.

Twinblade, warglaive, and quarterstaff types infer both striking ends; chakrams infer eight segments around the model silhouette. Packs can override the automatic result or define multiple emitters for unusual weapons.

This is especially useful for twinblades, double-ended weapons, asymmetric models, or items whose visual shape does not match their registry/category name.

## Impacts

Confirmed entity hits choose an impact family using weapon/model information and the attack direction. Stabs can produce puncture-style effects, horizontal/vertical cuts orient to the swing, and blunt weapons keep a visibly different burst.

Effects are short-lived and can follow the struck entity briefly instead of remaining frozen at a world coordinate while the target moves.

Fire Aspect automatically selects the fire element. Other element/material mappings can be supplied through datapacks.

## Configuration

Use the NeoForge Mods config screen or edit:

```text
config/combattraces-client.toml
```

Useful references:

- [Client settings](docs/CONFIGURATION.md)
- [Datapack format and examples](docs/DATAPACKS.md)
- [Public integration API](docs/API.md)
- [Compatibility notes](docs/COMPATIBILITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Testing and QA](docs/TESTING.md)

Commands:

```text
/combattraces debug on
/combattraces debug off
/combattraces reload
/reload
```

`/combattraces reload` refreshes client resources/model caches. The server's normal `/reload` refreshes synchronized datapack mappings.

## Pack examples

The repository includes optional example packs:

```text
examples/combattraces-example-datapack.zip
examples/combattraces-example-resourcepack.zip
```

They demonstrate a double-ended emitter, combined elements, a custom trail style, material mapping, and texture replacement for styles using `trail_geometry: "textured"`. Generated trails do not sample ribbon textures. They are examples only and are not installed automatically.

The source art for the bundled pixel-style effects is kept under `art/` so the shipped textures can be reproduced or replaced without treating the PNGs as unexplained generated assets.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.248 or newer compatible 21.1 build
- Java 21
- Better Combat / Player Animator for the included motion provider

Better Combat itself is optional if another mod registers a motion provider through the API.

## Building

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutBetterCombat
```

The 1.1.0 release JAR is written to `build/libs/combattraces-1.1.0.jar`.

Local `runClient` launches are muted and block mouse capture. These development controls are excluded from the release JAR.

Development validation tasks and third-party development dependencies are not bundled in the release artifact.

## License

Combat Traces is licensed under [CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). Modpacks and Minecraft servers, including monetized ones, may use it under the additional permission in the LICENSE.
