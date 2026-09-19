# Client configuration

File: `config/combattraces-client.toml`. Settings are also exposed in NeoForge's Mods configuration screen. Runtime values constrain all pack definitions.

| Setting | Default | Purpose |
|---|---:|---|
| enable_trails / enable_impacts / enable_particles | true | Independent effect switches |
| replace_better_combat_trails | true | Suppress Better Combat trail particles when Combat Traces can capture the attack; false shows both |
| impact_depth_priority | true | Render hit flashes over struck objects |
| enable_elemental_effects / enable_material_effects | true | Composition switches |
| critical_modifier | true | Critical size/flash accents |
| first_person_trails / third_person_trails / other_player_trails | true | Camera and actor visibility |
| trail_quality | 2 | 0: no ribbons; 1: reduced sampling; 2: full |
| trail_sample_count | 24 | Per-emitter history, 4-64 |
| trail_lifetime_multiplier | 1 | 0.1-3 |
| impact_scale / impact_lifetime_multiplier | 1 | 0.1-3 |
| particle_multiplier | 1 | 0-3 |
| render_distance | 64 | Complete cutoff, 8-128 blocks |
| full_quality_distance | 24 | Beyond this, fewer samples and no minor particles/element overlays |
| trail_distance | 48 | Beyond this, impact sprites only |
| first_person_opacity | 0.4 | 0-0.8, multiplied into trail alpha |
| first_person_width_multiplier | 1.0 | 0.1-1.5 |
| first_person_particle_multiplier | 0.3 | 0-1 |
| minimum_sample_distance | 0.018 | Avoid redundant nearly stationary samples |
| maximum_sample_distance | 0.16 | Target subdivision distance |
| max_subdivisions_per_frame | 6 | Maximum adaptive subdivisions |
| discontinuity_distance | 5 | Reset a strip after implausible movement |
| trail_velocity_threshold | 4 | Minimum weapon speed to start a strike trail, 0-100 blocks/second |
| max_simultaneous_trails | 64 | Includes elemental layers and multiple emitters |
| max_trail_samples_total | 1536 | Hard total across all ribbons |
| max_active_impacts | 96 | Hard limit on composed impact quads |
| max_particles_per_frame | 24 | Shared accent budget |
| max_particles_per_second | 200 | Strict rolling-one-second limit |
| max_accent_particles_per_impact | 12 | Shared across that impact's composition |
| max_element_layers | 2 | 0-4, per weapon |
| debug_rendering | false | Emitter/sample/velocity/hit lines and HUD |

Default distance tiers are 0-24 blocks full quality, 24-48 reduced ribbons, 48-64 impact sprites, then disabled. The total cutoff always applies even if individual distances are configured out of order.

Budget pressure drops distant effects before local effects. Equally distant old effects are evicted first. The server does not run trail simulation.

Pausing singleplayer freezes effect time. World, camera, and resource changes clear transient state; a disconnected server's mappings cannot leak into the next connection.

Vanilla particle settings are respected: Minimal suppresses accents; Decreased reduces them. Ribbon geometry is controlled independently by Combat Traces settings.

## Target visibility

`impact_depth_priority` defaults to `true`. Hit flashes render after the world with depth testing disabled, so embedded and back-facing contacts remain visible over their targets. This applies to slash, stab, blunt, and elemental impact layers. These brief flashes can also show through intervening geometry while alive. Set it to `false` to use normal world occlusion. Distance, lifetime, and count limits still apply.

The default first-person ribbon width is now 1.0, matching the inferred blade/head span. Existing user configuration values are preserved.

## Swing speed and windups

`trail_velocity_threshold` controls the minimum measured weapon speed needed to start a trail within the detected strike. Its default is 4 blocks/second. The fastest captured endpoint supplies the speed for the whole weapon, so both ends and elemental layers start together. Once started, the trail continues through strike deceleration and stops at the strike boundary. Existing config values are preserved. Raising the value makes starting more selective; 0 still requires motion.

Better Combat trails use a strike interval inferred from the loaded animation keyframes. Setup and recovery movements outside that interval do not become trail geometry or create trail accents. If a frame crosses an interval boundary, the geometry is clipped to that boundary. Base, enchantment, and elemental layers share this gate. Existing geometry and particles finish their normal fade. Motion remains sampled for impacts throughout the attack.

Custom providers can supply a strike interval. Without one, automatic timing is limited to 0.12 animation progress before and after their contact hint. Pack animation windows override the automatic estimate for unusual animations; the speed requirement still applies. Better Combat has no explicit damaging-phase marker, so animations with multiple equally strong movements may need a pack window.

Hit effects attach only to entities. Their height uses the Better Combat swing volume center, including aim pitch and reach. Horizontal contact remains approximate. For non-blunt weapons, a depth-dominant hitbox produces a stab. Otherwise its dominant width/height axis produces a horizontal/vertical slash with up to 7 degrees of fixed variation per hit. Blunt effects retain their existing behavior.

## Generated trails

All built-in trail styles use generated geometry with a 300 ms lifetime. Slash bands follow the captured blade positions in 3D, including diagonal and changing-plane motion. Hammer wakes follow the head, and detected stabs use crossed streaks behind the tip. The physical layer becomes cyan on enchanted weapons. Elemental layers retain their configured colors and use the same weapon/attack shape; their colors are not multiplied by the physical layer's blue enchantment tint.

`replace_better_combat_trails` suppresses Better Combat's own trail particles only for visible, recently captured attacks in range. Disabling Combat Traces trails, setting quality to 0, or disabling the relevant camera/player visibility restores Better Combat's normal behavior and its own settings. An unsupported or uncaptured attack also keeps Better Combat's effect. The replacement does not alter attacks or hit detection.

Pack authors can select `trail_geometry` per style; see [style configuration](DATAPACKS.md#trail-and-impact-styles).

## Weapons with several striking edges

Twinblades and warglaives use two blade emitters; quarterstaffs use two short terminal-cap emitters and keep blunt impacts. Chakrams use eight connected segments around the model silhouette. Item tags, model parents, and attack categories select the shape; the loaded model supplies its dimensions. Explicit pack/provider emitters still take precedence.

Elemental ribbons use the same emitters and timing. Accent particles alternate between emitters while sharing one cadence per weapon layer. Each ribbon remains subject to the existing trail and sample budgets. Generated strokes preserve their beginning and endpoint by simplifying interior samples at capacity.
