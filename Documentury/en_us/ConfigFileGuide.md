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

ZGR involves multiple `game_property` config files:

| # | Path | Scope | Purpose |
|---|------|-------|---------|
| 1 | `<world>/serverconfig/zgr_game_property.json` | **Server runtime** | Per-world config. Edited by OPs in-game and saved to server |
| 2 | `config/zombiegamereborn/default_game_property.json` | **New world first launch** | Client-side global default template. Used when no serverconfig exists. Delete this file after mod updates to regenerate defaults |
| 3 | `config/zombiegamereborn/game_properties/*.json` | **Import/Export GUI** | Local preset files saved/loaded via the in-game preset manager. Not related to server config |
| 4 | (hardcoded) `GamePropertyPresentUtils` | **Import/Export GUI** | Three hardcoded presets: Global Default, Initial Default (factory), Disabled. Always shown at the top as **[Preset]** |

> Config fallback chain: `serverconfig/zgr_game_property.json` → client `default_game_property.json` → `GameProperty.empty()` (code fallback)

### Minimal Config

```json
{
  "stage_properties": [
    {
      "day": 1.0,
      "zombie_property": {},
      "zombie_spawn_chooser": {
        "zombie_types": [
          {
            "spawn_type": "normal",
            "chance": 1.0,
            "zombie_type": "zombiegamereborn:vanilla"
          },
          {
            "spawn_type": "drowned",
            "chance": 1.0,
            "zombie_type": "zombiegamereborn:vanilla"
          },
          {
            "spawn_type": "blood_moon",
            "chance": 1.0,
            "zombie_type": "zombiegamereborn:vanilla"
          }
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
  "max_empowered_builder_count": 30,
  "max_empowered_miner_count": 30
}
```

### ⚠️ Important Notes

| Parameter | Description | Performance |
|-----------|-------------|-------------|
| `max_empowered_builder_count` | Max active builder zombies | Default 30 is smooth |
| `max_empowered_miner_count` | Max active miner zombies | Same as above |
| `disable_turtle_egg_seeking` | Disable zombie turtle egg seeking | Recommended to enable |

---

## Config Warnings & Best Practices

### Stage Design

- **10 stages over 100 days**: Set day to 1, 11, 21, 31...91 for a smooth difficulty curve
- **All 3 spawn_types must have entries**: `normal`, `drowned`, `blood_moon` pools each need at least one valid entry, otherwise they fall back to `dummy` (no AI)
- **`remove_chance` increases over stages**: From 0% up to 80%, never 100% (keep some passive mobs for world ecology)
- **`replace_chance` should be 100%**: Ensures all replace-marked mobs become zombies

### Zombie Stat Safety Thresholds

- **`movement_speed_modify`**: Do not exceed 1.4 — pathfinding breaks and zombies drift
- **Don't change `max_health`**: Adjust `armor` (0–30) and `armor_toughness` (0–20) instead
- **Don't touch `baby_probability`**: Baby zombies pathfind incorrectly at high speed, keep 0 or default 0.05

### Zombie Type Deployment Timing

- **Bow/Crossbow/Shield users are ultra-late game**: Introduce after day 70, total weight ≤ 5%
- **Builder/Miner are highly destructive**: Place in blood_moon pools and late stages (60d+) only, control with `max_empowered_*_count`

### mob_replacement Usage

- **Use the full default list, don't trim it**: Removing entries causes non-zombie mobs to reappear, breaking the apocalypse atmosphere
- **Replace entries + `replace_chance: 1.0` = 100% zombie world**

---

## Top-Level Config Structure

### GameProperty Fields

