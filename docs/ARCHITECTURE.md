# Implementation notes

## Pipeline

1. A motion provider reports an active attack. Better Combat's actual animation start/cancel and keyframe state cover local and remote client players.
2. A scoped held-item render context identifies entity and hand. The item-render hook runs after arm bending, item animation transforms, and the baked model's display transform.
3. Centered emitter coordinates are transformed to camera-relative coordinates, then the camera's double-precision position is added. World coordinates remain doubles to avoid world-border precision loss.
4. A frame gate avoids repeated shadow/outline/model passes. The tracker stores bounded observations separately from visible ribbons, preserving impact direction even if trails are disabled.
5. Model geometry is analyzed once per item/model and cached. The principal axis estimates handle/blade bounds; metadata and explicit mappings handle exceptions.
6. Each emitter and elemental layer has a fixed-capacity ribbon history. Motion-sensitive subdivision, distance/age eviction, discontinuity resets, and global budgets bound work.
7. Hit confirmation selects recent motion near the server hit tick. Horizontal entity contact is approximated on the bounding box; height comes from the stored Better Combat attack-volume center. Only entity contacts produce impacts.
8. For non-blunt weapons, local hitbox proportions choose stab or slash, and the dominant width/height axis orients the cut. The axis is projected onto a camera-facing sprite plane with a fixed small angle variation per hit. Families, elements, material accents, and critical/blocked modifiers compose independently.
9. Reusable render buffers submit transient quads. Each quad becomes two triangles. Trails retain normal depth testing; impact flashes default to target depth priority. Depth writes are disabled and additive layers use source alpha.
10. Data reloads swap validated snapshots, synchronize complete bundles, then invalidate caches and transient effects.

## Known limits

- Better Combat **2.4.0** and Player Animator **2.0.4** are the tested versions. The metadata currently accepts Better Combat 2.4.x.
- Automatic emitter inference is approximate. Asymmetric, unusually oriented, highly procedural, or internally animated custom-renderer models may need metadata or explicit emitters. Twinblade/warglaive, quarterstaff, and chakram hints enable multiple emitters; unusual arrangements still need explicit emitters.
- Player Animator's default first-person model path is exercised. Custom camera/render replacement mods and shader packs need separate testing.
- Entity contacts use bounding-box approximations and translation-relative attachment, not skeletal hit locations or bone rotation.
- Client-only local hit fallback associates recent candidates with hurt feedback. Simultaneous attackers can make attribution ambiguous. Install the optional server component for confirmed attributed remote impacts, criticals, and shields.
- Block collisions never produce Combat Traces hit sprites or contact accents. Entity material mappings and shield responses still apply.
- `THRUST` is inferred from forward motion relative to the blade; spin is inferred from accumulated blade-axis rotation. A perfectly axial weapon/body spin can lack enough blade-axis travel to classify as spin, although its ribbon still follows the rendered movement.
- Element providers support arbitrary integration logic. Built-in JSON conditions cover component presence, not arbitrary component values, vanilla loot predicate files, Curios, or spell-system state.
- Glow is emissive/additive rendering. There are no dynamic lights, screen distortion shaders, skeletal body-part detection, or GPU compute trails.
- A rendered `RemotePlayer` and an integrated server were exercised automatically. A real multi-user server session, latency/loss behavior, large-player GPU profiling, and the complete weapon/mod compatibility matrix remain manual checks.
- APIs that bypass Minecraft's held-item rendering path need their own integration.

No gameplay, weapon damage, attack cooldowns, block destruction, or animation assets are changed by the shipped mod.

## 1.0.1 model and impact refinements

Baked generated-item fronts contain transparent texture corners, so emitter inference uses their silhouette side quads. Baked cuboid edges are sampled to build a cross-section profile. Blades are fitted above an inferred crossguard; blunt heads are fitted across the distal widening. Geometry caches include the weapon family. Generic vanilla sword/axe tags have fallback priority, allowing a declared hammer category to select blunt effects even for SwordItem subclasses.

