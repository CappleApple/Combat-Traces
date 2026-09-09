package com.cappleapple.combattraces.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client preferences always cap pack-provided values. */
public final class ClientConfig {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.BooleanValue TRAILS,
      IMPACTS,
      IMPACT_DEPTH_PRIORITY,
      PARTICLES,
      ELEMENTS,
      MATERIALS,
      FIRST_PERSON,
      THIRD_PERSON,
      OTHERS,
      DEBUG,
      CRITICAL;
  public static final ModConfigSpec.IntValue QUALITY,
      SAMPLES,
      SUBDIVISIONS,
      MAX_TRAILS,
      TOTAL_SAMPLES,
      MAX_IMPACTS,
      PARTICLES_FRAME,
      PARTICLES_SECOND,
      PARTICLES_IMPACT,
      ELEMENT_LAYERS;
  public static final ModConfigSpec.DoubleValue TRAIL_LIFETIME,
      IMPACT_SCALE,
      IMPACT_LIFETIME,
      PARTICLE_MULTIPLIER,
      DISTANCE,
      FULL_DISTANCE,
      TRAIL_DISTANCE,
      FP_OPACITY,
      FP_WIDTH,
      FP_PARTICLES,
      MIN_DISTANCE,
      MAX_DISTANCE,
      DISCONTINUITY,
      SPEED_THRESHOLD;

  static {
    var b = new ModConfigSpec.Builder();
    TRAILS = b.define("enable_trails", true);
    IMPACTS = b.define("enable_impacts", true);
    IMPACT_DEPTH_PRIORITY =
        b.comment(
                "Draw hit flashes over their targets, including"
                    + " embedded/back-facing contacts.")
            .define("impact_depth_priority", true);
    PARTICLES = b.define("enable_particles", true);
    ELEMENTS = b.define("enable_elemental_effects", true);
    MATERIALS = b.define("enable_material_effects", true);
    CRITICAL = b.define("critical_modifier", true);
    FIRST_PERSON = b.define("first_person_trails", true);
    THIRD_PERSON = b.define("third_person_trails", true);
    OTHERS = b.define("other_player_trails", true);
    DEBUG = b.define("debug_rendering", false);
    QUALITY = b.defineInRange("trail_quality", 2, 0, 2);
    SAMPLES = b.defineInRange("trail_sample_count", 24, 4, 64);
    TRAIL_LIFETIME = b.defineInRange("trail_lifetime_multiplier", 1d, 0.1, 3);
    IMPACT_SCALE = b.defineInRange("impact_scale", 1d, 0.1, 3);
    IMPACT_LIFETIME = b.defineInRange("impact_lifetime_multiplier", 1d, 0.1, 3);
    PARTICLE_MULTIPLIER = b.defineInRange("particle_multiplier", 1d, 0, 3);
    DISTANCE = b.defineInRange("render_distance", 64d, 8, 128);
    FULL_DISTANCE = b.defineInRange("full_quality_distance", 24d, 4, 64);
    TRAIL_DISTANCE = b.defineInRange("trail_distance", 48d, 4, 96);
    FP_OPACITY = b.defineInRange("first_person_opacity", 0.4d, 0, 0.8);
    FP_WIDTH = b.defineInRange("first_person_width_multiplier", 1d, 0.1, 1.5);
    FP_PARTICLES = b.defineInRange("first_person_particle_multiplier", 0.3d, 0, 1);
    MIN_DISTANCE = b.defineInRange("minimum_sample_distance", 0.018d, 0.001, 0.2);
    MAX_DISTANCE = b.defineInRange("maximum_sample_distance", 0.16d, 0.02, 1);
    DISCONTINUITY = b.defineInRange("discontinuity_distance", 5d, 1, 12);
    SPEED_THRESHOLD =
        b.comment(
                "Minimum measured weapon speed in blocks/second to emit ribbons and trail accents.",
                "Applies during attacks, including custom animation windows. Existing trails fade"
                    + " normally.",
                "Automatic timing also suppresses early windup. Set to 0 to disable only the speed"
                    + " gate.")
            .defineInRange("trail_velocity_threshold", 4d, 0, 100);
    SUBDIVISIONS = b.defineInRange("max_subdivisions_per_frame", 6, 1, 12);
    MAX_TRAILS = b.defineInRange("max_simultaneous_trails", 64, 1, 256);
    TOTAL_SAMPLES = b.defineInRange("max_trail_samples_total", 1536, 32, 8192);
    MAX_IMPACTS = b.defineInRange("max_active_impacts", 96, 1, 256);
    PARTICLES_FRAME = b.defineInRange("max_particles_per_frame", 24, 0, 128);
    PARTICLES_SECOND = b.defineInRange("max_particles_per_second", 200, 0, 2000);
    PARTICLES_IMPACT = b.defineInRange("max_accent_particles_per_impact", 12, 0, 32);
    ELEMENT_LAYERS = b.defineInRange("max_element_layers", 2, 0, 4);
    SPEC = b.build();
  }

  private ClientConfig() {}
}
