# VFX artwork

The source atlases in this directory are kept so the shipped VFX tiles can be reproduced and adjusted without editing every sprite separately.

## Current atlas

`vfx-atlas-pixel.png` is the source for the current 32x32 trail and impact textures.

Run:

```text
python tools/export_pixel_atlas.py
```

to extract the individual tiles into:

```text
src/main/resources/assets/combattraces/textures/vfx/
```

The export uses nearest-neighbor sampling so the sprites keep hard pixel edges. `pixel-effects-preview.png` is an enlarged preview of the exported tiles, also without smoothing.

Trail tiles use the dense center band of their cell so ribbon coverage follows the inferred weapon span. Impact tiles keep their isolated silhouette and transparent surroundings. Runtime texture filtering is disabled for these effects.

## Legacy atlas

`vfx-atlas.png` is the older 1.0.0 source atlas. It is retained only as artwork history and is not used by the current export pipeline.

No third-party weapon-mod textures or animations are included in these source files.
