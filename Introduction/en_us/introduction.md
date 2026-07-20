**Authors:**

*   **Aljun2007**: Design & Code
*   **Deepseek**: Assistant & Advisor

***

## 👴 Old Version

*   [`ZombieGame`](https://www.curseforge.com/minecraft/mc-mods/zombiegame)

***

## 🧟 What is this mod?

A mod that makes Minecraft zombies smarter and more dangerous! If you've played "Zombie Apocalypse 100 Days" scenarios, you know the thrill of being hunted by intelligent zombies. In this mod, zombies are no longer mindless vanilla husks — they think, they break walls, they build bridges, they use weapons. They will make you fear the undead all over again! 🫣

***

## 🤔 What's new in Reborn?

*   ⚡ **Performance optimized**: Pathfinding calls massively optimized for smoother gameplay
*   👀 **Zombie Awareness fused**: Sensory system overhauled — zombies can smell blood, hear block breaking and gunshots. No more hiding behind a wall and being safe!
*   🧟 **11 specialized zombie types**: Each with unique AI and weapons
*   🎮 **Fully customizable difficulty**: Control zombie stats, spawn rates, blood moon chance, day-by-day progression through in-game GUI or JSON editing — define your own 100-day apocalypse
*   🔧 **Built-in preset manager**: One-click switch between "Global Default", "Initial Default", "Disabled" presets, plus export/import your own configs
*   🤝 **Multi-mod integration**: TACZ, Point Blank, Musket Mod sound sensing + gunner zombies, Enhanced Celestials blood moon, Guard Villagers infection
*   🌐 **Multiplayer friendly**: Based on the difficulty level system determined by players' individual survival days, both new and experienced players can have their own pace

***

## 🎮 How smart are zombies?

### 🧱 They break your base!

Zombies detect you behind walls and dig right through!

*   **Builder** — breaks walls, paves paths, builds bridges, clears overhead blocks, even builds over water
*   **Miner** — smart digging, prioritizes blocks between them and you

### 🛡️ They use shields!

Shield zombies block frontal attacks — **use an axe to break their guard!** 💥

### 🧨 They use TNT!

TNT Attacker zombies are pure chaos.

*   **Throw mode** — pulls TNT from thin air, lights it, and tosses it like a grenade from up to 16 blocks away
*   **Self-destruct mode** — when enemies get too close, they ignite and blow themselves up
*   Must have `mobGriefing` enabled to use TNT abilities

### 🏹 They use ranged weapons!

*   **Bow Attacker** — shoots arrows from range
*   **Crossbow Attacker** — uses crossbows (Piglins holding crossbows automatically become this type)
*   **Musket Gunner** — requires Musket Mod, devastating ranged firepower 🔫

### 👃 Super sensing!

With `enhanced_sense` enabled, zombies can:

*   Smell your blood from 64 blocks away 🩸
*   Feel block vibrations from 16 blocks
*   Hear gunshots from 64 blocks (silencers reduce to 16 blocks) 🔇

### 🏗️ They build bridges!

Gap in the way? Builder zombies will bridge it themselves — even over water!

### 🏊 They swim!

With swimming probability enabled, zombies can chase you through water. Swimming zombies won't convert to drowned (protection mechanic).

### 💪 All stats are configurable

Through the config file, you can control: health, armor, speed, damage, knockback resistance, equipment quality and enchantments, sun/fire immunity chance, and more.

***

## 🧟 Zombie Types (11 total)

| Type                  |Description                                                             |
| --------------------- |----------------------------------------------------------------------- |
| <code>Dummy</code>    |🤖 No AI, stands still (placeholder/fallback)                           |
| <code>Vanilla</code>  |🧟 Vanilla behavior with enhanced equipment &amp; enchants              |
| <code>Enhanced Vanilla</code> |💪 Upgraded melee AI                                                    |
| <code>Builder</code>  |🔨 Breaks blocks + places paths + bridges + places blocks               |
| <code>Miner</code>    |⛏️ Smart digging, prioritizes tunneling toward you                      |
| <code>Bow Attacker</code> |🏹 Ranged bow attacks                                                   |
| <code>Crossbow Attacker</code> |🎯 Ranged crossbow attacks                                              |
| <code>Shield User</code> |🛡️ Blocks frontal attacks + shield bash (use an axe!)                  |
| <code>TNT Attacker</code> |🧨 Throws TNT like grenades — or self-destructs when close! ⭐        |
| <code>Musket Gunner</code> |🔫 Ranged musket attacks (requires Musket Mod)                          |
| <code>Zombie Guard Villager</code> |⚔️ Infected guard retains bow/crossbow/musket/shield — all four weapons |

***

## ⚙️ Configuration

**In-game GUI editing:**

1.  OP enters `/zombiegamereborn config gameProperty`
2.  Adjust all parameters visually in real-time
3.  Built-in preset manager lets you save/load/import/export presets

**JSON file editing (advanced):**

*   See [`ConfigFileGuide.md`](https://github.com/Aljun2007/ZombieGameReborn/blob/zgr-forge-1.20.1/Documentury/en_us/ConfigFileGuide.md)
*   Supports 10+ stages for a full difficulty curve
*   Each stage independently controls zombie stats, spawn types, blood moon chance, etc.

***

## 📦 Dependencies & Integration

**Required:**

*   Minecraft Forge

**Optional integration mods:**

| Mod                    |Effect                                              |
| ---------------------- |--------------------------------------------------- |
| 🎵 <strong>TACZ</strong> |Zombies hear gunshots                               |
| 🎵 <strong>Vic's Point Blank</strong> |Zombies hear gunshots                               |
| 🔫 <strong>Musket Mod</strong> |Zombies hear gunshots + Gunner zombies use muskets  |
| 🌕 <strong>Enhanced Celestials</strong> |Blood moon support (configurable trigger chance)    |
| 🛡️ <strong>Guard Villagers</strong> |Infected guards retain weapon skills (default: off) |
| ⛏️ <strong>MineTraps</strong> |Cactus-immune zombies also ignore spike traps (Nail Trap, Spikes) — barbed wire only slows them |

***

## ⚠️ Notes

*   This mod uses a **zombie type system** (`zombie_type`) different from vanilla: only the correct zombie type can use its corresponding weapon (e.g., only bow attackers shoot)
*   Mixin injection is applied to zombie/zombie villager models — report any mod rendering conflicts (previously conflicted with Wither Storm's zombie model mixin)

***

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

***

## 💡 Tips

1.  🏗️ Build your doomsday fortress early
2.  🧱 Don't think walls will save you — zombies will tear them down!
3.  🪓 See a shield zombie? Use an axe to break its guard
4.  🏃 See a group of zombies building a bridge toward you? RUN!
5.  🔇 Install silencers on your guns
6.  📅 The default config ramps difficulty every 10 days — experience a full 100-day apocalypse

***

## 🎇 Finally

Questions or suggestions? Feel free to open an issue on [Github](https://github.com/Aljun2007/ZombieGameReborn)!