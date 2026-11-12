# Don't Hurt Them

Keep a personal friendship album and protect friends from player-caused harm.

## Installation

Install the matching JAR on both the client and server.

| Minecraft | Loader | Java | Additional dependency |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.3.22 | 17 | — |
| 1.21.1 | NeoForge 21.1.235 | 21 | — |
| 1.21.1 | Fabric Loader 0.20.1 | 21 | Fabric API 0.116.17+1.21.1 |
| 1.21.11 | Fabric Loader 0.20.1 | 21 | Fabric API 0.141.6+1.21.11 |
| 26.3 | Fabric Loader 0.20.1 | 25 | Fabric API 0.165.2+26.3 |

## Friendship album

Craft one **Proof of Friendship** with two redstone dust, two iron nuggets, one lapis lazuli and three gold nuggets. It is also available in the Tools & Utilities creative tab:

```text
R R
ILI
GGG
```

`R`: redstone dust · `I`: iron nugget · `L`: lapis lazuli · `G`: gold nugget.

- Hold it in either hand and sneak + right-click to open your own album.
- Right-click an entity without sneaking to add or remove that individual.
- Enter an exact `namespace:path` entity registry ID to protect every entity of that type. The compact input previews registered IDs as you type; use arrow keys or the wheel to select, Tab/Shift+Tab to complete or cycle, or click a candidate. Enter completes a partial ID first and adds the completed type on the next press. Escape hides the suggestions. Java inheritance and registry-name prefixes do not define membership.
- Click a star to pin an entry; click × to remove it. Pinned entries come first, then individuals before types, then registry ID, name and UUID in lexical order.
- The Individuals and Entity types buttons enable either category. Favorites is an additional filter, off by default. Deceased appears when a recorded friend has died; enabling it shows only recorded deaths.
- Portrait pages offer 2, 4 or 8 cards, defaulting to 4. The Cards/page button cycles the setting and saves it to `config/dont_hurt_them/dont_hurt_them-client.toml` (`cardsPerPage`). Card, font and control sizes adapt to the selected density and GUI scale.
- Hover a card for friendship duration in game days (one decimal place) and the individual's UUID. Click a loaded, living individual's name to rename it with name-tag naming and persistence behavior.
- While holding the item, individual friends have green outlines and type friends have blue outlines, visible through walls. Green takes precedence.
- Holding the item also shows friends' names within normal nameplate rendering range.

Each card has an entity portrait. Tracked individuals use their live client entity when available, including its current appearance. Otherwise the album uses a default model. Server-loaded individuals show current health and coordinates; same-dimension entries show distance. Unloaded or missing entities remain in the album with an explicit unavailable status. Opening the album does not load chunks.

Friendships and stars belong to the **player**, persist through death and reconnects, and do not depend on possessing any particular item stack. Only damage attributable to that player is prevented. Other players and natural hazards remain able to hurt the entity. Each player can store up to 256 entries.

## Recorded deaths and revival

Confirmed deaths are retained as gray individual cards, distinct from entities whose chunks are not loaded. The server records game time, dimension, identity, cause and the entity's serializable NBT state before death drops. An unloading or cancelled death does not create a death record.

With cheats enabled (permission level 2), a player can run:

```text
/dht revive <namespace:entity_type> <UUID>
```

Both arguments autocomplete only from that player's current individual friendships with eligible recorded deaths. Revival restores the original UUID, dimension, position and saved entity state, including variant, name, equipment, inventory, ownership and attributes; living entities return at full health with death/fire/fall transients cleared. The recorded chunk is loaded for this explicit command. A still-loaded corpse or conflicting UUID prevents duplication; wait for the corpse to disappear before retrying. Failed restoration preserves the record. Minecraft player entities use normal player respawn and cannot be cloned through this command.

Older entries have unknown friendship duration because their original join time was not recorded. Deaths that occurred before this feature cannot be reconstructed from an entity's absence. Only state actually saved by an entity's NBT contract can be restored; incomplete or unavailable entity snapshots are not offered for revival.

## Protection scope

The platform adapters intercept melee damage, owned projectiles, TNT/explosions, tamed attackers, harmful status effects, and non-living entities that use Minecraft's invulnerability contract. Owned projectiles skip friends during entity collision selection, continuing toward other targets or blocks without spending arrow piercing capacity. Arrows and tridents retain their normal item recovery and loyalty behavior. A fallback skips protected impacts without deleting the projectile. Protected targets are removed from player-caused explosion damage and knockback lists. Beneficial effects remain allowed.

Player block interactions and mining carry a cause through synchronous block changes, scheduled ticks, random ticks, fluid/fire spread and resulting entities. Provenance is saved for player-authored contact hazards (including lava, fire, cactus, magma, water and suffocating blocks). An enforcement mirror preserves a disconnected owner's rules for attributed lingering entities.

Damage with no observable player source cannot always be distinguished from natural damage. Arbitrary redstone machines, indirect terrain/fall traps, pre-existing effects applied before friendship, and other mods that discard provenance or directly edit health require additional integrations. These are not blanket environmental invulnerability rules. See [validation](docs/VALIDATION.md) for tested behavior and remaining runtime checks.

English and Simplified Chinese translations are included.

Each JAR targets exactly the Minecraft/loader pair in its filename. Friendship entries share a versioned schema, but transferring worlds or player files between loaders or Minecraft versions is not an automatic migration feature.

## License

See [LICENSE.md](LICENSE.md).