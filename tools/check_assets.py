"""Validate all pack JSON against its schema and inspect the production JAR."""
import json, pathlib, sys, zipfile
import jsonschema
from PIL import Image
ROOT=pathlib.Path(__file__).resolve().parents[1]
mapping={"trails":"style","impacts":"style","weapons":"weapon","classifications":"weapon","animations":"animation","elements":"element","materials":"material"}
count=0
for base in [ROOT/"src/main/resources",ROOT/"examples/datapack"]:
 for p in base.rglob("*.json"):
  if "combat_traces" not in p.parts: continue
  section=p.parts[p.parts.index("combat_traces")+1]
  schema=json.loads((ROOT/"docs/schema"/(mapping[section]+".schema.json")).read_text())
  jsonschema.Draft202012Validator(schema).validate(json.loads(p.read_text()))
  count+=1
assets=ROOT/"src/main/resources/assets/combattraces/textures/vfx"
textures=list(assets.rglob("*.png"))
assert len(textures)==16
for p in textures:
 im=Image.open(p)
 assert im.mode=="RGBA",p
 assert im.size==(32,32),p
 assert im.getchannel("A").getextrema()[0]==0,p
 assert im.getchannel("A").getextrema()[1]>0,p
version=next(line.split("=",1)[1].strip() for line in (ROOT/"gradle.properties").read_text().splitlines() if line.startswith("mod_version="))
jar=ROOT/"build/libs"/f"combattraces-{version}.jar"
with zipfile.ZipFile(jar) as archive:
 names=archive.namelist()
 assert "com/cappleapple/combattraces/CombatTraces.class" in names
 assert not any("/validation/" in name or "combattraces_validation" in name or name.endswith(".jar") for name in names)
 for section in ("trails","impacts","elements","materials","classifications"):
  assert any(name.startswith("data/combattraces/combat_traces/"+section+"/") for name in names)
 metadata=archive.read("META-INF/neoforge.mods.toml").decode()
 assert 'modId="combattraces"' in metadata and f'version="{version}"' in metadata
print(f"PASS {count} definitions, {len(textures)} RGBA textures, production JAR contents")
