# Example-Default.json Stage Plan Explanation (English)

> 10 stages × 10 days = 100-day complete apocalypse difficulty curve

---

## Overview

| Stage | Days | Core Feature |
|:-----:|:----:|--------------|
| ① | Day 1~10 | Outbreak begins, only enhanced melee zombies |
| ② | Day 11~20 | Worsening, mass non-zombie extinction, blood moon erupts |
| ③ | Day 21~30 | Piglin collision aggro, non-zombies濒危, sustained blood moon |
| ④ | Day 31~40 | Jump attack unlocked, non-zombies nearly gone |
| ⑤ | Day 41~50 | Sense system activated, zombies flee sun |
| ⑥ | Day 51~60 | Swimming zombies appear, Builder+Miner enter blood moon |
| ⑦ | Day 61~70 | Wall-hacking aggro, piglin rage mode, sustained Builder+Miner |
| ⑧ | Day 71~80 | **⭐ Turning point**: Ranged/shield units appear, blood moon Builder+Miner explode |
| ⑨ | Day 81~90 | All-out war, sense range expanded, equipment rate increased |
| ⑩ | Day 91~100 | **Peak apocalypse**: All stats maxed, 30% blood moon, 2x spawns |
| ⑪ | Day 101+ | **Post-apocalypse plateau**: Difficulty holds, blood moon drops to 10% |

---

## Stage ①: Day 1~10 — Outbreak

- **Zombie type**: Only `enhanced_vanilla`
- **Features**: No senses, no sun flee, no jump
- **Speed**: 1.0x (vanilla)
- **Armor**: 2.0 (zero protection)
- **Quantity**: 1.0x (vanilla)
- **remove_chance**: 0% (all non-zombies spawn normally)
- **Blood moon**: None
- **Design**: Give players time to adapt.

---

## Stage ②: Day 11~20 — Worsening

- **Attack**: 1.05x
- **Speed**: 1.02x
- **Armor**: 2.3, Toughness 0.2
- **Pickup coeff**: 0.58
- **Quantity**: 1.08x
- **remove_chance**: 50%
- **Blood moon**: 15% chance
- **Equipment**: factor 1.3, offset 0

---

## Stage ③: Day 21~30 — Escalation

- **Attack**: 1.1x
- **Speed**: 1.04x
- **Armor**: 2.6, Toughness 0.4
- **`enable_piglin_collision_anger: true`**
- **remove_chance**: 80%
- **Blood moon**: 15%
- **Quantity**: 1.16x

---

## Stage ④: Day 31~40 — Mutation

- **Attack**: 1.15x
- **Speed**: 1.06x
- **Armor**: 2.9, Toughness 0.6
- **`can_jump_attack: true`**
- **remove_chance**: 90%
- **Quantity**: 1.24x
- **Blood moon**: 15%
- **Equipment**: factor 1.9, offset 0.05

---

## Stage ⑤: Day 41~50 — Sense Awakening

- **Attack**: 1.2x
- **Speed**: 1.08x
- **Armor**: 3.2, Toughness 0.8
- **`flee_sun: true`**
- **`enhanced_sense: true`**
- **Sense radius**: 80 blocks, 480 ticks (24s)
- **remove_chance**: 90%
- **Quantity**: 1.32x
- **Blood moon**: 15%

---

## Stage ⑥: Day 51~60 — Underwater Threat

- **Attack**: 1.25x
- **Speed**: 1.1x
- **Armor**: 3.5, Toughness 1.0
- **`do_swimming_zombie_convert: true`**
- **Blood moon pool**: Builder(3)+Miner(5) introduced
- **remove_chance**: 95%
- **Quantity**: 1.4x
- **Blood moon**: 15%
- **Equipment**: factor 2.5, offset 0.15

---

## Stage ⑦: Day 61~70 — Endgame Acceleration

- **Attack**: 1.3x
- **Speed**: 1.12x
- **Armor**: 3.8, Toughness 1.2
- **`follow_must_see: false`** — zombies track through walls
- **`piglin_angry_mode: true`**
- **Blood moon pool**: Builder(3)+Miner(5) sustained
- **remove_chance**: 98%
- **Quantity**: 1.48x
- **Blood moon**: 20%

