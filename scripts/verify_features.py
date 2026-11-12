"""Verify feature-specific packaging beyond the multi-platform scaffold contract."""
import json
import tomllib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines()
                  if "=" in line and not line.startswith("#"))
matrix = json.loads((root / "gradle/platforms.json").read_text(encoding="utf-8"))
targets = {f"{properties['mod_id']}-{value['directory']}-{properties['mod_version']}.jar": value["directory"]
           for value in matrix.values()}
jars = list((root / "build/libs").glob("*.jar"))
if not jars:
    raise SystemExit("No collected release JARs; run the root build first")

for jar in jars:
    target = targets[jar.name]
    loader = target.split("-", 1)[0]
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        if loader == "neoforge":
            metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
            assert any(item["config"] == "dont_hurt_them.mixins.json" for item in metadata["mixins"]), "Mixin metadata missing"
        elif loader == "forge":
            manifest = archive.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "").replace("\n ", "")
            assert "MixinConfigs: dont_hurt_them.mixins.json" in manifest, "Forge Mixin manifest missing"
            assert "dont_hurt_them.refmap.json" in names, "Forge production Mixin mappings missing"
        else:
            metadata = json.loads(archive.read("fabric.mod.json"))
            configs = [item if isinstance(item, str) else item["config"] for item in metadata["mixins"]]
            assert "dont_hurt_them.mixins.json" in configs, "Fabric Mixin metadata missing"
        mixins = json.loads(archive.read("dont_hurt_them.mixins.json"))
        for name in mixins["mixins"] + mixins["client"]:
            path = (mixins["package"] + "." + name).replace(".", "/") + ".class"
            assert path in names, "Missing mixin class: " + path
        base = "assets/dont_hurt_them/"
        en = json.loads(archive.read(base + "lang/en_us.json"))
        zh = json.loads(archive.read(base + "lang/zh_cn.json"))
        assert en.keys() == zh.keys(), "Translation catalogs differ"
        assert en["item.dont_hurt_them.proof_of_friendship"], "Item translation missing"
        model = json.loads(archive.read(base + "models/item/proof_of_friendship.json"))
        assert model["textures"]["layer0"] == "dont_hurt_them:item/proof_of_friendship"
        assert archive.read(base + "textures/item/proof_of_friendship.png").startswith(b"\x89PNG\r\n\x1a\n")
        recipes = [name for name in ("data/dont_hurt_them/recipe/proof_of_friendship.json",
                                    "data/dont_hurt_them/recipes/proof_of_friendship.json") if name in names]
        assert len(recipes) == 1, "Expected one version-appropriate recipe"
        recipe = json.loads(archive.read(recipes[0]))
        assert recipe["result"].get("id", recipe["result"].get("item")) == "dont_hurt_them:proof_of_friendship"
        assert recipe["pattern"] == ["R R", "ILI", "GGG"] and recipe["result"]["count"] == 1
        ingredients = {key: value if isinstance(value, str) else value["item"] for key, value in recipe["key"].items()}
        assert ingredients == {"R": "minecraft:redstone", "I": "minecraft:iron_nugget", "L": "minecraft:lapis_lazuli", "G": "minecraft:gold_nugget"}
        item_definition = "assets/dont_hurt_them/items/proof_of_friendship.json"
        if (root / "platforms" / target / "src/main/resources" / item_definition).is_file():
            definition = json.loads(archive.read(item_definition))
            assert definition["model"] == {"type": "minecraft:model", "model": "dont_hurt_them:item/proof_of_friendship"}
        assert not any("GameTests" in name or "RulesTest" in name or name.endswith(("/structure/empty.nbt", "/structures/empty.nbt")) for name in names), "Test artifact leaked into release"
        for source in (root / "platforms" / target / "src/gameTest/java").rglob("*.java"):
            stem = source.relative_to(root / "platforms" / target / "src/gameTest/java").with_suffix("").as_posix()
            assert not any(name == stem + ".class" or name.startswith(stem + "$") for name in names), "GameTest fixture leaked into release"
        assert "dont_hurt_them.test.mixins.json" not in names, "Test Mixin configuration leaked into release"
        assert "dont_hurt_them.client_regression.mixins.json" not in names, "Client regression Mixin configuration leaked into release"
        print(jar.name + ": mixins, bilingual catalogs, item model/texture, recipe and test exclusion passed")