```json
{
  "stage_properties": [...],           // Stage list (core)
  "can_zombie_break_block": true,      // Global: can zombies break blocks
  "can_zombie_place_block": true,      // Global: can zombies place blocks
  "can_piglin_infection": true,        // Zombies infect piglins (→zombified) and hoglins (→zoglins)
  "mob_replacement": { ... },          // Mob replacement rules
  "keep_mob_loot_table": true,         // true=keep original loot on replace; false=use zombie default loot (rotten flesh, etc.)
  "max_empowered_builder_count": 30,   // Builder cap
  "max_empowered_miner_count": 30,     // Miner cap
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

```json
{
  "day": 1.0,                    // Start day
  "zombie_property": { ... },    // Zombie attribute config
  "zombie_spawn_chooser": { ... }, // Zombie type selector
  "replace_chance": 0.0,         // Replacement chance (non-zombie → zombie)
  "remove_chance": 0.0,          // Removal chance (cancel spawn)
  "blood_moon_chance": 0.0,      // Blood moon trigger chance
  "zombie_count_modify": 1.0,    // Zombie spawn count multiplier
  "holy_cleansing": false        // Holy cleansing
}
```

### Field Details

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `day` | double | [1.0, ∞) | Stage start day |
| `zombie_property` | object | — | Zombie attribute config for this stage |
| `zombie_spawn_chooser` | object | — | Zombie type selector |
| `replace_chance` | double | [0.0, 1.0] | Non-zombie → zombie replacement chance (only affects mobs not locked by `mob_replacement`) |
| `remove_chance` | double | [0.0, 1.0] | Non-zombie removal chance (does NOT affect mobs locked as remove/replace in `mob_replacement`) |
| `blood_moon_chance` | double | [0.0, 1.0] | Blood moon trigger chance |
| `zombie_count_modify` | double | [0.0, ∞) | Zombie spawn count multiplier |
| `holy_cleansing` | boolean | true/false | `true` = all zombies are set on fire and **no new zombies spawn**. ⚠️ **Once enabled, the current stage will stop spawning zombies entirely. It will not auto-recover. Do not use in normal gameplay** |

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
| `can_throw_tnt` | false | true/false | Can throw TNT (`false` = TNT zombie self-destructs) |
| `do_swimming_zombie_convert` | false | true/false | Swimming zombies don't convert to drowned (protection mechanism) |
| `enhanced_sense` | false | true/false | Enable advanced sensing |
| `boundless_hunting` | false | true/false | Boundless hunting: zombies sense all player positions globally every 5 seconds (ignoring obstacles) |
| `blood_moon_boundless_hunting` | false | true/false | Enable boundless hunting during blood moon |
| `break_light_sources` | false | true/false | Break light source blocks (torches, lanterns, candles, jack o'lanterns, etc.) |
| `can_zombie_guard_continue_use_weapons` | false | true/false | Infected guards retain weapons |
| `enable_piglin_collision_anger` | false | true/false | Colliding with zombified piglins angers them |
| `piglin_angry_mode` | false | true/false | Zombified piglins attack everything indiscriminately |

### Probability Values

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `can_swim_probability` | 0.0 | [0.0, 1.0] | Individual swimming chance |
| `zombie_swim_speed_modify` | 1.0 | [0.0, ∞) | Zombie swim speed multiplier |
| `drowned_swim_speed_modify` | 1.0 | [0.0, ∞) | Drowned swim speed multiplier |
| `sun_immunity_probability` | 0.0 | [0.0, 1.0] | Sun immunity chance |
| `fire_immune_probability` | 0.0 | [0.0, 1.0] | Fire immunity chance |
| `block_stab_immune_probability` | 0.0 | [0.0, 1.0] | Block stab immunity chance (compatible with MineTrap) |
| `ladder_climb_probability` | 0.0 | [0.0, 1.0] | Individual ladder climb chance |
| `baby_probability` | 0.05 | [0.0, 1.0] | Baby zombie chance |
| `can_pick_up_loot_coefficient` | 0.55 | [0.0, ∞) | Item pickup coefficient |

### Advanced Sensing (requires `enhanced_sense: true`)

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `sense_bleeding_radius` | 64.0 | [0.0, ∞) | Player bleeding sense radius |
| `sense_bleeding_lifespan` | 400 (≈20s) | [1, ∞) | Sense duration (20 ticks = 1s) |
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
| `equipment_probability_factor` | 1.0 | [0.0, 128.0] | Equipment spawn probability factor |
| `equipment_enchantment_factor` | 1.0 | [0.0, 128.0] | Enchantment probability factor |

### Mod Integration

| JSON Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `musket_mod_gun_damage_modify` | 0.5 | [0.0, ∞) | Musket damage modifier |
| `piglin_collision_anger_chance` | 0.25 | [0.0, 1.0] | Collision anger chance |
| `max_empowered_zombie_miner_count` | 100 | [0, ∞) | Max miner count (**reserved, not active** — use top-level `max_empowered_miner_count`) |
| `max_empowered_zombie_builder_count` | 100 | [0, ∞) | Max builder count (**reserved, not active** — use top-level `max_empowered_builder_count`) |

---

## Zombie Spawn Selector

### Structure

```json
{
  "zombie_spawn_chooser": {
    "zombie_types": [
      {
        "spawn_type": "normal",        // normal / drowned / blood_moon
        "chance": 1.0,                 // weight
        "zombie_type": "zombiegamereborn:vanilla"
      }
    ]
  }
}
```

### Three Spawn Types

| `spawn_type` | Trigger | Notes |
|---|---|---|
| `normal` | Natural spawn | Zombies, husks, zombie villagers |
| `drowned` | Drowned conversion/water spawn | Recommended: use `vanilla` only, as no special drowned AI is implemented yet |
| `blood_moon` | Blood moon active | Overrides `normal` during blood moon to create zombie hordes |

### Weight System

- Each `spawn_type` has an **independent weighted pool**. Total weights don't need to sum to 1. Weighted random selection (`1.0+1.0` = 50/50, `1.0+3.0` = 25%/75%)
- During blood moon: **`normal` pool is completely ignored**, only `blood_moon` entries are used
- If `blood_moon` pool is empty → falls back to `dummy` (no AI, stands still)

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

> `mob_replacement` uses a `HashMap`. If the same `mob_id` appears multiple times, only the **last entry** takes effect. Be careful when editing collaboratively to avoid duplicates.

<details>
<summary>Click to expand full list</summary>

> ⚠️ `musket_mod_gunner` requires **Musket Mod**, `zombie_guard_villager` requires **Guard Villagers**. If the mod is missing, the type safely degrades to melee with no errors.

```json
{
  "mobs": [
    {"mob_id": "minecraft:dolphin", "action": "remove"},
    {"mob_id": "quark:shiba", "action": "remove"},
    {"mob_id": "minecraft:phantom", "action": "remove"},
    {"mob_id": "minecraft:wither_skeleton", "action": "replace"},
    {"mob_id": "minecraft:goat", "action": "remove"},
    {"mob_id": "minecraft:piglin_brute", "action": "replace"},
    {"mob_id": "minecraft:mule", "action": "remove"},
    {"mob_id": "minecraft:fox", "action": "remove"},
    {"mob_id": "touhou_little_maid:entity.monster.fairy", "action": "replace"},
    {"mob_id": "minecraft:frog", "action": "remove"},
    {"mob_id": "minecraft:sheep", "action": "remove"},
    {"mob_id": "minecraft:cat", "action": "remove"},
    {"mob_id": "minecraft:sniffer", "action": "remove"},
    {"mob_id": "mekanismadditions:baby_creeper", "action": "replace"},
    {"mob_id": "minecraft:bee", "action": "remove"},
    {"mob_id": "minecraft:cow", "action": "remove"},
    {"mob_id": "quark:foxhound", "action": "remove"},
    {"mob_id": "minecraft:spider", "action": "replace"},
    {"mob_id": "minecraft:witch", "action": "replace"},
    {"mob_id": "minecraft:stray", "action": "replace"},
    {"mob_id": "minecraft:creeper", "action": "replace"},
    {"mob_id": "minecraft:polar_bear", "action": "remove"},
    {"mob_id": "minecraft:turtle", "action": "remove"},
    {"mob_id": "minecraft:cave_spider", "action": "replace"},
    {"mob_id": "minecraft:wolf", "action": "remove"},
    {"mob_id": "minecraft:ocelot", "action": "remove"},
    {"mob_id": "mekanismadditions:baby_skeleton", "action": "replace"},
    {"mob_id": "minecraft:hoglin", "action": "replace"},
    {"mob_id": "mekanismadditions:baby_wither_skeleton", "action": "replace"},
    {"mob_id": "quark:forgotten", "action": "replace"},
    {"mob_id": "minecraft:pig", "action": "remove"},
    {"mob_id": "minecraft:rabbit", "action": "remove"},
    {"mob_id": "minecraft:magma_cube", "action": "replace"},
    {"mob_id": "minecraft:ghast", "action": "replace"},
    {"mob_id": "minecraft:camel", "action": "remove"},
    {"mob_id": "minecraft:llama", "action": "remove"},
    {"mob_id": "minecraft:blaze", "action": "replace"},
    {"mob_id": "minecraft:chicken", "action": "remove"},
    {"mob_id": "mekanismadditions:baby_stray", "action": "replace"},
    {"mob_id": "minecraft:slime", "action": "remove"},
    {"mob_id": "minecraft:panda", "action": "remove"},
    {"mob_id": "minecraft:axolotl", "action": "remove"},
    {"mob_id": "mekanismadditions:baby_enderman", "action": "replace"},
    {"mob_id": "quark:stoneling", "action": "remove"},
    {"mob_id": "minecraft:mooshroom", "action": "remove"},
    {"mob_id": "minecraft:donkey", "action": "remove"},
    {"mob_id": "minecraft:horse", "action": "remove"},
    {"mob_id": "minecraft:skeleton", "action": "replace"},
    {"mob_id": "quark:toretoise", "action": "remove"},
    {"mob_id": "minecraft:parrot", "action": "remove"},
    {"mob_id": "quark:wraith", "action": "replace"},
    {"mob_id": "minecraft:glow_squid", "action": "remove"},
    {"mob_id": "quark:crab", "action": "remove"},
    {"mob_id": "minecraft:enderman", "action": "replace"},
    {"mob_id": "minecraft:squid", "action": "remove"}
  ]
}
```
</details>

> 💡 **Tip**: If your modpack includes mobs from other mods, add corresponding replace/remove rules to this list.

---

## Zombie Types

ZGR provides **11 different zombie types**, each with unique behavior logic.

### Type Overview

| Registry ID | Language Key | Melee | Breaks | Places | Specialty |
|------------|--------------|:-----:|:------:|:------:|-----------|
| `zombiegamereborn:dummy` | Dummy | ❌ | ❌ | ❌ | No AI, stands still |
| `zombiegamereborn:vanilla` | Vanilla Zombie | ✅ | ❌ | ❌ | Pure vanilla zombie behavior |
| `zombiegamereborn:enhanced_vanilla` | Enhanced Vanilla | ✅ | ❌ | ❌ | Enhanced melee AI |
| `zombiegamereborn:builder` | Builder | ✅ | ✅ | ✅ | Break walls, place paths, build bridges |
| `zombiegamereborn:miner` | Miner | ✅ | ✅ | ❌ | Smart block breaking |
| `zombiegamereborn:bow_attacker` | Bow Attacker | ❌ | ❌ | ❌ | Bowman |
| `zombiegamereborn:crossbow_attacker` | Crossbow Attacker | ❌ | ❌ | ❌ | Crossbowman |
| `zombiegamereborn:shield_user` | Shield User | ✅ | ❌ | ❌ | Shield block + bash |
| `zombiegamereborn:tnt_attacker` | TNT Attacker | ✅ | ❌ | ❌ | TNT throw/self-destruct |
| `zombiegamereborn:musket_mod_gunner` | Musket Gunner | ❌ | ❌ | ❌ | Musket gunner (requires Musket Mod) |
| `zombiegamereborn:zombie_guard_villager` | Zombified Guard Villager | ❌ | ❌ | ❌ | Infected guard (all weapons) |

### Detailed Descriptions

> ⚠️ **Mod-dependent types**: `musket_mod_gunner` requires Musket Mod, `zombie_guard_villager` requires Guard Villagers. If the mod is missing, the type safely degrades to melee with no crashes.

#### 1. `dummy` — No AI Zombie

Clears all targets and behavior goals. Completely immobile — no movement, attack, or targeting.

---

#### 2. `vanilla` — Vanilla Zombie

Directly inherits the base class. Only adds equipment and enchantment enhancements over vanilla.

---

#### 3. `enhanced_vanilla` — Enhanced Vanilla Zombie

Replaces vanilla's `ZombieAttackGoal` with `EnhancedZombieAttackGoal` (a more CPU-efficient intelligent melee AI).

---

#### 4. `builder` — Builder Zombie ⭐

| Ability | Implementation |
|---------|---------------|
| Break blocks | `ZombieBreakBlockGoal` — breaks blocks in the way |
| Place blocks | `ZombiePlaceBlockGoal` — places blocks from offhand |
| Clear head space | `ClearHeadBlockGoal` — clears suffocating blocks above |
| Water bridging | `ZombieWaterBridgeBuildGoal` — builds paths over water |
| Attack | `ZombieMeleeAndPathBuildGoal` — melee combat while path-building |

**Equipment features**:
- 40% chance to hold a pickaxe, 60% to hold a regular weapon
- Offhand always holds a path block (auto-selected based on biome)

**Activation mechanism**:
- Initial state `isEmpowered = false`, won't execute core builder AI
- Competes for activation slots via the quota system (controlled by `max_empowered_builder_count`)

---

#### 5. `miner` — Miner Zombie ⭐

| Ability | Implementation |
|---------|---------------|
| Break blocks | `ZombieBreakBlockGoal` — breaks blocks |
| Clear head space | `ClearHeadBlockGoal` — clears suffocating blocks above |
| Attack | `ZombieSmartBreakAttackGoal` — **smart digging**, prioritizes breaking blocks toward the player |
| Place blocks | ❌ |

Same quota system as Builder, controlled by `max_empowered_miner_count`.

---

#### 6. `bow_attacker` — Bow Attacker Zombie

- Main hand holds a bow
- Uses `ZombieBowAttackGoal` for ranged attacks
- Has a melee backup goal

---

#### 7. `crossbow_attacker` — Crossbow Attacker Zombie

- Main hand holds a crossbow
- Uses `ZombieCrossbowAttackGoal` for ranged attacks
- Zombified piglins holding crossbows automatically override to this type

---

#### 8. `shield_user` — Shield User Zombie

- Offhand holds a shield
- `ZombieShieldGoal` (raise shield to block) + `ZombieShieldAttackGoal` (shield bash)
- Takes no damage when blocking frontal attacks

---

#### 9. `tnt_attacker` — TNT Attacker Zombie ⭐

| Mode | Condition | Behavior |
|------|-----------|----------|
| **Throw mode** | `canThrowTNT = true` | Lights TNT, holds it overhead, and throws it toward the enemy with a parabolic arc |
| **Self-destruct mode** | `canThrowTNT = false` | Like a creeper, approaches the target then explodes |

**Implementation**: `ZombieTNTAttackGoal` — Priority 2 (same as main attack goal)

**Behavior mechanics**:
- Must wear TNT as helmet and world must allow mob griefing (`mobGriefing`)
- Cooldown: 80 ticks (4 seconds)
- Throw mode: aggro range 16 blocks, parabolic throw (similar to skeleton arrow trajectory)
- Self-destruct mode: triggers within 5 blocks of target
- After throwing, cooldown resets to 80 ticks — won't re-ignite during cooldown

---

#### 10. `musket_mod_gunner` — Musket Gunner Zombie

- Requires **Musket Mod**
- Uses muskets for ranged attacks
- Damage controlled by `musket_mod_gun_damage_modify`
- Degrades to melee if the mod is missing

---

#### 11. `zombie_guard_villager` — Zombified Guard Villager ⭐

**Requires Guard Villagers mod.**

| Mode | Condition | Behavior |
|------|-----------|----------|
| **All-weapon mode** | `canZombieGuardContinueUseWeapons = true` | Has all four weapon abilities: bow, crossbow, musket, and shield |
| **Melee mode** | `canZombieGuardContinueUseWeapons = false` | Enhanced melee only |

This is the **only type with all four weapon abilities**.

---

### Goal Priority Overview

| Priority | Common Goals |
|:--------:|-------------|
| 1 | `ZombieShieldGoal`, `ZombieBreakBlockGoal`, `ZombiePlaceBlockGoal`, `ZombieRemoveLightSourceGoal`, `ZombieFloatGoal` |
| 2 | Main attack Goals (melee/bow/crossbow/musket/TNT throw/smart break) |
| 2 | `ZombieRestrictSunGoal`, `ZombieWaterBridgeBuildGoal`, `ZombieTNTAttackGoal` |
| 3 | `JumpAttackGoal`, `ClearHeadBlockGoal` |
| 4 | Backup melee Goal, Advanced Sensing Goals |

---

## Equipment Guide

### Three Core Parameters

| Parameter | Effect | Default | Recommended |
|-----------|--------|---------|-------------|
| `equipment_quality_mean_offset` | Equipment material quality | 0 | -2 ~ +10 |
| `equipment_probability_factor` | Equipment spawn chance | 1.0 | 0 ~ 10 |
| `equipment_enchantment_factor` | Enchantment chance | 1.0 | 0 ~ 3 |

---

### Table 1: Material Distribution (`equipment_quality_mean_offset`)

Normal distribution: `N(μ = 2.58 + offset, σ = 1.14)`

**Material thresholds:**

| Material | Code Threshold | Range |
|----------|---------------|-------|
| Leather | `tier < 2.2` |
| Gold | `2.2 ≤ tier < 3.8` |
| Chainmail | `3.8 ≤ tier < 5.5` |
| Iron | `5.5 ≤ tier < 7.5` |
| Diamond | 9.0 | `7.5 ≤ tier < 9.0` |
| Netherite | 10.0 | `tier ≥ 9.0` |

> Boundary logic: when `tier` equals a threshold, it counts toward the **higher tier** (e.g., `tier = 3.8` → Chainmail, `tier = 5.5` → Iron)

**Distribution per offset** (`N(μ = 2.58 + offset, σ = 1.14)`):

| offset | Leather | Gold | Chainmail | Iron | Diamond | Netherite | Notes |
|:------:|:-------:|:----:|:---------:|:----:|:-------:|:---------:|-------|
| **0** | 37.0% | 48.8% | 13.7% | 0.5% | <0.1% | <0.001% | 🟢 Vanilla-style |
| 1 | 11.3% | 46.3% | 37.7% | 4.6% | <0.1% | <0.001% | Mainly chainmail |
| 2 | 1.8% | 23.0% | 54.3% | 20.4% | 0.5% | <0.01% | Chainmail majority |
| 3 | 0.2% | 5.8% | 41.3% | 48.1% | 4.5% | 0.1% | Mainly iron |
| 4 | <0.1% | 0.7% | 16.4% | 62.0% | 19.2% | 1.7% | ~1/5 diamond |
| 5 | — | 0.05% | 3.4% | 43.8% | **42.2%** | **10.6%** | 🔥 Diamond+Netherite majority |
| 6 | — | — | 0.4% | 16.8% | **47.3%** | **35.6%** | Nearly half diamond |
| 7 | — | — | — | 3.4% | **27.1%** | **69.5%** | Nearly 70% netherite |
| 8 | — | — | — | 0.4% | **7.9%** | **91.8%** | ⚠️ 90%+ netherite |
| 10 | — | — | — | — | 0.1% | **99.9%** | Almost all netherite |

---

### Table 2: Equipment Probability (`equipment_probability_factor`)

**Base formula:**
```
Weapon chance = (0.01 + 0.04 × d) × factor
Armor chance = 0.16 × (d - 0.10) × factor
```

> The `d` value is computed by the vanilla regional difficulty system and **cannot be manually modified**.

The `d` value changes over game progression:

| Scenario | `d` value |
|----------|-----------|
| Easy · New area | 0.15 |
| Normal · New area | 0.30 |
| Hard · New area | 0.45 |
| Easy · Max difficulty area | 0.79 |
| Normal/Hard · Max difficulty area | 1.00 |

**Weapon probability:**

| factor | d=0.15 | d=0.30 | d=0.45 | d=0.79 | d=1.00 |
|:------:|:------:|:------:|:------:|:------:|:------:|
| 0.0 | 0% | 0% | 0% | 0% | 0% |
| 0.5 | 0.8% | 1.1% | 1.4% | 2.1% | 2.5% |
| **1.0** | **1.6%** | **2.2%** | **2.8%** | **4.2%** | **5.0%** |
| 2.0 | 3.2% | 4.4% | 5.6% | 8.3% | 10.0% |
| 3.0 | 4.8% | 6.6% | 8.4% | 12.5% | 15.0% |
| 5.0 | 8.0% | 11.0% | 14.0% | 20.8% | 25.0% |
| 10.0 | 16.0% | 22.0% | 28.0% | 41.5% | 50.0% |

**Armor probability:**

| factor | d=0.15 | d=0.30 | d=0.45 | d=0.79 | d=1.00 |
|:------:|:------:|:------:|:------:|:------:|:------:|
| 0.0 | 0% | 0% | 0% | 0% | 0% |
| 0.5 | 0.4% | 1.6% | 2.8% | 5.5% | 7.2% |
| **1.0** | **0.8%** | **3.2%** | **5.6%** | **11.0%** | **14.4%** |
| 2.0 | 1.6% | 6.4% | 11.2% | 22.0% | 28.8% |
| 3.0 | 2.4% | 9.6% | 16.8% | 33.0% | 43.2% |
| 5.0 | 4.0% | 16.0% | 28.0% | 55.0% | 72.0% |
| 10.0 | 8.0% | 32.0% | 56.0% | ≈100% | ≈100% |

---

### Table 3: Enchantment Probability (`equipment_enchantment_factor`)

**Base formula:**
```
Weapon enchant chance = 0.25 × difficulty × factor
Armor enchant chance = 0.50 × difficulty × factor (per piece, independent)
Enchantment level = 5 + difficulty × random.nextInt(18), rounded (range scales with difficulty)
```

> `difficulty` is calculated by `calculateDifficulty()`: Easy=0.28, Normal=0.51, Hard=0.70.

| factor | Weapon enchant (Hard) | Armor enchant (Hard) | Effect |
|:------:|:---------------------:|:--------------------:|--------|
| 0 | 0% | 0% | Enchantments disabled |
| 0.5 | 8.7% | 17.5% | Half enchantment chance |
| **1.0** | **17.5%** | **35%** | **Vanilla-style** 🟢 |
| 2.0 | 35% | 70% | ~70% armor enchanted |
| 3.0 | 52.5% | ≈100% | Over half weapons enchanted |

---

### Quick Reference

| Desired Effect | offset | factor | enchant |
|----------------|:------:|:------:|:-------:|
| 🟢 Fully vanilla | 0 | 1.0 | 1.0 |
| Rarer gear, better quality | +2 ~ +3 | 0.5 ~ 0.8 | 1.0 |
| More zombies with gear, same quality | 0 | 2.0 ~ 3.0 | 1.0 |
| Elite style (fewer but stronger) | +3 ~ +5 | 0.3 ~ 0.5 | 1.0 ~ 2.0 |
| Everyone armed (quantity + quality) | +3 ~ +4 | 3.0 ~ 5.0 | 1.0 ~ 2.0 |
| 🔥 Netherite legion | +8 ~ +10 | 5.0 ~ 10.0 | 2.0 ~ 3.0 |
| Extreme apocalypse (max enchant) | +5 ~ +7 | 3.0 ~ 5.0 | 3.0 |

---

## FAQ

### Q1: Does the stage array need to be sorted by day?

**A**: No. The code auto-sorts by day internally, regardless of JSON order.

---

### Q2: What if `zombie_count_modify` is set to 0 or negative?

**A**: At 0, zombies won't spawn naturally. Same for negative values (clamped to 0). Recommended range: `0.0 ~ 10.0`.

---

### Q3: Are the chances under the same spawn_type weights or probabilities?

**A**: They are weight ratios. Weighted random selection: `1.0+1.0` → 50% each; `1.0+3.0` → 25%/75%. Different spawn_type pools don't interfere with each other.

---

### Q4: What happens when blood_moon pool is empty?

**A**: Falls back to `dummy` zombies (no AI). Won't crash.

---

### Q5: Do `remove_chance` and `replace_chance` affect the same mob?

**A**: No. Each mob in `mob_replacement` is pre-assigned either `remove` or `replace`. The two chances apply to their respective mob groups independently.

---

### Q6: What if the same mob_id appears multiple times in `mob_replacement`?

**A**: The last entry wins (HashMap behavior).

---

### Q7: Will a wrong mob_id for a mod mob cause a crash?

**A**: No. Invalid IDs are silently skipped at runtime without matching any real mob.

---

### Q8: Is `holy_cleansing` permanent?

**A**: No — it's a stage-level real-time switch. The game reads the current stage config every 20 ticks:
- Current stage `holy_cleansing: true` → all zombies ignite, **no new zombies spawn**
- Switch to a stage with `holy_cleansing: false` → zombies stop burning, normal spawning resumes

> ⚠️ **Warning**: Once a tick with `holy_cleansing: true` executes, the current stage completely stops zombie spawning. This is not an archive-level toggle — it recovers on stage switch, but players will face no enemies during that period. Do not use this field in normal config unless you have a specific design intent.

---

### Q9: Will a JSON syntax error (missing comma, missing quotes) cause a crash?

**A**: No — your server won't crash, but your custom config will be lost. The mod logs an error and runs with full default config.

---

### Q10: What if I delete top-level fields (e.g., `max_empowered_builder_count`)?

**A**: Safe to delete. Missing fields automatically use their Java code defaults.

---

### Q11: What's the difference between `max_empowered_zombie_*_count` in stages vs. the top-level fields?

**A**: The two fields inside stages are **reserved — not connected to the quota system in the current version**. Actual quotas are controlled only by the two top-level `GameProperty` fields.

---

### Q12: Can the drowned pool use non-vanilla types?

**A**: You can, but it's not recommended. Builder's `ZombieWaterBridgeBuildGoal` will constantly try to place blocks under its feet, causing block chaos underwater. Use only `vanilla` for the drowned pool.

---

> 📝 **Document version**: 1.4  
> 📅 **Last updated**: 2026-07-19
