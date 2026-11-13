"""Generate the bilingual UI catalog and the small, deterministic item sprite."""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "common/src/main/resources/assets/dont_hurt_them"
CATALOG = {
    "item.dont_hurt_them.proof_of_friendship": ("Proof of Friendship", "友谊之证"),
    "item.dont_hurt_them.proof_of_friendship.hint": ("Sneak + right-click: open your friendship album", "潜行并右键：打开自己的友谊图鉴"),
    "item.dont_hurt_them.proof_of_friendship.mark": ("Right-click an entity: add or remove a friend", "右键实体：加入或移出友谊清单"),
    "message.dont_hurt_them.added": ("Added %s to your friends", "已将 %s 加入友谊清单"),
    "message.dont_hurt_them.removed": ("Removed %s from your friends", "已将 %s 移出友谊清单"),
    "message.dont_hurt_them.full": ("Your friendship album is full (256 entries)", "友谊清单已满（256 项）"),
    "message.dont_hurt_them.invalid_type": ("Unknown entity registry ID; use namespace:path", "未知实体注册名，请输入 namespace:path"),
    "message.dont_hurt_them.player_type_forbidden": ("Add players individually with Proof of Friendship.", "请手持友谊之证，逐个右键添加玩家。"),
    "message.dont_hurt_them.exists": ("That entity type is already a friend", "此实体类型已在友谊清单中"),
    "message.dont_hurt_them.rename_failed": ("This friend must be alive, loaded and in your album; use a name of 1–64 characters.", "只能修改清单内已加载且存活的实体名称，名称须为 1–64 个字符。"),
    "message.dont_hurt_them.revived": ("Revived friend %s", "已复活友谊实体 %s"),
    "message.dont_hurt_them.no_death_record": ("No eligible death record in your own friendship album.", "自己的友谊清单中没有符合条件的死亡记录。"),
    "message.dont_hurt_them.player_revive_forbidden": ("Players cannot be revived with this command.", "不能使用此指令复活玩家。"),
    "message.dont_hurt_them.non_living_revive_forbidden": ("Nonliving entities cannot be revived.", "非生物实体不能复活。"),
    "message.dont_hurt_them.grave_unloaded": ("The friend's grave is not loaded.", "朋友墓地未加载"),
    "message.dont_hurt_them.invalid_position": ("Invalid revival position.", "复活位置无效。"),
    "message.dont_hurt_them.cleared_deceased": ("Removed %s deceased friends.", "已清理 %s 位故友。"),
    "message.dont_hurt_them.missing_dimension": ("The recorded dimension is unavailable.", "死亡记录所在维度不可用。"),
    "message.dont_hurt_them.invalid_snapshot": ("The entity snapshot cannot be restored with the installed entity types.", "实体快照无法使用当前安装的实体类型恢复。"),
    "message.dont_hurt_them.uuid_conflict": ("That UUID is still loaded; wait for the corpse to disappear or resolve the existing entity.", "该 UUID 仍已加载，请等待死亡实体消失，或检查已存在的同 UUID 实体。"),
    "message.dont_hurt_them.spawn_blocked": ("Entity restoration was blocked; the death record was retained.", "实体生成被拦截，死亡记录已保留。"),
}
SCREEN = {
    "title": ("Friendship Album", "友谊图鉴"),
    "individuals": ("Individuals", "特定实体"),
    "types": ("Entity types", "实体类型"),
    "favorites": ("Favorites", "特别关注"),
    "show_non_living": ("Show nonliving friends", "显示非生物朋友"),
    "hide_non_living": ("Hide nonliving friends", "不显示非生物朋友"),
    "only_non_living": ("Only nonliving friends", "仅显示非生物朋友"),
    "search": ("Search friends", "搜索朋友"),
    "search_name": ("Name", "名称"),
    "search_registry": ("Registry ID", "注册名"),
    "no_matches": ("No matching friends", "没有匹配的朋友"),
    "dead": ("Deceased", "已死亡"),
    "clear_deceased": ("Clear deceased", "清理故友"),
    "page_size": ("Cards/page", "每页数量"),
    "duration": ("Friendship: ", "友谊时长："),
    "duration_unknown": ("Friendship duration unknown", "友谊时长未知"),
    "days": (" game days", " 游戏日"),
    "rename": ("Rename friend", "修改友谊实体名称"),
    "unloaded_short": ("Not loaded", "未加载"),
    "legend": ("Green: individual · Blue: type · Star: pin", "绿色：特定实体 · 蓝色：实体类型 · 星标：置顶"),
    "registry": ("Entity registry ID", "实体注册名"),
    "completion_hint": ("Tab: complete · ↑/↓: select · Esc: hide", "Tab 补全 · ↑/↓ 选择 · Esc 收起"),
    "add": ("Add type", "加入类型"), "close": ("Close", "关闭"),
    "invalid": ("Unknown ID; use namespace:path", "注册名无效，请使用 namespace:path"),
    "player_type_forbidden": ("Add players individually with Proof of Friendship.", "请手持友谊之证，逐个右键添加玩家。"),
    "exists": ("Already in your album", "已在友谊清单中"),
    "full": ("Album is full (256 entries)", "清单已满（256 项）"),
    "sent": ("Request sent", "已发送添加请求"),
    "empty": ("Right-click a friend, or add a registry ID below", "右键实体添加朋友，或在下方输入实体注册名"),
    "no_preview": ("Preview unavailable", "暂无可用模型"),
    "live": ("Individual · live model", "特定实体 · 实时模型"),
    "remote": ("Individual · loaded remotely", "特定实体 · 远端已加载"),
    "unloaded": ("Individual · not loaded", "特定实体 · 未加载"),
    "type": ("Entity type · default model", "实体类型 · 默认模型"),
    "health": ("HP: ", "生命："), "other_dimension": ("Other dimension", "其他维度"),
    "controls": ("Star: pin/unpin · ×: remove", "星标：置顶或取消 · ×：移除"),
}
CATALOG.update({"screen.dont_hurt_them." + key: value for key, value in SCREEN.items()})
for index, locale in enumerate(("en_us", "zh_cn")):
    path = ROOT / "lang" / (locale + ".json")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({key: value[index] for key, value in CATALOG.items()}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# A gold-edged blue album with a pink heart; transparent pixels preserve the inventory silhouette.
sprite = [
    "................", "...gggggggggg...", "..gddddddddddg..", "..gdbbbbbbbbdg..",
    "..gdbppbbppbdg..", "..gdbppppppbdg..", "..gdbppppppbdg..", "..gdbbppppbbdg..",
    "..gdbbbppbbbdg..", "..gdbbbbbbbbdg..", "..gdbwwwwwwbdg..", "..gdbbbbbbbbdg..",
    "..gddddddddddg..", "..gwwwwwwwwwwg..", "...gggggggggg...", "................",
]
colors = {".": (0, 0, 0, 0), "g": (235, 193, 98, 255), "d": (43, 61, 78, 255),
          "b": (73, 133, 168, 255), "p": (250, 147, 169, 255), "w": (227, 239, 225, 255)}
raw = b"".join(b"\0" + b"".join(bytes(colors[pixel]) for pixel in row) for row in sprite)
def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
path = ROOT / "textures/item/proof_of_friendship.png"
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(png)
print("Generated two language files and the item texture")
