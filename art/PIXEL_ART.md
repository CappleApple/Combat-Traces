# Pixel effect artwork

The 1.0.1 production assets come from **vfx-atlas-pixel.png**, generated using the built-in imagegen tool. The selected output has a genuine RGBA alpha channel; rejected checkerboard previews are not used.

Run tools/export_pixel_atlas.py to extract the tiles at Minecraft-sized 32x32 resolution using nearest-neighbor sampling. Trail tiles use the dense middle band rather than the transparent sprite canvas, so texture coverage follows the inferred blade/head span. Impact tiles retain their isolated silhouettes. GPU texture filtering is disabled. The output files are under src/main/resources/assets/combattraces/textures/vfx/; pixel-effects-preview.png shows the exported assets enlarged without smoothing.

Exact final generation prompt:

> Create ONE production-ready Minecraft PIXEL ART sprite atlas, 4 by 4 grid on a genuinely TRANSPARENT RGBA canvas (alpha zero outside sprites). This is a new image, no photograph. Do NOT depict transparency as a checkerboard. Do NOT paint any checkerboard. No gray background, no backdrop. The PNG must contain actual alpha transparency like a game sprite.
> Style: extremely crisp Minecraft-native 32x32 pixel art for each cell, huge visible regular square pixels, strictly limited 3-5 flat colors per sprite, NO smooth gradients, NO glow blur, NO wispy curves, NO antialiasing. Like vanilla critical-hit particles and item textures. Precisely 16 sprites in equal square cells of a 4x4 grid, clear 24px transparent separation, no text or grid lines.
> Row1: silver-white sword ribbon band; cream heavier sword ribbon band; gold hammer ribbon band; orange-red fire ribbon band. All are HORIZONTAL tapered bands with a dense straight center and jagged pixel steps. They are texture strips for real-motion ribbons, not smoke.
> Row2: cyan frost band; jagged blue-white lightning band; purple magic band; narrow white whip band.
> Row3: (1) narrow horizontal sword CUT across a target, razor straight with long sharp pixel tapered tips and 2 parallel white/cyan broken streaks, no crescent; (2) a slightly thicker horizontal heavy sword CUT, still straight and pointed; (3) chunky golden blunt-hit burst with 8 blocky rays and broken square-stepped ring; (4) compact white/cyan stab puncture, small square center and short four-axis rays.
> Row4: three straight parallel diagonal claw cuts; compact white 4-point pixel magic flash; small silver stepped pixel shock ring with transparent center; tiny gold cross spark.
> Keep sprites simple and bold, read instantly at 32x32 pixels. All boundaries must be staircase square pixels. No actual weapons, no scenery, no text. Genuine transparent alpha background, not visual checkerboard. Square 1024x1024.

The earlier vfx-atlas.png is retained as 1.0.0 artwork history; it is no longer used by the asset export pipeline. No Simply Swords textures or animations are distributed in Combat Traces.