---

## Stage ⑧: Day 71~80 — ⭐ Super Upgrade

**The most important turning point in the 100-day curve.**

### New units appear (normal + blood moon pools)

| Type | Weight | % |
|------|:------:|:-:|
| `enhanced_vanilla` | 80 | 93.0% |
| `shield_user` | 3 | 3.5% |
| `musket_mod_gunner` | 1 | 1.2% |
| `bow_attacker` | 1 | 1.2% |
| `crossbow_attacker` | 1 | 1.2% |

> Shield:Musket:Bow:Crossbow = 3:1:1:1, elite units total ~7%.

### Blood moon pool — Destructive zombie explosion

| Type | Weight |
|------|:------:|
| `miner` | 50 |
| `builder` | 30 |
| Elite units | 3:1:1:1 |

~93% of blood moon spawns are Builder+Miner — extreme base destruction.

### Other changes

- **`can_zombie_guard_continue_use_weapons: true`**
- **Attack**: 1.35x, **Speed**: 1.14x
- **Armor**: 4.1, Toughness 1.4
- **remove_chance**: 100%
- **Quantity**: 1.56x
- **Blood moon**: 25% (every ~4 nights)

---

## Stage ⑨: Day 81~90 — Total War

- **Attack**: 1.4x
- **Speed**: 1.17x
- **Armor**: 4.4, Toughness 1.6
- **Sense radius**: 96 blocks, 600 ticks (30s)
- **remove_chance**: 100%
- **Quantity**: 1.64x
- **Blood moon**: 25%
- **Equipment**: factor 3.4, offset 0.3 (~30% coverage, chain/iron)

---

## Stage ⑩: Day 91~100 — Peak Apocalypse

| Attribute | Value | Notes |
|-----------|:-----:|-------|
| Attack modifier | 1.5x | Linear growth to 1.5x |
| Mining modifier | 1.5x | Peak destruction speed |
| Speed modifier | 1.2x | Below 1.4 safety limit |
| Armor | 5.0 | Equivalent to gold armor |
| Toughness | 2.0 | Gold-grade toughness |
| Knockback resist | 0.45 | Near immunity |
| Swim chance | 60% | Most zombies can swim |
| Sun immunity | 25% | 1/4 ignore sunlight |
| Fire immunity | 12% | Some ignore fire |
| Pickup coeff | 0.85 | Most pick up items |
| Ambient volume | 0.3x | Nearly silent (stealth difficulty ↑) |
| **remove_chance** | **100%** | Only zombies remain |
| **Quantity** | **2.0x** | Double spawns |
| **Blood moon** | **30%** | Every 3~4 nights |
| **Equipment coverage** | **~40%** | factor 3.7 |
| **Equipment quality** | **offset 0.35** | Leather+chain 70%, low quality |

> Shield weight increases from 3→5 at Day 81, then 6 at Day 91.

---

## Stage ⑪: Day 101+ — Post-Apocalypse Plateau

Same stats as Stage ⑩, except:

- **Blood moon drops to 10%**: Peak has passed
- **Quantity drops to 1.8x**: Slight density decrease
- **Design**: "New normal" — players can handle it, difficulty stops climbing

---

## Linear Progression Quick Reference

| Attribute | Day 1 | Day 50 | Day 91 | Day 100(extrapolated) |
|-----------|:-----:|:------:|:------:|:---------------------:|
| Attack modifier | 1.0 | 1.25 | 1.5 | 1.5 |
| Mining modifier | 1.0 | 1.25 | 1.5 | 1.5 |
| Speed modifier | 1.0 | 1.1 | 1.2 | 1.22 |
| Armor | 2.0 | 3.5 | 5.0 | 5.2 |
| Toughness | 0 | 1.0 | 2.0 | 2.2 |
| Knockback resist | 0 | 0.25 | 0.45 | 0.45 |
| remove_chance | 0% | 95% | 100% | 100% |
| Quantity modifier | 1.0 | 1.4 | 2.0 | 2.0 |
| Equipment factor | 1.0 | 2.5 | 3.7 | 3.7 |
| Blood moon chance | 0% | 15% | 30% | 10% |