AttackShape carries optional volume metadata from the actual animation-selected Better Combat attack. FORWARD is only a hint: geometric translation along both blade endpoints detects thrusts, and a contact gesture is retained through recovery for delayed hit packets. Explicit sweeps do not latch incidental axial motion. Blunt weapons always retain blunt impact composition.

Priority impacts draw during AFTER_LEVEL with an explicit camera matrix, main framebuffer, and restored depth/color state. Trails retain normal depth occlusion. Vanilla accent particles are tinted before publication; no post-publication particle mutation occurs. See COMPATIBILITY.md for the scoped AsyncParticles validation.

## 1.0.2 contact and activation refinements

BetterCombatGeometry reconstructs the TargetFinder volume center from its public tracing point, weapon hitbox dimensions, pitch/yaw, attack range multiplier, and ordered range extensions. Spin volumes remain centered on the tracing origin. The immutable motion snapshot carries this center; older motion-provider constructors retain the existing contact-height fallback. The contact tracker stores the strongest nearby incoming motion and its center so recovery and hit-packet delay do not change the cut's angle or height.

The volume construction follows [Better Combat TargetFinder](https://github.com/ZsoltMolnarrr/BetterCombat/blob/1.21.1/common/src/main/java/net/bettercombat/client/collision/TargetFinder.java), verified against the pinned 2.4.0 JAR and live collision results. No collision search is repeated while sampling weapon poses.

Every trail layer requires the configurable minimum measured speed to start. Automatic timing excludes setup and recovery; explicit animation windows remain subject to the starting speed gate. Started strokes continue through deceleration and fade when the interval ends.

## 1.0.3 hitbox-driven impact selection

AttackHitbox carries the full local dimensions and oriented world-space axes from the same Better Combat collision volume used for height. Classification compares local dimensions, not the rotated world AABB: depth strictly larger than both width and height selects a stab; otherwise the larger width/height axis selects a slash. Depth ties in spin volumes remain slashes; equal width and height deterministically use width. Heavy cleave weapons retain their heavier slash texture.

The selected axis is projected into the sprite plane. Stateless per-hit variation rotates that basis by at most 7 degrees, remains constant over the flash lifetime, and is shared by its composed layers. Blunt weapons are exempt from both the shape-driven family change and angle variation. Valid hitbox metadata works even without usable blade velocity samples. Providers without valid geometry retain the existing motion-based fallback.

## Generated sweeps

The reference is Better Combat 2.4.0 at commit `e4a6a4a2ae109045d7a1eae334728bd726c305ea`. Its [trail definitions](https://github.com/ZsoltMolnarrr/BetterCombat/blob/e4a6a4a2ae109045d7a1eae334728bd726c305ea/common/src/main/java/net/bettercombat/client/particle/TrailParticles.java) select layered slash/stab sprites with offsets per animation. [Placement](https://github.com/ZsoltMolnarrr/BetterCombat/blob/e4a6a4a2ae109045d7a1eae334728bd726c305ea/common/src/main/java/net/bettercombat/client/particle/SlashParticleUtil.java) starts below eye height, applies yaw/pitch/roll and local offsets, and scales by weapon range plus 0.25 blocks. [SlashParticle](https://github.com/ZsoltMolnarrr/BetterCombat/blob/e4a6a4a2ae109045d7a1eae334728bd726c305ea/common/src/main/java/net/bettercombat/client/particle/SlashParticle.java) renders an oriented, double-sided quad over six ticks.

Combat Traces generates its own geometry; no Better Combat textures, animation tables, or source files are bundled. Since 1.0.5, slash strips use the captured blade origin and tip directly. The bright outer edge follows each sampled tip, and the remaining bands lie along the corresponding blade span. Collision-volume planes, fixed body anchors, and artificial overhead tilt do not alter the trail. This preserves diagonal swings, overhead attacks, offhand motion, and animations that change plane.

Hitbox proportions still select a stab versus a slash; that classification is independent of slash-trail orientation. Blunt wakes retain the detected head endpoints, and non-blunt stabs retain their crossed tip wakes. Their shape and detection behavior are unchanged by the tracking fix. Entity-hit flashes also retain their existing hitbox-based placement and direction.

The renderer uses three flat-color bands with twelve silhouette-width steps. Only the physical base layer receives the generic enchantment tint. Built-in fire, frost, lightning, arcane, poison, holy, and shadow trails use generated geometry with their own configured colors. Their 300 ms lifetime and smoothstep fade match the base styles. Custom pack styles can still opt into textured rendering.

Rotational subdivision preserves blade length; model-relative sample spacing limits redundant points. Completed strokes fade intact over the style lifetime, and resumption retires the prior stroke instead of joining through slow motion. Retired strokes count against the same trail and sample limits.

The optional Better Combat spawn hook suppresses authored particles only when the relevant trail settings permit rendering and the current attack has a recent held-item observation. It runs before particle construction, independently of AsyncParticles' worker queues. Original textured styles and the earlier EffectStyle constructor remain available for pack/API consumers.

## Strike-phase emission

`CombatMotion.swingWindow` carries normalized start/end bounds for the striking motion. `HeldItemCapture` continues recording poses throughout the attack for impact inference, but its shared activation gate starts base and elemental ribbons only inside that interval after reaching the configured weapon speed. Started strokes continue to the interval boundary. Leaving the interval pauses every layer. Existing geometry and already-created particles retain their normal lifetime.

Better Combat supplies no explicit damaging-phase interval. `BetterCombatSwingTiming` estimates it once when an attack starts from the loaded Player Animator keyframes, preferring strong coordinated arm/item motion and using torso rotation for spin attacks. It does not use animation names or copied animation tables. The estimate is separate from animation fade-in and fade-out metadata, which can overlap the strike. The adapter converts Better Combat's cooldown-based upswing hint to the corresponding keyframe-clock contact position before selecting the interval.

Providers can supply their own `SwingWindow`. Older providers receive a bounded contact-phase estimate; pack animation windows can override automatic timing for unusual animations. Multiple equally strong gestures can remain ambiguous, so automatic timing is an estimate rather than an authored damage marker. No damage, cooldown, hitbox, or animation playback is changed.

## Complete strokes and multiple emitters

In 1.1.0, strike timing merges substantial coherent arm, item, and torso segments instead of selecting one fastest pair of keyframes. This includes successive cuts, full staff rotations, and item-driven chakram motion. Pauses, substantial reversals, and weak settling bound the estimate. Unusual clips can still use explicit pack windows.

`HeldItemCapture` measures all emitters before starting any layer. `StrokeSampler` uses the fastest endpoint speed to start the stroke, keeps it active through deceleration, and clips frame-to-frame samples at the strike boundaries. Two boundary poses can create a short visible strip even when the whole interval crosses between frames. This is interpolation between observed poses; it cannot reconstruct an unseen complete revolution whose endpoints coincide. Already-created accents are not replayed for reconstructed boundary poses.

Generated trails retain the start and current endpoint at the sample cap, removing the least significant interior pose. This keeps the complete stroke within the existing fixed capacity. Global distance and sample budgets still apply. A world/camera/resource change clears the bounded sampler map with the other effect state.

`WeaponTopology` uses item tags, model-parent hints, and attack categories to select single, double-bladed, double-blunt, or circular geometry. Specific model/type hints take precedence over shared animation categories. `MultiEmitterGeometry` fits the actual baked silhouette: opposed blade spans, transverse staff end caps, or eight segments around a planar rim. Physical and elemental layers share every emitter; accents alternate around those emitters without multiplying their cadence. No external model or animation assets are bundled.
