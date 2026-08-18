# Lucky drops

Lucky drops are JSON files in `data/lucky/drops/<identifier>.json`.

```json
{
  "luck": 2,
  "weight": 1.0,
  "drop": [
    {"message": "<gold>Lucky!"},
    {"item": {"type": "minecraft:diamond", "amount": 3}}
  ]
}
```

`luck` is required. `weight` defaults to `1.0` and must be positive. `drop` is
required and may be one action object or an array; arrays run in order.

## Luck selection

Drop selection uses AlexSocha Lucky Block's original exponential luck formula.
Traditional packs using `-2` through `2` use that formula unchanged. Extended
packs may use any outcome value from `-100` through `100`; these values are
internally mapped onto AlexSocha's five-tier scale before the same formula is
applied. Thus a block at `+100` strongly favors `+100`/high-luck outcomes but
still rolls the rest of the eligible pool; `-100` mirrors that behavior toward
punishments. Use `weight` to tune outcomes within the same luck tier.

## Actions

All actions may use `delay` (ticks, clamped to zero or greater). Position-aware
actions accept `pos`/`anchor`, `posOffset`, and `x`, `y`, `z`. Valid positions
include `player`, `#pPos`, `block`, and structure anchors such as `#sPos(x,y,z)`.
Offsets accept `[x,y,z]`, `"x,y,z"`, `#pOffset(x,z)`, and `#circleOffset(radius)`.

| Action | Key fields |
|---|---|
| `message` | formatted message text |
| `command` | command text |
| `item` | `type`/`id`, `amount`, `components`, `nbt` |
| `block` | `type`/`id`, `state`, `nbt`, `mode`, `lootTable`, `customDrop` |
| `entity` | `type`, `amount`, `nbt` |
| `repeat` | `amount`, required `drops` |
| `random` | `amount`, required `drops` array; selection has no duplicates |
| `fill` | `type`/`id`, `xSize`, `ySize`, `zSize` |
| `explosion` | `power`/`size`, `fuse`, position fields |
| `sound` | `id`/`type` |
| `particle` | `id`/`type`, `amount`/`particleAmount` |
| `structure` | `id`/`type`, rotation and placement fields |
| `time` / `difficulty` | vanilla value to set |
| `effect` | `id`/`type`, `duration`, `amplifier`, `target` |
| `impulse` | `vector`, or `x`, `y`, `z`; optional `target` |

`group` and `legacy` are not supported.

## Explosions

```json
{"explosion": {"power": 6.0, "fuse": 20, "posOffset": [0, 2, 0]}}
```

`power` defaults to `4.0` and must be positive; `size` is an alias. A fused
power-4 explosion creates primed TNT. Other fused explosions run after the
delay. Explosions respect the `tntExplodes` game rule.

## Effects and impulses

```json
{"effect": {"id": "minecraft:strength", "duration": 200, "amplifier": 1}}
```

Targets default to `player`; `#nearbyPlayers(radius)` and
`#nearbyEntities(radius)` are also supported.

```json
{"impulse": {"vector": "#calc(#pLookVector*0.5)"}}
```

## Templates

Templates include `#random(min,max)`, `#randList(...)`, `#index`, `#pName`,
`#pUUID`, `#pYaw`, `#pPitch`, and `#bPosX`/`#bPosY`/`#bPosZ`.
`#calc(...)` supports arithmetic, vector literals (`[x,y,z]`), and
`#pLookVector` with scalar multiplication.
