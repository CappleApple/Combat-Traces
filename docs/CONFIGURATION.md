# Client configuration

File: `config/combattraces-client.toml`. Settings are also exposed in NeoForge's Mods configuration screen. Runtime values constrain all pack definitions.

| Setting | Default | Purpose |
|---|---:|---|
| enable_trails / enable_impacts / enable_particles | true | Independent effect switches |
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
| trail_velocity_threshold | 4 | Minimum weapon speed for all ribbons and trail accents, 0-100 blocks/second |
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

`trail_velocity_threshold` controls the minimum measured weapon speed during an attack. Its default is 4 blocks/second, and it applies to base ribbons, elemental ribbons, and their accents, including explicit animation windows. Existing config values are preserved; set an older value to 4 to try the new default. Raising it makes trail emission more selective; 0 disables only the speed requirement.

Automatic timing also waits until shortly before the provider's contact phase, suppressing fast early windups that a speed check alone would allow. Explicit pack windows can refine that timing but cannot bypass the speed requirement. When the weapon slows down, the existing strip fades; a later fast movement starts a fresh strip. Motion remains sampled for impacts even when no ribbon is emitted.

Hit effects attach only to entities. Their height uses the Better Combat swing volume center, including aim pitch and reach. Horizontal contact remains approximate. For non-blunt weapons, a depth-dominant hitbox produces a stab. Otherwise its dominant width/height axis produces a horizontal/vertical slash with up to 7 degrees of fixed variation per hit. Blunt effects retain their existing behavior.
