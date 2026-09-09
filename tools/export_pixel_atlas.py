"""Export the generated pixel-art atlas as Minecraft-sized sprite assets (no interpolation)."""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parents[1]
atlas = Image.open(ROOT / "art/vfx-atlas-pixel.png")
assert atlas.mode == "RGBA", "The atlas must have genuine transparency"
assert atlas.getchannel("A").getextrema()[0] == 0
names = [
    "trails/slash","trails/heavy_slash","trails/blunt","trails/fire",
    "trails/frost","trails/lightning","trails/arcane","impacts/whip",
    "impacts/slash","impacts/cleave","impacts/blunt","impacts/pierce",
    "impacts/claw","impacts/generic","impacts/ring","impacts/spark"
]
preview = Image.new("RGBA",(128,128))
for index,name in enumerate(names):
    row,col=divmod(index,4)
    tile=atlas.crop((round(col*atlas.width/4),round(row*atlas.height/4),
                     round((col+1)*atlas.width/4),round((row+1)*atlas.height/4)))
    if name.startswith("trails/"):
        # A ribbon consumes a texture band, not an isolated sprite's transparent canvas.
        # Use the dense center of each streak so its V axis covers the blade/head span.
        ink=tile.getchannel("A").point(lambda a: 255 if a>=128 else 0)
        bounds=ink.getbbox()
        assert bounds,name
        left,top,right,bottom=bounds
        pad=max(1,round((bottom-top)*.025))
        center=(left+right)/2
        half=(right-left)*.25
        tile=tile.crop((round(center-half),max(0,top-pad),round(center+half),min(tile.height,bottom+pad)))
    tile=tile.resize((32,32),Image.Resampling.NEAREST)
    target=ROOT/"src/main/resources/assets/combattraces/textures/vfx"/(name+".png")
    target.parent.mkdir(parents=True,exist_ok=True)
    tile.save(target)
    preview.paste(tile,(col*32,row*32))
preview.resize((512,512),Image.Resampling.NEAREST).save(ROOT/"art/pixel-effects-preview.png")
print(f"Exported {len(names)} RGBA sprites at 32x32 with nearest-neighbor sampling")

# Keep the optional replacement-pack example on the same pixel-art generation.
import shutil
shutil.copyfile(ROOT/"src/main/resources/assets/combattraces/textures/vfx/trails/fire.png",
                ROOT/"examples/resourcepack/assets/combattraces/textures/vfx/trails/slash.png")
