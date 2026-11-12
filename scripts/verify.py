#!/usr/bin/env python3
"""Verify scaffold release artifacts without loading Minecraft (Python 3.11+)."""

import argparse
import hashlib
from io import BytesIO
import json
from pathlib import Path
import re
import struct
import tomllib
from zipfile import BadZipFile, ZipFile


def properties(path):
    """Read the UTF-8 Java Properties subset emitted by scaffold.py."""
    result = {}
    for line in re.split(r"\r\n|\n|\r", path.read_text(encoding="utf-8")):
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or (len(line) - len(line.rstrip("\\"))) % 2 == 1:
            raise ValueError(f"Unsupported property syntax in {path}: {key}")
        def decode(match):
            token = match[1]
            return chr(int(token[1:], 16)) if token.startswith("u") else {"n": "\n", "r": "\r", "t": "\t", "f": "\f"}.get(token, token)
        result[key.strip()] = re.sub(r"\\(u[0-9a-fA-F]{4}|.)", decode, value)
    return result


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_jar(project, target, jar):
    shared = properties(project / "gradle.properties")
    local = properties(project / "platforms" / target / "gradle.properties")
    loader, minecraft = target.split("-", 1)
    java = int(local["platform_java"])
    root = project / "platforms" / target / "src/main"
    with ZipFile(jar) as archive:
        entries = archive.namelist()
        require(len(entries) == len(set(entries)), f"Duplicate ZIP entries in {jar}")
        require(archive.testzip() is None, f"Corrupt ZIP entry in {jar}")
        require(archive.read("LICENSE.md") == (project / "LICENSE.md").read_bytes(), "Embedded license differs")
        for source, version in ((project / "common/src/main", 17), (root, java)):
            for path in (source / "java").rglob("*.java"):
                relative = path.relative_to(source / "java").with_suffix(".class").as_posix()
                require(relative in entries, f"Missing class {relative}")
                bytecode = archive.read(relative)
                require(len(bytecode) >= 8 and bytecode[:4] == b"\xca\xfe\xba\xbe", f"Invalid class {relative}")
                require(struct.unpack(">H", bytecode[6:8])[0] <= version + 44, f"Bytecode target too new: {relative}")
        for path in (project / "common/src/main/resources").rglob("*"):
            if path.is_file():
                relative = path.relative_to(project / "common/src/main/resources").as_posix()
                require(archive.read(relative) == path.read_bytes(), f"Shared resource differs: {relative}")
        if loader == "fabric":
            metadata = json.loads(archive.read("fabric.mod.json"))
            for key, property_key in (("id", "mod_id"), ("version", "mod_version"), ("name", "mod_name"),
                                      ("description", "mod_description"), ("license", "mod_license")):
                require(metadata[key] == shared[property_key], f"Wrong Fabric metadata: {key}")
            require(metadata["authors"] == [shared["mod_authors"]], "Wrong Fabric authors")
            expected = {"minecraft": minecraft, "java": f">={java}", "fabricloader": ">=" + local["loader_version"],
                        "fabric-api": ">=" + local["fabric_api_version"]}
            require(all(metadata["depends"].get(k) == v for k, v in expected.items()), "Wrong Fabric dependency ranges")
            for side in ("main", "client"):
                for entrypoint in metadata["entrypoints"][side]:
                    require(entrypoint.replace(".", "/") + ".class" in entries, f"Missing {side} entrypoint")
            nested_classes = set()
            for nested in metadata.get("jars", []):
                name = nested["file"]
                require(name in entries and name.startswith("META-INF/jars/") and ".." not in Path(name).parts,
                        f"Missing or unsafe nested JAR: {name}")
                with ZipFile(BytesIO(archive.read(name))) as dependency:
                    nested_classes.update(dependency.namelist())
            require({"com/electronwill/nightconfig/core/Config.class", "com/electronwill/nightconfig/toml/TomlFormat.class"}
                    <= nested_classes, "Fabric config runtime dependencies are not bundled")
        else:
            descriptor = "META-INF/neoforge.mods.toml" if loader == "neoforge" else "META-INF/mods.toml"
            metadata = tomllib.loads(archive.read(descriptor).decode("utf-8"))
            require(metadata["modLoader"] == "javafml", "Wrong FML mod loader")
            mod = next((m for m in metadata["mods"] if m["modId"] == shared["mod_id"]), None)
            require(mod is not None, "Missing FML mod descriptor")
            for key, property_key in (("version", "mod_version"), ("displayName", "mod_name"), ("description", "mod_description")):
                require(mod[key] == shared[property_key], f"Wrong FML metadata: {key}")
            require(metadata["license"] == shared["mod_license"], "Wrong FML license")
            require(metadata["loaderVersion"] == local["fml_range"], "Wrong FML language-loader range")
            dependencies = {value["modId"]: value for value in metadata["dependencies"][shared["mod_id"]]}
            require(dependencies["minecraft"]["versionRange"] == f"[{minecraft}]", "Wrong Minecraft compatibility range")
            require(dependencies[loader]["versionRange"] == local["loader_range"], "Wrong loader compatibility range")
            if loader == "forge":
                require(json.loads(archive.read("pack.mcmeta"))["pack"]["pack_format"] > 0, "Missing Forge resource-pack format")
    return {"target": target, "jar": str(jar), "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(), "status": "passed"}


def verify_project(project, targets=None, release=False):
    project = Path(project).resolve()
    matrix = json.loads((project / "gradle/platforms.json").read_text(encoding="utf-8"))
    available = {entry["directory"] for entry in matrix.values()}
    selected = available if targets is None else set(targets)
    require(bool(selected) and selected <= available, "Unknown or empty verification target selection")
    shared = properties(project / "gradle.properties")
    expected = {target: f"{shared['mod_id']}-{target}-{shared['mod_version']}.jar" for target in available}
    if release:
        actual = {path.name for path in (project / "build/libs").glob("*.jar")}
        require(actual == set(expected.values()), f"Root release set differs: expected {sorted(expected.values())}, got {sorted(actual)}")
    results = []
    for target in sorted(selected):
        jar = project / ("build/libs" if release else f"platforms/{target}/build/libs") / expected[target]
        results.append(verify_jar(project, target, jar))
        if release:
            original = project / "platforms" / target / "build/libs" / expected[target]
            require(jar.read_bytes() == original.read_bytes(), f"Root collection differs: {target}")
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--target", action="append")
    parser.add_argument("--release", action="store_true", help="Verify exact root build/libs set and byte-for-byte collection")
    args = parser.parse_args()
    try:
        print(json.dumps(verify_project(args.project, args.target, args.release), indent=2))
    except (OSError, ValueError, KeyError, TypeError, BadZipFile) as error:
        parser.exit(1, f"Artifact verification failed: {error}\n")


if __name__ == "__main__":
    main()
