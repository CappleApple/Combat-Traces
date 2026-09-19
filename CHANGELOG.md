# Changelog

## 1.1.0 - 2026-09-19

### Added
- Separate trails for both striking ends of twinblades, warglaives, and quarterstaffs.
- Circular rim trails fitted to chakram models.

### Fixed
- Trails starting late, ending early, or missing short attacks between rendered frames.
- Multi-segment cuts, staff rotations, and chakram flight/spin animations losing parts of their trails.
- Long strokes losing their starting geometry when the sample limit was reached.
- Twinblades being treated as blunt weapons because of their Better Combat category.

### Changed
- Updated the bundled license and mod metadata to CC BY-NC-SA 4.0 with the Minecraft modpack/server additional permission.
- The minimum weapon speed starts a trail; the trail then continues through the detected strike as the weapon slows.
- Enchantment and elemental layers follow all striking ends, with accents sharing one particle cadence per weapon layer.

## 1.0.6 - 2026-09-13

### Fixed
- Weapon trails continuing through animation windups, recovery, and transition movements.
- Base, enchantment, and elemental trails now share the same strike interval and stop adding particles when it ends.

### Changed
- Infer strike timing from the loaded Better Combat animation keyframes while preserving the completed trail's fade.

## 1.0.5 - 2026-09-13

### Fixed
- Sword and slashing trails being flattened onto the attack hitbox plane instead of following the animated blade.
- Elemental trail overlays still using the older textured ribbon renderer.
- Elemental colors being multiplied by the physical trail's blue enchantment tint.

### Changed
- Matched built-in elemental trails to the base trail's 300 ms lifetime and fade.


## 1.0.4 - 2026-09-13

### Changed
- Replaced default thin ribbons with automatically generated, pixel-stepped slash crescents modeled on Better Combat trail placement and layering.
- Derived arc size from weapon models and orientation from swing geometry, including custom animations.
- Added crossed thrust streaks and layered wakes across detected blunt-weapon heads.
- Kept completed sweeps visible during their short fade and separated resumed swings from earlier strokes.
- Added a setting to replace Better Combat trail particles during captured attacks, preventing duplicate effects.
- Kept textured trails available through the per-style `trail_geometry` option.

## 1.0.3 - 2026-09-09

### Changed
- Selected non-blunt stab and slash effects from Better Combat hitbox proportions.
- Aligned slash effects with the dominant width or height axis, with stable variation of up to 7 degrees per hit.
- Preserved blunt impact selection for every hitbox shape.
- Retained valid attack geometry for hits without usable blade velocity samples.

## 1.0.2 - 2026-09-09

### Changed
- Aligned hit sprites with the incoming swing direction retained through recovery.
- Positioned entity hits at the vertical center of the Better Combat attack volume.
- Removed block-contact hit effects and accents.
- Made angled and short forward stabs easier to detect.
- Applied the configurable minimum swing speed to every trail layer and custom animation window; raised its default to 4 blocks/second.
- Suppressed early automatic windup trails and prevented strips from connecting across slow movement.

## 1.0.1 — 2026-09-09

### Changed
- Remade effect sprites with crisp Minecraft-style pixels and straight sword-cut impacts.
- Gave hit effects depth priority over struck targets.
- Matched physical ribbon widths to the detected blade or hammer head.

### Fixed
- Full-blade and greathammer-head emitter inference using visible model geometry.
- Blunt weapons being classified as swords through generic item tags.
- Sword stabs losing their puncture effect when hit feedback arrives during recovery.
- Particle tint initialization occurring after publication to the particle engine.

## 1.0.0 — 2026-09-09

### Added
- Animated weapon ribbons sampled from the actual rendered item pose.
- Optional Better Combat and Player Animator integration.
- Automatic weapon classification and baked-model emitter inference.
- Multi-emitter overrides, directional impact families, elemental overlays, and material accents.
- Confirmed entity-hit metadata, first-person controls, LOD, and bounded effect budgets.
- Reloadable pack definitions, replaceable textures, extension API, configuration UI, and debug tools.
