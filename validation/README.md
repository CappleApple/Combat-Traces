# Local validation evidence

Combat Traces **1.0.3** passed **52 unit tests**, 5 GameTests with Better Combat, 5 without it, 19 actual-client acceptance checks, and client startup without Better Combat. The JAR passed the 101-definition schema audit and contains 16 RGBA 32x32 effect textures. Test classes, test animations, and dependency JARs are excluded.

The isolated Simply Swords 1.70.2 / AsyncParticles 21.1.4.2 client passed **126 checks** using actual Better Combat animations: longsword slash/stab, claymore slash/stab/overhead slam, and greathammer swing/slam. Server-side test damage exercises the confirmed-hit path while these animations run; mouse combat input is not automated.

The geometry checks compare center, full dimensions, and all three oriented axes with Better Combat's actual collision volume while aiming up, level, and down. Confirmed hits select the actual sprite from those proportions and use the dominant axis. The greathammer's depth-dominant slam still selects blunt. Horizontal and vertical screenshots were inspected. Unit tests additionally cover conflicting or absent motion samples, rotated volumes, spin ties, invalid geometry, and stable angle variation bounded to seven degrees. The trace's raw variation value is ignored for stab and blunt effects.

Early windups create no ribbon. Block/null-target submissions create neither impact quads nor contact accents. The tested trail speed setting is 4 blocks/second.

A cut behind an opaque golem changes 0 target-region pixels at normal depth and 1011 with depth priority enabled. AsyncParticles worker ticking and GPU rendering are active. Particle bursts span a resource reload and create 600 accents afterward without observed particle-thread, rendering, or OpenGL errors.

Evidence:

- [Artifact identity and counts](results.json)
- [Base-client assertions](client-validation.txt)
- [Simply Swords / AsyncParticles assertions](simplyswords-validation.txt)
- [Recorded geometry and motion](simplyswords-validation-detail.txt)
- [Horizontal cut](screenshots/ss-0-hit.png), [vertical cut](screenshots/ss-5-hit.png), [stab](screenshots/ss-6-hit.png), and [hammer ribbon](screenshots/ss-3-ribbon.png)
- [Configuration](../docs/CONFIGURATION.md) and [implementation](../docs/ARCHITECTURE.md)

Dedicated-server fresh startup, reload, and restart evidence in server-smoke.txt belongs to the earlier 1.0.0 baseline. Current server-side gates are the two GameTest runs.

The remote-player check renders a synthetic RemotePlayer in one client. A real multi-user session, the full Within modpack, and shader packs remain untested.
