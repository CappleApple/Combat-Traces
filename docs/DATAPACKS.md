# Pack definitions

Minecraft 1.21.1 uses datapack format **48** and resource-pack format **34**.

Definitions live at `data/<namespace>/combat_traces/<section>/<name>.json`. Their ID is `<namespace>:<name>`. Sections are `weapons`, `classifications`, `animations`, `trails`, `impacts`, `elements`, and `materials`.

The bundled definitions are mirrored under `assets/<namespace>/combat_traces/` for clients connected to servers without Combat Traces. When the server supplies definitions, server entries override matching client entries. Resource packs can always replace textures independently.

The reload is prepared off-thread, then applied as an immutable snapshot on the appropriate main thread. Server snapshots are split into optional, bounded packets; clients apply them only after every chunk arrives. Models, effect instances, and classifications refresh together.

## Weapon mappings

```json
{
  "priority": 100,
  "items": ["minecraft:diamond_sword"],
  "item_tags": [],
  "weapon_class": "combattraces:slash",
  "elements": ["combattraces:fire", "combattraces:lightning"],
  "trail": "combattraces:slash",
  "impact_scale": 1.15,
  "emitters": [
    {"origin": [-0.1, -0.1, 0.0], "tip": [0.45, 0.45, 0.0]},
    {"origin": [0.1, 0.1, 0.0], "tip": [-0.45, -0.45, 0.0]}
  ]
}
```

This intentionally makes a diamond sword a double-ended visual demonstration. For an actual unusual weapon, use its real item ID or an item tag.

Selectors are ORed. Higher priority wins; equal-priority entries have deterministic resource-ID ordering. Only the first matching weapon rule supplies overrides. Omitted fields continue through automatic inference.

**Coordinate convention:** origin/tip coordinates are in block units relative to the baked model center, before item display transforms. A raw model vertex at `[0.5, 0.5, 0.5]` is emitter coordinate `[0, 0, 0]`. The renderer applies the exact held-item transformation, including mirroring and model scale. There are no world-space or per-animation path coordinates in a weapon mapping.

Up to 32 emitters may be defined; rendering remains subject to the global trail/sample budgets. Coordinates must be finite and within ±16; a zero-length emitter is invalid.

Emitter priority:

1. Explicit weapon rule.
2. Registered emitter providers, `TrailEmitterModel`, or item-model metadata.
3. Cached baked geometry analysis.
4. Category-shaped fallback.
5. Generic diagonal emitter.

For item-model JSON metadata, add `"combat_traces": {"emitters": [...]}` to `assets/<item namespace>/models/item/<item path>.json`. For runtime model variants, implement `TrailEmitterModel` or register an emitter provider.

## Classification rules

Files in `classifications/` use `priority`, `items`, `item_tags`, and `weapon_class`.

Classes: `slash`, `cleave`, `blunt`, `pierce`, `claw`, `whip`, `magic`, `generic`.

Priority: weapon override → classification tags/rules → API classifiers → Better Combat category → item class → model proportions → generic. Broad categories are matched, never mod-specific weapon IDs or animation-name patterns.

Empty extension tags are provided for blunt, piercing, claw, whip, magic, and every elemental weapon group. Tag paths use Minecraft 1.21's singular `tags/item/`.

## Animation windows

```json
{
  "animation": "combattraces_validation:diagonal_probe",
  "trail_window": {"start": 0.18, "end": 0.72},
  "sample_multiplier": 1.25
}
```

This animation ID belongs to the development fixture and is excluded from the shipped mod. Use an installed animation's actual ID in a real pack.

Windows are optional. Without one, measured weapon velocity controls activation. Progress comes from the active Player Animator keyframe player after Better Combat speed modifiers. `sample_multiplier` is clamped to 0.25–3.

## Trail and impact styles

```json
{
  "texture": "combattraces:textures/vfx/trails/fire.png",
  "render_mode": "additive",
  "lifetime_ms": 220,
  "width_multiplier": 1.0,
  "opacity": 0.7,
  "intensity_multiplier": 1.0,
  "uv_scroll_speed": 0.8,
  "fade_curve": "ease_out",
  "frames": 1,
  "fps": 20,
  "particle": "minecraft:small_flame",
  "particle_rate": 0.15
}
```

