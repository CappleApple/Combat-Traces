# Combat Traces

Motion-derived weapon ribbons and directional combat impacts for **Minecraft 1.21.1 / NeoForge 21.1.248+ / Java 21**.

- Mod ID: `combattraces`
- Java namespace: `com.cappleapple.combattraces`
- Version: `1.0.3`
- Tested integration: **Better Combat 2.4.0 + Player Animator 2.0.4**
- License: MIT

Combat Traces samples the held item's actual rendered matrix **after Player Animator and the item model's display transform**. It builds temporary ribbon geometry between blade samples. Custom attack animations need no Combat Traces path definition.

## Install

Download [Combat Traces 1.0.3](https://github.com/CappleApple/Combat-Traces/releases/download/v1.0.3/combattraces-1.0.3.jar) and place it in your instance's `mods/` folder. Install Better Combat and its required Player Animator and Cloth Config dependencies to use the included motion provider.

The mod safely loads without Better Combat; it then waits for another registered motion provider. Better Combat integration is isolated in `compat/bettercombat` and conditionally enabled client mixins.

Install Combat Traces on the server too for **confirmed, attributed entity impacts**, shield/critical metadata, and synchronized datapack definitions. Clients without Combat Traces can still connect. No weapon-transform packets are sent.

On a server without Combat Traces, local hit candidates can produce approximate impacts when vanilla hurt feedback arrives. Remote players' synchronized animations still produce trails; reliable remote hit attribution requires the optional server component.

## Included

- Frame-sampled ribbons, adaptive subdivisions, fading, scrolling/flipbook textures, alpha-weighted additive and translucent styles.
- Model-geometry emitter inference, optional item-model metadata, configurable multi-emitter weapons.
- Slash, cleave, blunt, pierce, claw, whip, magic/generic impact families.
- Pixel-art hit flashes with target depth priority, distinct blunt bursts, and detected sword-stab punctures.
- Full blade and hammer-head ribbon inference from visible model geometry.
- Hitbox-driven stab/slash selection and horizontal/vertical cut orientation with small, stable variation. Blunt impacts remain distinct.
- Entity-only hit effects placed at Better Combat's hitbox-center height, with short-lived entity-relative attachment.
- Configurable minimum swing speed and windup suppression for weapon trails.
- Composited fire, frost, lightning, poison, arcane, holy, and shadow overlays. Fire Aspect selects fire automatically.
- Configurable entity material and armor responses, including shields. Terrain contacts do not create hit effects.
- First-person opacity/width controls, camera guard, distance LOD, global sample/effect/particle limits.
- Datapack reload/synchronization, resource-pack textures, debug geometry/HUD, client configuration screen, public extension API.

## Build and run

Use Java 21. The Gradle wrapper downloads the pinned build and development dependencies.

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutBetterCombat
```

The artifact is `build/libs/combattraces-1.0.3.jar`. Better Combat, Player Animator, Cloth Config, development tests, and the validation animation are **not bundled**.

Development-only acceptance runs create disposable flat worlds in `run-client/` and close the client when finished:

```powershell
.\gradlew.bat runClient -PvalidateClient
.\gradlew.bat runClient -PwithoutBetterCombat -PvalidateEmptyClient
python tools/server_smoke.py
```

The smoke script uses `run-server/`, binds only to loopback on ports 25598/25599, writes its isolated EULA/config, tests reload and restart, then shuts down through RCON. It uses Java 21 from `JAVA_HOME` or `PATH`.

## Configure

Use the NeoForge Mods configuration screen or `config/combattraces-client.toml`.

- [Client settings](docs/CONFIGURATION.md)
- [Pack definitions and examples](docs/DATAPACKS.md)
- [Integration API](docs/API.md)
- [Validation results and manual checklist](docs/TESTING.md)
- [Simply Swords and AsyncParticles checks](docs/COMPATIBILITY.md)
- [Implementation and known limits](docs/ARCHITECTURE.md)

Commands:

```text
/combattraces debug on
/combattraces debug off
/combattraces reload
/reload
```

The first three are client commands. `/combattraces reload` reloads client resources and model caches; the server's normal `/reload` refreshes datapack mappings and sends the completed snapshot to connected clients.

## Pack examples

`examples/combattraces-example-datapack.zip` is installable in a world's `datapacks/` folder. It demonstrates a deliberately double-ended diamond-sword emitter override, combined fire/lightning, a custom trail style, and a material mapping. It is optional and does not change damage or recipes.

`examples/combattraces-example-resourcepack.zip` is installable in `resourcepacks/`. It demonstrates replacing the default slash texture. Neither example is automatically installed.

## Visual evidence

The implementation was exercised in an actual development client, including arbitrary custom animation, first-person and third-person capture, a rendered `RemotePlayer`, entity hits, Fire Aspect, live datapack reload, and multi-emitter rendering. See [the test record](docs/TESTING.md) for exactly what was checked and the remaining manual scenarios.

The included textures were generated as a single pixel-art VFX atlas using the built-in image tool, then extracted into replaceable PNG tiles. The source atlas and prompt are retained under `art/`.

Optional asset/schema verification: `python -m pip install -r tools/requirements.txt`, then `python tools/check_assets.py`.
