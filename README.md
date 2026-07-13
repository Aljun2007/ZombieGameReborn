# 💀 ZombieGame:Reborn — README (English)

**Authors:**
- **Aljun2007**: Design & Code
- **Deepseek**: Assistant & Advisor

---

## 🧟 What is this mod?

A mod that makes Minecraft zombies smarter and more dangerous!
If you've played "Zombie Apocalypse 100 Days" scenarios, you know the thrill of being hunted by intelligent zombies.
In this mod, zombies are no longer mindless vanilla husks — they think, they break walls, they build bridges, they use weapons.
They will make you fear the undead all over again! 🫣

---

## 🤔 What's new in Reborn?

- ⚡ **Performance optimized**: Pathfinding calls massively optimized for smoother gameplay
- 👀 **Zombie Awareness fused**: Sensory system overhauled — zombies can smell blood, hear block breaking and gunshots. No more hiding behind a wall and being safe!
- 🧟 **10 specialized zombie types**: Each with unique AI and weapons
- 🎮 **Fully customizable difficulty**: Control zombie stats, spawn rates, blood moon chance, day-by-day progression through in-game GUI or JSON editing — define your own 100-day apocalypse
- 🔧 **Built-in preset manager**: One-click switch between "Global Default", "Initial Default", "Disabled" presets, plus export/import your own configs
- 🤝 **Multi-mod integration**: TACZ, Point Blank, Musket Mod sound sensing + gunner zombies, Enhanced Celestials blood moon, Guard Villagers infection
- 🌐 **Multiplayer friendly**: Stage-based difficulty system scales with in-game days, giving both new and veteran players a balanced challenge

---

## 🎮 How smart are zombies?

### 🧱 They break your base!

Zombies detect you behind walls and dig right through!
- **Builder** — breaks walls, paves paths, builds bridges, clears overhead blocks, even builds over water
- **Miner** — smart digging, prioritizes blocks between them and you

### 🛡️ They use shields!

Shield zombies block frontal attacks — **use an axe to break their guard!** 💥

### 🏹 They use ranged weapons!

- **Bow Attacker** — shoots arrows from range
- **Crossbow Attacker** — uses crossbows (Piglins holding crossbows automatically become this type)
- **Musket Gunner** — requires Musket Mod, devastating ranged firepower 🔫

### 👃 Super sensing!

With `enhanced_sense` enabled, zombies can:
- Smell your blood from 64 blocks away 🩸
- Feel block vibrations from 16 blocks
- Hear gunshots from 64 blocks (silencers reduce to 16 blocks) 🔇

### 🏗️ They build bridges!

Gap in the way? Builder zombies will bridge it themselves — even over water!

### 🏊 They swim!

With swimming probability enabled, zombies can chase you through water. Swimming zombies won't convert to drowned (protection mechanic).

### 💪 All stats are configurable

Through the config file, you can control: health, armor, speed, damage, knockback resistance, equipment quality and enchantments, sun/fire immunity chance, and more.

---

## 🧟 Zombie Types (10 total)

| Type | Description |
|------|-------------|
| `Dummy` | 🤖 No AI, stands still (placeholder/fallback) |
| `Vanilla` | 🧟 Vanilla behavior with enhanced equipment & enchants |
| `Enhanced Vanilla` | 💪 Upgraded melee AI |
| `Builder` | 🔨 Breaks blocks + places paths + bridges + places blocks |
| `Miner` | ⛏️ Smart digging, prioritizes tunneling toward you |
| `Bow Attacker` | 🏹 Ranged bow attacks |
| `Crossbow Attacker` | 🎯 Ranged crossbow attacks |
| `Shield User` | 🛡️ Blocks frontal attacks + shield bash (use an axe!) |
| `Musket Gunner` | 🔫 Ranged musket attacks (requires Musket Mod) |
| `Zombie Guard Villager` | ⚔️ Infected guard retains bow/crossbow/musket/shield — all four weapons |

---

## ⚙️ Configuration

**In-game GUI editing:**
1. OP enters `/zombiegamereborn config gameProperty`
2. Adjust all parameters visually in real-time
3. Built-in preset manager lets you save/load/import/export presets

**JSON file editing (advanced):**
- See [`ConfigFileGuide.md`](Documentury/en_us/ConfigFileGuide.md)
- Supports 10+ stages × 10-day intervals for a full difficulty curve
- Each stage independently controls zombie stats, spawn types, blood moon chance, etc.

---

## 📦 Dependencies & Integration

**Required:**
- Minecraft Forge (1.20.1)

**Optional integration mods:**
| Mod | Effect |
|-----|--------|
| 🎵 **TACZ 1.1.7** | Zombies hear gunshots |
| 🎵 **Vic's Point Blank 1.11.1** | Zombies hear gunshots |
| 🔫 **Musket Mod 1.5.4** | Zombies hear gunshots + Gunner zombies use muskets |
| 🌕 **Enhanced Celestials 5.0.3.2** | Blood moon support (configurable trigger chance) |
| 🛡️ **Guard Villagers 1.6.18** | Infected guards retain weapon skills (default: off) |

---

## ⚠️ Notes

- This mod uses a **zombie type system** (`zombie_type`) different from vanilla: only the correct zombie type can use its corresponding weapon (e.g., only bow attackers shoot bows)
- Mixin injection is applied to zombie/zombie villager models — report any mod conflicts (previously conflicted with Wither Storm's zombie model mixin)
- Spartan Weaponry bows and shields are supported; crossbows are not (same as vanilla MC)

---

## ❓ FAQ

**Q: Zombies are too hard, I can't survive!** 😰

> A: Lower the values in the config file, or install a powerful gun mod!

**Q: Why do some zombies have bows/crossbows/shields/guns?**

> A: These are specialized zombie types, each with their own weapons and AI. Adjust their spawn weights in the config.

**Q: Can this work with other zombie mods?**

> A: Possibly, but test carefully. Report any conflicts.

**Q: Can this be used on servers?**

> A: Yes! Install on both server and client.

**Q: Can I put this in my modpack?**

> A: Feel free (non-commercial use) 👍

---

## 💡 Tips

1. 🏗️ Build your doomsday fortress early
2. 🧱 Don't think walls will save you — zombies will tear them down!
3. 🪓 See a shield zombie? Use an axe to break its guard
4. 🏃 See a group of zombies building a bridge toward you? RUN!
5. 🔇 Install silencers on your guns
6. 📅 The default config ramps difficulty every 10 days — experience a full 100-day apocalypse

---

## 🎇 Finally

Questions or suggestions? Feel free to open an issue on [Github](https://github.com/Aljun2007/ZombieGameReborn)!