Impact styles use the same common fields plus `scale`; trail width and accent rate are irrelevant to impact quads. `camera_bias` remains accepted for pack compatibility, but impact sprites now face the camera and, when hitbox geometry is available, rotate to its dominant width/height axis with a small fixed variation per hit.

- Render modes: `translucent`, `additive`. Additive blending respects alpha.
- Fade curves: `linear`, `ease_out` (quadratic remaining alpha), `smoothstep`.
- `color`: optional six-digit RGB hex, such as `"ff9d42"`.
- Lifetime: 30–1000 ms before the client multiplier.
- Width: 0.05–4; scale: 0.1–3; opacity: 0–1; intensity: 0–3; camera bias: 0–1.
- Animated textures: put equal-sized frames in a vertical PNG strip and set `frames` (1–64) and `fps` (1–120). This is an explicit flipbook, independent of Minecraft atlas `.mcmeta` animation.
- Trail `particle_rate` is expected particles per tick, converted to a frame-independent cadence and clamped to 0–4. All particle budgets still apply.
- Named particle IDs must be simple particle types. For block debris, the material system builds block-state particle options automatically.

## Elements

```json
{
  "id": "combattraces:fire",
  "priority": 100,
  "conditions": {
    "or": [
      {"item_tag": "combattraces:fire_weapons"},
      {"enchantment": "minecraft:fire_aspect"}
    ]
  },
  "trail": "combattraces:fire",
  "impact": "combattraces:fire_overlay",
  "particle": "minecraft:small_flame",
  "color": "ff8c36"
}
```

When style references are omitted, an element uses its own ID as its trail style and `<element path>_overlay` as its impact style. This lets a concise condition rule reuse built-in fire/frost/etc. styles.

Each condition object contains exactly one operator:

- `item` or `item_id`: item resource ID.
- `item_tag`: tag ID.
- `enchantment`: any positive level of the named enchantment.
- `component`: presence of the named data component.
- `effect`: active status-effect ID.
- `category`: exact Better Combat category.
- `or`, `and`: arrays of conditions.
- `not`: one nested condition.

Nesting is limited to eight levels, with at most 32 clauses in an AND/OR. Arbitrary vanilla loot-predicate files and component-value comparisons are not evaluated on the client; register an API provider for those integrations.

Explicit weapon elements are considered first, API results second, then matching element definitions in priority order. IDs are deduplicated and capped by `max_element_layers`. Physical effects come from the base weapon style rather than an extra overlay.

## Materials

```json
{
  "material": "combattraces:metal",
  "priority": 100,
  "block_tags": ["minecraft:anvil", "c:storage_blocks/iron"],
  "blocks": [],
  "entity_tags": [],
  "entities": ["minecraft:iron_golem"],
  "armor_tags": ["combattraces:metal_armor"],
  "particle": "minecraft:electric_spark",
  "particle_count": 7,
  "color": "ffd188"
}
```

Mappings can define new material IDs. First matching priority rule wins. Entities without a mapping fall back to flesh; other targets fall back to generic. Flesh does not imply gore. Shield blocks use the metal response.

Only entity contacts produce hit effects. Block mappings remain accepted in the data format, but swinging into terrain produces no Combat Traces impact sprites or block-contact accents. The mod does not alter Better Combat collision/damage rules.

## Resource-pack textures

All textures are replaceable at `assets/combattraces/textures/vfx/`:

- `trails/`: slash, heavy_slash, blunt, fire, frost, lightning, arcane.
- `impacts/`: slash, cleave, blunt, pierce, claw, whip, generic, ring, spark.

Use RGBA PNGs with transparent backgrounds. Nearest-neighbor sampling preserves the pixel-art edges. Avoid large opaque borders; the PNG is mapped directly across a ribbon segment or impact quad.

See `docs/schema/` for machine-readable schemas and `examples/` for installable examples. The loader bounds each definition to 16,384 characters, total read text to 400,000 characters, and definition count to 2,048. Invalid entries are logged at reload, not every frame.
