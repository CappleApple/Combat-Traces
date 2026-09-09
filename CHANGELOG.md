# Changelog

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
