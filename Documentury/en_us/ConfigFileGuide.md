# ZombieGameReborn Configuration Guide (English)

> A comprehensive configuration manual for pack authors, server admins, and advanced players.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Config Warnings & Best Practices](#config-warnings--best-practices)
- [Top-Level Config Structure](#top-level-config-structure)
- [Stage System](#stage-system)
- [Zombie Properties](#zombie-properties)
- [Zombie Spawn Selector](#zombie-spawn-selector)
- [Mob Replacement System](#mob-replacement-system)
- [Zombie Types](#zombie-types)
- [Equipment Guide](#equipment-guide)
- [FAQ](#faq)

---

## Quick Start

### Config File Locations & Purpose

| # | Path | Scope | Purpose |
|---|------|-------|---------|
| 1 | `<world>/serverconfig/zgr_game_property.json` | **Server runtime** | Per-world config. Edited by OPs in-game and saved to server |
| 2 | `config/zombiegamereborn/default_game_property.json` | **New world first launch** | Client-side global default template. Used when no serverconfig exists |
| 3 | `config/zombiegamereborn/game_properties/*.json` | **Import/Export GUI** | Local preset files managed via the in-game preset manager |
| 4 | (in mod jar) `/data/.../initial_default.json` | **Import/Export GUI** | Built-in initial default preset, shown as **[Preset]** in the manager |
| 5 | (hardcoded) `GamePropertyPresentUtils` | **Import/Export GUI** | Three hardcoded presets: Global Default, Initial Default, Disabled |

> Config fallback chain: `serverconfig/zgr_game_property.json` → client `default_game_property.json` → `GameProperty.empty()`

### Minimal Config

```json
{
  "stage_properties": [
    {
      "day": 1.0,
      "zombie_property": {},
      "zombie_spawn_chooser": {
        "zombie_types": [
          { "spawn_type": "normal", "chance": 1.0, "zombie_type": "zombiegamereborn:vanilla" },
          { "spawn_type": "drowned", "chance": 1.0, "zombie_type": "zombiegamereborn:vanilla" },
          { "spawn_type": "blood_moon", "chance": 1.0, "zombie_type": "zombiegamereborn:vanilla" }
        ]
      },
      "replace_chance": 0.0,
      "remove_chance": 0.0,
      "blood_moon_chance": 0.0,
      "zombie_count_modify": 1.0,
      "holy_cleansing": false
    }
  ],
  "can_zombie_break_block": true,
  "can_zombie_place_block": true,
  "can_piglin_infection": true,
  "mob_replacement": { "mobs": [] },
  "keep_mob_loot_table": true,
  "max_empowered_builder_count": 100,
  "max_empowered_miner_count": 100,
  "disable_turtle_egg_seeking": false
}
```

### ⚠️ Important Notes

| Parameter | Description | Performance |
|-----------|-------------|-------------|
| `max_empowered_builder_count` | Max active builder zombies | Solo~30, Small MP~50, Large~80 |
| `max_empowered_miner_count` | Max active miner zombies | Same as above |
| `disable_turtle_egg_seeking` | Disable zombie turtle egg seeking | Enable on coastal maps to save CPU |

---

## Config Warnings & Best Practices

### Stage Design

- **10 stages over 100 days**: Set day to 1, 11, 21, 31...91 for a smooth difficulty curve
- **All 3 spawn_types must have entries**: `normal`, `drowned`, `blood_moon` pools each need at least one valid entry, otherwise they fall back to `dummy` (no AI)
- **`remove_chance` increases over stages**: From 0% up to 80%, never 100% (keep some ecosystem)
- **`replace_chance` should be 100%**: Ensures all replace-marked mobs become zombies

### Zombie Stat Safety Thresholds

- **`movement_speed_modify`**: Do not exceed 1.4 — pathfinding breaks and zombies drift
- **Don't change `max_health`**: Adjust `armor` (0–30) and `armor_toughness` (0–20) instead
- **Don't touch `baby_probability`**: Baby zombies pathfind incorrectly at high speed

### Zombie Type Deployment Timing

- **Bow/Crossbow/Shield users are ultra-late game**: Introduce after day 70, total weight ≤ 5%
- **Builder/Miner are highly destructive**: Place in blood_moon pools and late stages (60d+) only, control with `max_empowered_*_count`

### mob_replacement Usage

- **Use the full default list, don't trim it**: Removing entries causes non-zombie mobs to reappear
- **Replace entries + `replace_chance: 1.0` = 100% zombie world**

---

## Top-Level Config Structure

```json
{
  "stage_properties": [...],           // Stage list (core)
  "can_zombie_break_block": true,      // Global: can zombies break blocks
  "can_zombie_place_block": true,      // Global: can zombies place blocks
  "can_piglin_infection": true,        // Zombies infect piglins (→zombified) and hoglins (→zoglins)
  "mob_replacement": { ... },          // Mob replacement rules
  "keep_mob_loot_table": true,         // true=keep original loot on replace; false=use zombie default loot
  "max_empowered_builder_count": 100,  // Builder cap
  "max_empowered_miner_count": 100,    // Miner cap
  "disable_turtle_egg_seeking": false  // Disable turtle egg seeking (performance)
}
```

---

## Stage System

### Design Philosophy

Stages let you **dynamically adjust zombie strength, behavior, and spawning** based on in-game days. Multiple stages form a timeline, and the game automatically matches the current day to the correct stage.

### Stage Matching Rules

```text
Given:
  Stage A: day=1
  Stage B: day=5
  Stage C: day=10

Day 1.0 ~ Day 4.99 → Stage A
Day 5.0 ~ Day 9.99 → Stage B
Day 10.0+          → Stage C
```

> The effective range is: `current day ≥ stage.day AND < next stage.day` (for the last stage, `≥ stage.day` applies forever)

> ⚠️ `<=` is inclusive, so day 5 immediately switches to Stage B.

### StageProperty Structure

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `day` | double | [1.0, ∞) | Stage start day |
| `zombie_property` | object | — | Zombie attribute config for this stage |
| `zombie_spawn_chooser` | object | — | Zombie type selector |
| `replace_chance` | double | [0.0, 1.0] | Non-zombie → zombie replacement chance (only affects mobs not locked by `mob_replacement`) |
| `remove_chance` | double | [0.0, 1.0] | Non-zombie removal chance (does NOT affect mobs locked as remove/replace in `mob_replacement`) |
| `blood_moon_chance` | double | [0.0, 1.0] | Blood moon trigger chance |
| `zombie_count_modify` | double | [0.0, ∞) | Zombie spawn count multiplier |
| `holy_cleansing` | boolean | true/false | All zombies are set on fire and **no new zombies spawn**. ⚠️ **Do not use in normal gameplay** |

---

## Zombie Properties

### Base Attributes

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `max_health` | 20.0 | [1.0, ∞) | Max health |
| `attack_damage_modify` | 1.0 | [0.0, ∞) | Attack damage **multiplier** (× base damage) |
| `movement_speed_modify` | 1.0 | [0.0, ∞) | Movement speed multiplier |
| `armor` | 2.0 | [0.0, 30.0] | Armor value |
| `armor_toughness` | 0.0 | [0.0, 20.0] | Armor toughness |
| `knockback_resistance` | 0.0 | [0.0, 1.0] | Knockback resistance |
| `follow_range` | 40.0 | [0.0, ∞) | Follow range |
| `mining_speed_modify` | 1.0 | [0.0, ∞) | Mining speed multiplier |

### Behavior Toggles

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `follow_must_see` | true | true/false | Must see target to aggro; `false` = zombies sense players through walls |
| `flee_sun` | false | true/false | Flee from sunlight |
| `can_jump_attack` | false | true/false | Can jump-attack |
| `do_swimming_zombie_convert` | false | true/false | Swimming zombies don't convert to drowned (protection) |
| `enhanced_sense` | false | true/false | Enable advanced sensing |
| `can_zombie_guard_continue_use_weapons` | false | true/false | Infected guards retain weapons |
| `enable_piglin_collision_anger` | false | true/false | Colliding with zombified piglins angers them |
| `piglin_angry_mode` | false | true/false | Zombified piglins attack everything indiscriminately |

### Probability Values

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `can_swim_probability` | 0.0 | [0.0, 1.0] | Individual swimming chance |
| `sun_immunity_probability` | 0.0 | [0.0, 1.0] | Sun immunity chance |
| `fire_immune_probability` | 0.0 | [0.0, 1.0] | Fire immunity chance |
| `baby_probability` | 0.05 | [0.0, 1.0] | Baby zombie chance |
| `can_pick_up_loot_coefficient` | 0.55 | [0.0, ∞) | Item pickup coefficient |

### Advanced Sensing (requires `enhanced_sense: true`)

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `sense_bleeding_radius` | 64.0 | [0.0, ∞) | Player bleeding sense radius |
| `sense_bleeding_lifespan` | 400 (≈20s) | [1, ∞) | Sense duration (20 tick = 1s) |
| `sense_block_radius` | 16.0 | [0.0, ∞) | Block break sense radius |
| `sense_block_lifespan` | 100 (≈5s) | [1, ∞) | Sense duration |
| `sense_gun_shot_radius` | 64.0 | [0.0, ∞) | Gunshot sense radius |
| `sense_gun_shot_lifespan` | 400 (≈20s) | [1, ∞) | Sense duration |
| `sense_gun_shot_silenced_radius` | 16.0 | [0.0, ∞) | Silenced gunshot sense radius |
| `sense_gun_shot_silenced_lifespan` | 100 (≈5s) | [1, ∞) | Sense duration |

### Volume Control

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `ambient_volume_modify` | 1.0 | [0.0, 1.0] | Ambient volume multiplier |
| `step_volume_modify` | 1.0 | [0.0, 1.0] | Step volume multiplier |

### Equipment

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `equipment_quality_mean_offset` | 0.0 | [0.0, 128.0] | Equipment quality offset |
| `equipment_probability_factor` | 1.0 | [0.0, 128.0] | Equipment drop probability factor |
| `equipment_enchantment_factor` | 1.0 | [0.0, 128.0] | Enchantment probability factor |

### Mod Integration

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `musket_mod_gun_damage_modify` | 0.5 | [0.0, ∞) | Musket damage modifier |
| `piglin_collision_anger_chance` | 0.25 | [0.0, 1.0] | Collision anger chance |
| `max_empowered_zombie_miner_count` | 100 | [0, ∞) | Max miner count (**reserved, not active** — use top-level field) |
| `max_empowered_zombie_builder_count` | 100 | [0, ∞) | Max builder count (**reserved, not active** — use top-level field) |

---

## Zombie Spawn Selector

### Structure

```json
{
  "zombie_spawn_chooser": {
    "zombie_types": [
      { "spawn_type": "normal", "chance": 1.0, "zombie_type": "zombiegamereborn:vanilla" }
    ]
  }
}
```

### Three Spawn Types

| `spawn_type` | Trigger | Notes |
|---|---|---|
| `normal` | Natural spawn | Zombies, husks, zombie villagers |
| `drowned` | Drowned conversion | Recommended: use `vanilla` only |
| `blood_moon` | Blood moon active | Overrides normal during blood moon |

### Weight System

- Each `spawn_type` has an **independent weighted pool**. Total weights don't need to sum to 1. Weighted random selection (`1.0+1.0` = 50/50, `1.0+3.0` = 25%/75%)
- During blood moon: **`normal` pool is completely ignored**, only `blood_moon` entries are used
- If `blood_moon` pool is empty → falls back to `dummy` (no AI zombies)

---

## Mob Replacement System

### Structure

```json
{
  "mob_replacement": {
    "mobs": [
      { "mob_id": "minecraft:cow", "action": "remove" },
      { "mob_id": "minecraft:skeleton", "action": "replace" }
    ]
  }
}
```

### Actions

| Action | Effect |
|--------|--------|
| `remove` | Mob won't spawn naturally |
| `replace` | Mob is replaced by a zombie (biome-dependent variant) |

### Default Replacement List

> `mob_replacement` uses a `HashMap`. If the same `mob_id` appears multiple times, only the **last entry** takes effect.

<details>
<summary>Click to expand full list</summary>

> ⚠️ `musket_mod_gunner` requires **Musket Mod**, `zombie_guard_villager` requires **Guard Villagers**. If the mod is missing, the type safely degrades to melee with no errors.

```json
{
  "mobs": [
    { "mob_id": "minecraft:stray", "action": "replace" },
    { "mob_id": "minecraft:creeper", "action": "replace" },
    { "mob_id": "minecraft:dolphin", "action": "remove" },
    { "mob_id": "minecraft:polar_bear", "action": "remove" },
    { "mob_id": "minecraft:turtle", "action": "remove" },
    { "mob_id": "minecraft:cave_spider", "action": "replace" },
    { "mob_id": "minecraft:wolf", "action": "remove" },
    { "mob_id": "minecraft:phantom", "action": "remove" },
    { "mob_id": "minecraft:ocelot", "action": "remove" },
    { "mob_id": "minecraft:wither_skeleton", "action": "replace" },
    { "mob_id": "minecraft:goat", "action": "remove" },
    { "mob_id": "minecraft:mule", "action": "remove" },
    { "mob_id": "minecraft:fox", "action": "remove" },
    { "mob_id": "mekanismadditions:baby_skeleton", "action": "replace" },
    { "mob_id": "minecraft:hoglin", "action": "replace" },
    { "mob_id": "mekanismadditions:baby_wither_skeleton", "action": "replace" },
    { "mob_id": "touhou_little_maid:entity.monster.fairy", "action": "replace" },
    { "mob_id": "minecraft:pig", "action": "remove" },
    { "mob_id": "minecraft:rabbit", "action": "remove" },
    { "mob_id": "minecraft:magma_cube", "action": "replace" },
    { "mob_id": "minecraft:frog", "action": "remove" },
    { "mob_id": "minecraft:sheep", "action": "remove" },
    { "mob_id": "minecraft:ghast", "action": "replace" },
    { "mob_id": "minecraft:cat", "action": "remove" },
    { "mob_id": "minecraft:camel", "action": "remove" },
    { "mob_id": "minecraft:llama", "action": "remove" },
    { "mob_id": "minecraft:chicken", "action": "remove" },
    { "mob_id": "mekanismadditions:baby_stray", "action": "replace" },
    { "mob_id": "minecraft:slime", "action": "remove" },
    { "mob_id": "minecraft:panda", "action": "remove" },
    { "mob_id": "minecraft:axolotl", "action": "remove" },
    { "mob_id": "minecraft:sniffer", "action": "remove" },
    { "mob_id": "mekanismadditions:baby_enderman", "action": "replace" },
    { "mob_id": "minecraft:bee", "action": "remove" },
    { "mob_id": "minecraft:mooshroom", "action": "remove" },
    { "mob_id": "minecraft:donkey", "action": "remove" },
    { "mob_id": "minecraft:cow", "action": "remove" },
    { "mob_id": "minecraft:horse", "action": "remove" },
    { "mob_id": "minecraft:skeleton", "action": "replace" },
    { "mob_id": "minecraft:spider", "action": "replace" },
    { "mob_id": "minecraft:parrot", "action": "remove" },
    { "mob_id": "minecraft:witch", "action": "replace" },
    { "mob_id": "minecraft:glow_squid", "action": "remove" },
    { "mob_id": "minecraft:enderman", "action": "replace" },
    { "mob_id": "minecraft:squid", "action": "remove" }
  ]
}
```
</details>

---

## Zombie Types

| ID | Melee | Breaks | Places | Specialty |
|----|:-----:|:------:|:------:|-----------|
| `zombiegamereborn:dummy` | ❌ | ❌ | ❌ | No AI, stands still |
| `zombiegamereborn:vanilla` | ✅ | ❌ | ❌ | Pure vanilla behavior |
| `zombiegamereborn:enhanced_vanilla` | ✅ | ❌ | ❌ | Enhanced melee AI |
| `zombiegamereborn:builder` | ✅ | ✅ | ✅ | Break+place+bridge |
| `zombiegamereborn:miner` | ✅ | ✅ | ❌ | Smart digging |
| `zombiegamereborn:bow_attacker` | ❌ | ❌ | ❌ | Bowman |
| `zombiegamereborn:crossbow_attacker` | ❌ | ❌ | ❌ | Crossbowman |
| `zombiegamereborn:shield_user` | ✅ | ❌ | ❌ | Shield block+bash |
| `zombiegamereborn:musket_mod_gunner` | ❌ | ❌ | ❌ | Musket gunner (needs Musket Mod) |
| `zombiegamereborn:zombie_guard_villager` | ❌ | ❌ | ❌ | All-weapon infected guard |

---

## Equipment Guide

### Three Core Parameters

| Parameter | Effect | Default | Recommended |
|-----------|--------|---------|-------------|
| `equipment_quality_mean_offset` | Equipment material quality | 0 | -2 ~ +10 |
| `equipment_probability_factor` | Equipment spawn chance | 1.0 | 0 ~ 10 |
| `equipment_enchantment_factor` | Enchantment chance | 1.0 | 0 ~ 3 |

For full equipment formulas and distribution tables, see the Chinese version of this guide.

---

## FAQ

### Q1: Does the stage array need to be sorted by day?
**A**: No. The code auto-sorts by day internally.

### Q2: What if `remove_chance` is set to 100%?
**A**: All non-zombie mobs will be removed. This is not recommended as it breaks game progression.

### Q3: Same mob_id appears multiple times in mob_replacement?
**A**: Last entry wins (HashMap behavior).

### Q4: What if blood_moon pool is empty?
**A**: Falls back to `dummy` zombies (no AI). Won't crash.

### Q5: JSON syntax error in config file?
**A**: Won't crash the server. The mod logs an error and uses default config.

### Q6: Can I delete top-level fields?
**A**: Yes. Missing fields use their Java code defaults.

---

> **Document version**: 1.0  
> **Last updated**: 2026-07-13
