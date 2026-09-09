# Validation record

## Automated and runtime checks

- Java 21 / NeoForge 21.1.248 build and **52 unit tests**.
- **5 GameTests** with Better Combat installed and the same **5 GameTests without Better Combat**.
- Dedicated server (1.0.0 baseline): fresh startup without Better Combat, normal `/reload`, save/stop, restart with Better Combat, reload, save/stop. Both reached `Done` and shut down cleanly.
- Actual development client: captured weapon positions and screenshots, verified custom animation clock/state, first/third person, a rendered `RemotePlayer`, Fire Aspect, an integrated-server-confirmed entity hit, loaded stone/wood/metal/ice tags, metal armor, a 25-emitter budget burst, live datapack enable/reload, and independent twin ribbons.
- Client startup without Better Combat passed. The final live harness passed **19 checks**, including an offhand animation at **20 FPS / 110 FOV**. See `validation/client-validation.txt`.
- The production JAR excludes validation classes, validation animations, and all third-party JARs.

The `RemotePlayer` scenario exercises the actual remote-player render/lifecycle path in one client. It is **not** a two-user network session or a production modpack compatibility test.

## 1.0.3 hitbox selection gate

The current Simply Swords / AsyncParticles client passed **126 checks**. Geometry comparisons cover the center, full dimensions, and all oriented axes at three aim pitches. Confirmed hits select stab/horizontal/vertical effects from volume proportions while both hammer attacks remain blunt. Per-hit sprite variation stays within seven degrees; depth priority, windup suppression, entity-only impacts, and particle reload checks also pass. See [current evidence](../validation/README.md).

New unit regressions cover absent/contradictory motion, yaw-rotated local dimensions, depth ties in spin boxes, invalid geometry, blunt exemptions, and bounded stable variation.

## 1.0.2 contact and trail baseline

The 1.0.2 Simply Swords / AsyncParticles harness passed **109 checks**, including the existing blade/head, depth, particle-thread, and resource-reload checks. Added checks compare attack centers at three aim pitches, verify incoming-direction rotation and entity-only impacts, reject early windup ribbons, and exercise claymore stab/overhead attacks. The speed threshold was set to 4 blocks/second. See [current evidence](../validation/README.md).

Unit regressions cover angled/short stabs, recovery direction retention, pure retractions, horizontal position preservation, screen projection, and configurable speed gating for explicit animation windows.

## 1.0.1 visual and compatibility baseline

The dedicated Simply Swords harness passed **54 checks** with Simply Swords 1.70.2 and AsyncParticles 21.1.4.2 installed. Its real Better Combat animations cover longsword slash/stab, claymore, greathammer swing, and greathammer slam. The model assertions use actual baked blade coordinates and ensure both hammer emitter endpoints lie on the 3-D head.

A framebuffer test places a sword cut behind an opaque iron golem. In the central target region it changes **0 pixels** with normal depth and **1011 pixels** with priority enabled. The check reads RGB channels directly and excludes the alpha channel from comparisons.

AsyncParticles worker ticking and GPU rendering are asserted active. Bursts exercise all built-in accent particle families before and after a resource reload, including **600 successfully created particles after reload**, with no observed particle-thread, rendering, or OpenGL errors. This is an isolated real client, not the full Within modpack or a multi-user session. See [compatibility scope and reproduction](COMPATIBILITY.md).

The 16 production sprites are all RGBA **32x32** textures, sampled without smoothing. The generated atlas and reproducible export are documented in [art notes](../art/PIXEL_ART.md).

## Reproduce

```powershell
.\gradlew.bat clean test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutBetterCombat
.\gradlew.bat runClient -PvalidateClient
.\gradlew.bat runClient -PwithoutBetterCombat -PvalidateEmptyClient
python tools/server_smoke.py
```

Client results are written to `run-client/client-validation.txt` and screenshots to `run-client/screenshots/`. Server smoke logs are under `.work/`. These runtime directories are ignored by Git.

## Phase gates

| Phase | Implementation | Validation |
|---|---|---|
| 1 | Provider, real item-matrix capture, debug endpoints | Compile, transform tests, actual animated sword screenshot and motion travel |
| 2 | Ribbon geometry, histories, adaptive sampling | Compile, bounds/discontinuity tests, actual ribbon rendering |
| 3 | Confirmed entity hits, oriented impact quads | Compile, orientation tests, integrated-server hit receipt and client screenshot |
| 4 | Classification, geometry inference, pack overrides | Compile, geometry/schema tests, client catalog and model inference |
| 5 | Element resolution and overlays | Compile, live enchanted Fire Aspect weapon |
| 6 | Material mappings and entity armor accents | Compile and loaded tag/armor client checks; terrain does not emit hits |
| 7 | LOD, budgets, first person, client-only fallback | Compile, both cameras, remote render path, budget burst |
| 8 | API, reload, debug/config UI, docs | Compile, live datapack synchronization/twin emitters, optionality and server gates |

## Manual release checklist

Use `/combattraces debug on` to inspect origins, tips, blade lines, sample paths, velocity, hit point/normal, family, elements, material, and strength.

### Motion and weapons

- [ ] Horizontal sword slash; upward/downward/overhead variants.
- [ ] Arbitrary diagonal animation from a real external pack.
- [ ] Spear/rapier thrust and puncture-style impact.
- [ ] Axe/cleave and hammer/mace blunt impacts visibly differ from sword impacts.
- [ ] Dagger, claw, whip, twinblade, asymmetric custom model.
- [ ] Left-handed player and offhand combos.
- [ ] Attack cancel, weapon swap, rapid combo restart, teleport, respawn, dimension change.

### Targets and composition

- [ ] Blade crosses terrain without emitting contact impacts; entity material and armor effects remain distinct.
- [ ] Moving entity impact remains attached for its short lifetime.
- [ ] Real shield block; partially blocked hit; critical modifier on/off.
- [ ] Fire + lightning; custom element; element and material toggles.
- [ ] No spurious entity effects after a miss or rejected/invulnerable hit.

### Camera and compatibility

- [ ] First/third person in the user's actual modpack and preferred FOV.
- [ ] Spectator camera observing another player.
- [ ] Custom camera, shader, renderer, and held-item replacement mods.
- [ ] Resource-pack texture replacement and resource reload during combat.
- [ ] Client with Combat Traces on a server without it; joining a modded server with unmodded clients.

### Multiplayer and performance

- [ ] Two real users see the same animation and correctly attributed hit.
- [ ] 1, 10, and 25 simultaneously attacking players; measure frame times.
- [ ] Low FPS, high FPS, network latency, packet jitter/loss, repeated combat.
- [ ] Observe LOD transitions at 24/48/64 blocks.
- [ ] Confirm local/recent effects survive overload while distant effects degrade.
- [ ] Disconnect/reconnect and datapack reload do not retain old trails or mappings.

Do not mark these manual checks passed solely because the automated harness passes.
