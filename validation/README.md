# Local validation evidence

Combat Traces **1.1.0** passed **123 unit tests**, 5 GameTests with Better Combat, 5 without it, 19 actual-client acceptance checks, and client startup without Better Combat. The JAR passed the 101-definition schema audit and contains 16 RGBA 32x32 effect textures. Test classes, test animations, and dependency JARs are excluded.

The isolated Simply Swords 1.70.2 / AsyncParticles 21.1.4.2 client passed **324 checks** using actual Better Combat animations: longsword slash/stab, claymore slash/stab/overhead slam, and greathammer swing/slam, and spear stab. Server-side test damage exercises the confirmed-hit path while these animations run; mouse combat input is not automated.

Generated-trail checks compare actual emitted slash vertices with the captured blade origins/tips in the live Simply Swords client. Unit cases cover diagonal, overhead, horizontal, changing-plane, mirrored offhand, low-frame-rate, and world-border geometry. The renderer no longer projects blade movement onto a hitbox plane.

Fire Aspect cases cover longsword slash, claymore stab, and greathammer swing. Physical and elemental layers use generated geometry with the same shape selection, and the elemental edge retains its configured color. Existing stab detection, crossed thrust streaks, hammer-head geometry, native duplicate suppression, and stroke fading checks still pass.

The geometry checks compare center, full dimensions, and all three oriented axes with Better Combat's actual collision volume while aiming up, level, and down. Confirmed hits select the actual sprite from those proportions and use the dominant axis. The greathammer's depth-dominant slam still selects blunt. Horizontal and vertical screenshots were inspected. Unit tests additionally cover conflicting or absent motion samples, rotated volumes, spin ties, invalid geometry, and stable angle variation bounded to seven degrees. The trace's raw variation value is ignored for stab and blunt effects.

The additional Simply Swords / Simply More suite passed **1,328 checks across 47 cases**, covering all 24 installed diamond weapon families and 18 loaded animations at actual weapon cooldowns. All ends and eight chakram rim segments produce finite meshes. Across 98 recorded emitter cases, the smallest retained portion was 93.3% of the selected strike interval; every trail reached its endpoint. Tests include offhand/first-person motion and Fire Aspect at 20 FPS. Setup/recovery stay outside the emitted geometry, and started strokes remain continuous through deceleration.

Synthetic tests cover boundary crossings between frames, post-end interpolation, two-pose meshes, long histories at capacity, multiple coherent animation segments, and model-derived opposite ends/rims. Automatic timing remains an estimate, and individual unique/custom models are not exhaustively tested.

Every development client launch starts muted with mouse capture blocked. Runtime checks verified master volume zero and GLFW's normal cursor in menus and worlds; the guards are absent from the release JAR.

Block/null-target submissions create neither impact quads nor contact accents. The tested trail speed setting is 4 blocks/second.

A cut behind an opaque golem changes 0 target-region pixels at normal depth and 1011 with depth priority enabled. AsyncParticles worker ticking and GPU rendering are active. Particle bursts span a resource reload and create 600 accents afterward without observed particle-thread, rendering, or OpenGL errors.

Evidence:

- [Artifact identity and counts](results.json)
- [Base-client assertions](client-validation.txt)
- [Simply Swords / AsyncParticles assertions](simplyswords-validation.txt)
- [Recorded geometry and motion](simplyswords-validation-detail.txt)
- [Simply Swords / Simply More coverage](multi-weapon-validation.txt) and [per-frame/retained-stroke evidence](multi-weapon-validation-detail.txt)
- [Twinblade](screenshots/twinblade.png), [quarterstaff](screenshots/quarterstaff.png), [warglaive](screenshots/warglaive.png), and [chakram spin](screenshots/chakram-spin.png)
- [Horizontal cut](screenshots/ss-0-hit.png), [vertical cut](screenshots/ss-5-hit.png), [stab](screenshots/ss-6-hit.png), and [hammer ribbon](screenshots/ss-3-ribbon.png)
- [Generated slash](screenshots/generated-slash.png), [overhead cut](screenshots/generated-overhead.png), [head wake](screenshots/generated-hammer.png), [thrust](screenshots/generated-stab.png), and [spear](screenshots/generated-spear.png)
- [Fire Aspect slash](screenshots/fire-slash.png), [stab](screenshots/fire-stab.png), and [hammer](screenshots/fire-hammer.png)
- [Configuration](../docs/CONFIGURATION.md) and [implementation](../docs/ARCHITECTURE.md)

Dedicated-server fresh startup, reload, and restart evidence in server-smoke.txt belongs to the earlier 1.0.0 baseline. Current server-side gates are the two GameTest runs.

The remote-player check renders a synthetic RemotePlayer in one client. A real multi-user session, the full Within modpack, and shader packs remain untested.
