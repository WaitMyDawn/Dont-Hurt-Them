"""Write an empty GameTest structure without an NBT library dependency."""
import gzip
import struct
from pathlib import Path

def string(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded

def tag(kind, name, value):
    return bytes([kind]) + string(name) + value

integer = lambda value: struct.pack(">i", value)
root = tag(3, "DataVersion", integer(3955))
root += tag(9, "size", b"\x03" + integer(3) + integer(8) + integer(6) + integer(8))
palette = tag(8, "Name", string("minecraft:air")) + b"\0"
root += tag(9, "palette", b"\x0a" + integer(1) + palette)
root += tag(9, "blocks", b"\x0a" + integer(0))
root += tag(9, "entities", b"\x0a" + integer(0))
path = Path(__file__).resolve().parents[1] / "platforms/neoforge-1.21.1/src/gameTest/resources/data/dont_hurt_them/structure/empty.nbt"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(b"\x0a\0\0" + root + b"\0", mtime=0))
print(path)
