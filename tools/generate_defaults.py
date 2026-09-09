"""Regenerate built-in JSON definitions, mirrored as client-only fallback resources."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
def definition(section, name, value):
    for kind in ("assets", "data"):
        p = ROOT/kind/"combattraces/combat_traces"/section/(name+".json")
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(value, indent=2)+"\n", encoding="utf-8")
def texture(kind, name): return "combattraces:textures/vfx/"+kind+"/"+name+".png"
for name, width, opacity in [("slash",1,.78),("heavy_slash",1,.85),("blunt",1,.75),("fire",1.04,.62),("frost",1.03,.62),("lightning",1.02,.65),("arcane",1.04,.55)]:
    definition("trails",name,dict(texture=texture("trails",name),render_mode="translucent" if name in ("slash","heavy_slash","blunt") else "additive",
        lifetime_ms=180 if name=="slash" else 220,width_multiplier=width,opacity=opacity,uv_scroll_speed=.8 if name in ("fire","arcane") else 0,fade_curve="ease_out",
        particle="minecraft:small_flame" if name=="fire" else "minecraft:snowflake" if name=="frost" else "minecraft:electric_spark" if name=="lightning" else "minecraft:enchanted_hit",
        particle_rate=.15 if name in ("fire","frost","lightning","arcane") else 0))
for name, lifetime, scale in [("slash",200,1.55),("cleave",220,1.8),("blunt",200,1.3),("pierce",180,1),("claw",150,1),("whip",160,1.1),("generic",180,.8),("ring",150,.8),("spark",100,.55)]:
    definition("impacts",name,dict(texture=texture("impacts",name),render_mode="additive",lifetime_ms=lifetime,scale=scale,opacity=1,camera_bias=.85,fade_curve="linear"))
elements = {
    "physical": ("slash","generic","minecraft:crit","ffffff"),
    "fire": ("fire","slash","minecraft:small_flame","ff8c36"),
    "frost": ("frost","pierce","minecraft:snowflake","94e5ff"),
    "lightning": ("lightning","generic","minecraft:electric_spark","bedaff"),
    "poison": ("slash","generic","minecraft:spore_blossom_air","81d646"),
    "arcane": ("arcane","generic","minecraft:enchanted_hit","bd8bff"),
    "holy": ("slash","ring","minecraft:end_rod","fff2b3"),
    "shadow": ("arcane","slash","minecraft:smoke","8258b0"),
}
for name,(trail,impact,particle,color) in elements.items():
    definition("impacts",name+"_overlay",dict(texture=texture("impacts",impact),render_mode="additive",lifetime_ms=200,scale=1.08,opacity=.5,color=color,camera_bias=.3,fade_curve="ease_out"))
    condition = {"item_tag":"combattraces:"+name+"_weapons"}
    if name=="fire": condition={"or":[condition,{"enchantment":"minecraft:fire_aspect"}]}
    definition("elements",name,dict(id="combattraces:"+name,priority=100 if name=="fire" else 0,conditions=condition,trail="combattraces:"+trail,impact="combattraces:"+name+"_overlay",particle=particle,color=color))
    p=ROOT/"data/combattraces/tags/item"/(name+"_weapons.json");p.parent.mkdir(parents=True,exist_ok=True);p.write_text('{"replace":false,"values":[]}\n')
classes={"slash":["minecraft:swords"],"cleave":["minecraft:axes"],"blunt":["combattraces:blunt_weapons"],"pierce":["combattraces:piercing_weapons"],"claw":["combattraces:claw_weapons"],"whip":["combattraces:whip_weapons"],"magic":["combattraces:magic_weapons"]}
for name,tags in classes.items():
    definition("classifications",name,dict(priority=-100 if name in ("slash","cleave") else 0,item_tags=tags,weapon_class="combattraces:"+name))
    if tags[0].startswith("combattraces:"):
        values=["minecraft:mace"] if name=="blunt" else ["minecraft:trident"] if name=="pierce" else []
        p=ROOT/"data/combattraces/tags/item"/(tags[0].split(":")[1]+".json");p.write_text(json.dumps(dict(replace=False,values=values),indent=2)+"\n")
materials={
 "metal":dict(priority=100,block_tags=["minecraft:anvil","c:storage_blocks/iron","c:storage_blocks/copper","c:storage_blocks/gold","c:storage_blocks/netherite"],entities=["minecraft:iron_golem"],armor_tags=["combattraces:metal_armor"],particle="minecraft:electric_spark",color="ffd188",particle_count=7),
 "glass":dict(priority=90,block_tags=["c:glass_blocks","c:glass_panes"],particle="minecraft:end_rod",color="c3f5ff",particle_count=4),
 "ice":dict(priority=85,block_tags=["minecraft:ice"],particle="minecraft:snowflake",color="9edfff",particle_count=5),
 "foliage":dict(priority=80,block_tags=["minecraft:leaves","minecraft:flowers","minecraft:saplings"],particle="minecraft:spore_blossom_air",color="8cc266",particle_count=4),
 "water":dict(priority=80,blocks=["minecraft:water","minecraft:bubble_column"],particle="minecraft:splash",color="75b5f5",particle_count=4),
 "wood":dict(priority=20,block_tags=["minecraft:logs","minecraft:planks","minecraft:wooden_fences","minecraft:wooden_slabs","minecraft:mineable/axe"],entities=["minecraft:armor_stand"],particle="minecraft:crit",color="bf976d",particle_count=5),
 "dirt":dict(priority=15,block_tags=["minecraft:dirt","minecraft:sand"],particle="minecraft:poof",color="a88c6b",particle_count=4),
 "stone":dict(priority=-50,block_tags=["minecraft:mineable/pickaxe"],particle="minecraft:crit",color="bab6ad",particle_count=6),
 "flesh":dict(priority=-100,particle="minecraft:crit",color="fff1d6",particle_count=2),
 "generic":dict(priority=-1000,particle="minecraft:crit",color="ffffff",particle_count=2),
}
for name,value in materials.items(): definition("materials",name,dict(material="combattraces:"+name,**value))
armor=[f"minecraft:{material}_{part}" for material in ("iron","golden","chainmail","netherite") for part in ("helmet","chestplate","leggings","boots")]
(ROOT/"data/combattraces/tags/item/metal_armor.json").write_text(json.dumps(dict(replace=False,values=armor),indent=2)+"\n")
