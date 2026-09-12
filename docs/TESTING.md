# Testing and QA

Combat Traces depends heavily on rendered transforms, animation timing, and optional combat mods, so the test setup is split between unit/GameTests and disposable client runs.

Raw runtime evidence is kept in `validation/` for regression debugging. Passing those fixtures is useful coverage, not a substitute for checking new combat/renderer combinations in-game.

## Automated tests

Run:

```powershell
.\gradlew.bat clean test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runGameTestServer -PwithoutBetterCombat
```

The automated suite covers things such as:

- motion/history math and discontinuities;
- emitter/model geometry inference;
- stab/slash/blunt classification;
- oriented hit geometry;
- stable bounded impact variation;
- resource/datapack parsing;
- element/material mappings;
- effect budgets; and
- behavior with Better Combat absent.

GameTests are run both with and without the Better Combat development runtime so the optional dependency stays optional.

## Client validation

Focused development-client runs are available with:

```powershell
.\gradlew.bat runClient -PvalidateClient
.\gradlew.bat runClient -PwithoutBetterCombat -PvalidateEmptyClient
```

The validation client has been used to exercise first- and third-person trails, remote-player rendering, custom Better Combat animations, entity impacts, elemental overlays, datapack reloads, model-geometry inference, multi-emitter weapons, and the global effect budget.

For the current test fixture, results are written to:

```text
run-client/client-validation.txt
run-client/screenshots/
```

The `validation/` directory contains retained release/regression artifacts.

## Dedicated server smoke check

Run:

```powershell
python tools/server_smoke.py
```

The smoke server is intended to catch accidental client-only class loading and basic reload/save/restart problems. It is not a multiplayer combat test.

## Important manual scenarios

Use `/combattraces debug on` when checking motion. The debug view shows inferred emitter geometry, sample paths, velocity, hit orientation/family, element/material information, and other useful state.

### Weapons and motion

Check a representative mix rather than only a vanilla sword:

- horizontal and vertical sword cuts;
- thrust/stab animations;
- axe/cleave and hammer/mace attacks;
- a long polearm;
- twinblade or other multi-emitter model;
- offhand/left-handed motion;
- rapid combo restarts and attack cancellation; and
- at least one animation supplied by an external datapack/mod.

If emitter inference is changed, include an asymmetric/custom model instead of testing only clean vanilla geometry.

### Hits

- Verify slash, stab, and blunt effects remain visibly distinct.
- Check a moving target so the short-lived effect attachment is obvious.
- Test a shield block and an armored target.
- Confirm misses and terrain crossings do not create entity impacts.
- Test at least one element overlay and one material mapping.

### Camera/rendering

- First and third person at the user's normal FOV.
- A remote player rendered by the client.
- Resource reload while effects are active.
- Resource-pack texture replacement.
- Any shader/custom-camera/held-item renderer used by the target pack.

### Optional-mod compatibility

The maintained compatibility profiles include Better Combat, Simply Swords, and AsyncParticles. When changing hooks that touch these paths, rerun the relevant profile rather than assuming the generic path covers it.

Client startup without Better Combat should remain part of release QA.

### Multiplayer

A single-client `RemotePlayer` fixture exercises remote rendering but does **not** prove real network behavior. For changes to server hit attribution or synchronization, test with two actual users/clients and verify that both see the expected swing and that confirmed impacts are attributed to the correct attacker/target.

### Performance

For changes to trail/effect budgets, try several simultaneous attackers and observe frame times at near and far LOD ranges. Check that overload drops/downgrades distant effects before destroying recent local effects.

## Art/resource checks

The shipped VFX textures are small pixel-art sprites and should remain nearest-sampled. Source/export notes are kept under `art/`.

Optional asset/schema checks can be run with:

```powershell
python -m pip install -r tools/requirements.txt
python tools/check_assets.py
```

## Scope

The automated/client fixtures cover the combinations documented above. They do not certify every shader, animation pack, weapon model, combat overhaul, network condition, or full modpack.

When a new integration is added, document the specific scenario needed to reproduce it rather than only increasing a generic “checks passed” count.
