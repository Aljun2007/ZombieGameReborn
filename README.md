# ZombieGame:Reborn

**Version**: 1.4 | **Minecraft**: 1.20.1 | **Forge**: 47.4.20

**Authors**:
- **Aljun2007**: Design & Development
- **DeepSeek**: Assistant & Advisor

---

## Tech Stack

- **Minecraft Forge 1.20.1** (MDK 47.4.20)
- **Java 17+** (Gradle JVM 3G)
- **Gradle** (ForgeGradle build system)
- **Mixin** (SpongePowered Mixin 0.8.5, runtime bytecode injection)
- **Gson** (Config serialization/deserialization)

### Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| Cloth Config 11.1.136 | Compile-only | Client config GUI rendering |
| CorgiLib 4.0.3.4 | Compile-only | Utility library |
| Data Anchor 1.0.0.20 | Compile-only | Data persistence |
| Enhanced Celestials 5.0.3.2 | Compile-only | Blood moon integration |
| Guard Villagers 1.6.18 | Compile-only | Guard infection system |
| Musket Mod 1.5.4 | Compile-only | Musket gunner zombie + sound sensing |
| Spartan Shields 3.1.1 | Compile-only | Shield compatibility |
| Spartan Weaponry 3.2.1 | Compile-only | Weapon compatibility |
| TACZ 1.1.7 | Compile-only | Gunshot sensing |
| GeckoLib 4.8.4 | Compile-only | Animation system |
| Point Blank 1.11.1 | Compile-only | Gunshot sensing |
| MineTraps 2.3.0 | Compile-only | Trap integration |

> Compile-only dependencies do not need to be installed at runtime. Integration features will be enabled automatically when the corresponding mod is detected.

---

## Project Structure

```
src/main/java/com/aljun/zombiegamereborn/
├── ZombieGameReborn.java           # Mod entry point (@Mod annotation)
├── api/                            # Public API
│   ├── ZGRCommonAPI.java
│   ├── ZGRPlayerAPI.java
│   ├── ZGRZombieAttributesAPI.java
│   └── ZGRZombieControlAPI.java
├── common/                         # Core logic
│   ├── client/ResourcePackDetector.java
│   ├── commands/                   # Command system
│   │   ├── ConfigCommand.java      # /zombiegamereborn config
│   │   ├── PlayerCommand.java
│   │   ├── SummonZombieCommand.java
│   │   └── ZGRCommands.java
│   ├── config/                     # Config system (core)
│   │   ├── GameProperty.java       # Game master config
│   │   ├── MobReplacement.java     # Mob replacement config
│   │   ├── StageProperty.java      # Stage config
│   │   ├── ZombieProperty.java     # Zombie property config
│   │   ├── ZGRConfigFileManager.java # Config file manager
│   │   └── ZombieSpawnChooser.java # Spawn selector
│   ├── game/                       # Game runtime
│   │   ├── DayTime.java            # Time system
│   │   ├── ZGRGame.java            # Game state singleton
│   │   └── ZombieStatic.java       # Zombie static data
│   ├── optimizer/                  # Performance optimization
│   │   └── ZombieGoalOptimizer.java
│   └── player/                     # Player management
│       ├── PlayerStatic.java
│       ├── ReginalStageDetector.java # Regional stage detection
│       └── TimeBroadcast.java      # Time broadcasting
├── debug/                          # Debug mode
│   ├── ZGRDebug.java
│   └── events/ZGRDebugEvents.java
├── diplomat/                       # Mod integration layer (polymorphic diplomat)
│   ├── Diplomat.java               # Diplomat interface
│   ├── ZGRDiplomacyCenter.java     # Diplomacy center (init all diplomats)
│   ├── enhancedcelestials/         # Blood moon integration
│   ├── guardvillagers/             # Guard villager integration
│   ├── musketmod/                  # Musket mod integration
│   ├── pointblank/                 # Point Blank integration
│   └── tacz/                       # TACZ integration
├── mixins/                         # Mixin injection
│   ├── client/                     # Client mixins
│   │   ├── AbstractZombieModelMixin.java
│   │   ├── HumanoidModelMixin.java
│   │   └── ZombieVillagerModelMixin.java
│   ├── musketmod/                  # Musket Mod mixins
│   │   ├── BulletEntityMixin.java
│   │   └── GunItemFireMixin.java
│   └── pointblank/                 # Point Blank mixins
│       └── MainHeldSimplifiedStateSyncRequestMixin.java
├── network/                        # Network sync
│   ├── ZGRNetwork.java             # Network channel registration
│   └── packet/                     # Packets
│       ├── GamePropertyDownloadPacket.java
│       ├── GamePropertyUploadPacket.java
│       ├── LoginWelcomePacket.java
│       ├── OpenClientConfigScreenPacket.java
│       ├── TimeBroadcastPacket.java
│       └── ZombieCapacitySyncPacket.java
├── register/                       # Registry system
│   ├── ZGRCommonRegister.java
│   ├── ZGRRegistries.java
│   └── ZGRSpecialRegisterEvents.java
├── sounds/                         # Sound effects
│   └── ZGRSoundEvents.java
└── utils/                          # Utilities
    ├── GamePropertyPresentUtils.java
    ├── JsonUtils.java
    ├── MathUtils.java
    ├── PathConstructor.java
    ├── RandomUtils.java
    └── ZombieUtils.java
```

### Resources

```
src/main/resources/
├── META-INF/mods.toml              # Mod metadata
├── pack.mcmeta                     # Resource pack description
├── mixins.zombiegamereborn.json    # Mixin config
├── logo.png                        # Mod icon
└── assets/zombiegamereborn/
    ├── sounds.json                  # Sound registry
    ├── lang/
    │   ├── en_us.json               # English localization
    │   └── zh_cn.json               # Chinese localization
    └── sounds/                      # Audio assets
        ├── clock_ring.ogg
        ├── evening_howl.ogg
        └── morning_roast.ogg
```

```
Documentury/                         # Documentation
├── en_us/ConfigFileGuide.md         # English config guide
└── zh_cn/配置文件指南.md             # Chinese config guide
```

---

## Architecture & Design Principles

### 1. Config System

Three-tier configuration structure:

```
GameProperty (master config)
├── Global fields (max_empowered_*, global_*, behavior switches)
└── stages[] (stage list)
    └── stage (index)
```

- **Serialization**: Custom `GamePropertyAdapter` (Gson TypeAdapter) for JSON serialization/deserialization
- **Load priority**: Server world save > global default config > built-in initial defaults
- **Config directory**: `config/zombiegamereborn/` (client config + presets)
- **Preset manager**: In-game GUI via `/zombiegamereborn config gameProperty`

### 2. Zombie Type System

Each zombie type has its own `ZombieProperty`, differentiated by `zombie_type` field (not native NBT). Total 12 types:

| Type | Core AI |
|------|---------|
| `dummy` | No AI, stationary |
| `vanilla` | Vanilla behavior + equipment/enchant boost |
| `enhanced_vanilla` | Enhanced melee AI |
| `builder` | `ZombieBreakBlockGoal` + `ZombiePlaceBlockGoal` + bridging |
| `miner` | `ZombieSmartBreakAttackGoal` (smart tunneling) |
| `bow_attacker` | `ZombieBowAttackGoal` |
| `crossbow_attacker` | Ranged crossbow (piglins auto-convert) |
| `shield_user` | `ZombieShieldGoal` + blocking + shield bash |
| `tnt_attacker` | `ZombieTNTAttackGoal` (throw/self-destruct TNT) |
| `musket_mod_gunner` | Musket ranged attack (requires Musket Mod) |
| `zombie_guard_villager` | Infected guard, can wield bow/crossbow/musket/shield |

Builder and Miner types use the **Empower system**: zombies compete dynamically for "empowered" status (`isEmpowered`), controlled by `max_empowered_builder_count` / `max_empowered_miner_count` caps.

### 3. Diplomat System (Polymorphic Mod Integration)

Strategy pattern + runtime detection for pluggable integration:

```
ZGRDiplomacyCenter
├── init() → initializes each Diplomat
│
├── EnhancedCelestialsDiplomat
│   └── IEnhancedCelestialsProvider (interface)
│       ├── EnhancedCelestialsProviderImpl (when mod is loaded)
│       └── (no-op fallback otherwise)
├── TaczDiplomat → ITaczProvider
├── MusketmodDiplomat → IMusketmodProvider
├── PointblankDiplomat → IPointblankProvider
└── GuardVillagersDiplomat
```

Each Diplomat detects whether its target mod is loaded (`ModList.get().isLoaded()`) during `init()`. If absent, it falls back to a no-op implementation, providing a consistent API to callers.

### 4. Network Sync

Built on **Forge SimpleChannel** for server↔client bidirectional sync:

| Packet | Direction | Purpose |
|--------|-----------|---------|
| `GamePropertyUploadPacket` | C→S | Client uploads config |
| `GamePropertyDownloadPacket` | S→C | Server distributes config |
| `LoginWelcomePacket` | S→C | Init config on login |
| `TimeBroadcastPacket` | S→C | In-game time broadcasting |
| `ZombieCapacitySyncPacket` | S→C | Zombie capacity sync |
| `OpenClientConfigScreenPacket` | S→C | Request client config screen |

### 5. Goal System & Optimization

All zombie types extend/override the vanilla `Zombie` Goal system. `ZombieGoalOptimizer` significantly optimizes pathfinding calls.

Goal priority reference:
| Priority | Goals |
|----------|-------|
| 1 (Highest) | `ZombieShieldGoal`, `ZombieBreakBlockGoal`, `ZombiePlaceBlockGoal`, `ZombieRemoveLightSourceGoal`, `ZombieFloatGoal` |
| 2 | `ZombieRestrictSunGoal`, `ZombieWaterBridgeBuildGoal`, `ZombieTNTAttackGoal` |
| 3 | `ZombieBowAttackGoal`, `ZombieMeleeAttackGoal` |
| ... | ... |

---

## Build & Development

### Prerequisites

- JDK 17+
- Git
- At least 4GB RAM (IDE + Gradle concurrent)

### Build

```bash
# Windows (PowerShell)
gradlew build

# Output JAR at build/libs/
```

### Dev Run

```bash
# Start Minecraft client
gradlew runClient

# Start dedicated server
gradlew runServer

# Run data generators (auto-generates some resources)
gradlew runData
```

### IDE Setup

Recommended: IntelliJ IDEA
1. Clone repo and run `gradlew idea` to generate project files
2. Open `build.gradle` as project
3. ForgeGradle auto-configures run configurations (`runClient`, `runServer`)

---

## Development Notes

### Mixin

- Mixin config: `src/main/resources/mixins.zombiegamereborn.json`
- Client mixins go in `mixins/client/` package (must be server-safe via `@OnlyIn(Dist.CLIENT)`)
- Third-party mod mixins (Musket Mod, Point Blank) go in their respective subpackages
- Debug: set `debug.verbose = true` in mixin config for verbose output

### Client Class References

Client-specific classes (GUI screens, models) are referenced via reflection (see `ZGRNetwork.java` `Class.forName()` pattern) to avoid `ClassNotFoundException` on dedicated servers. When adding client features:
- Place client handler classes in `common.client` package
- Call client methods via reflection from server code
- Use `ctx.get().enqueueWork()` + `Class.forName()` dispatch pattern (see `ZGRNetwork.java`)

### Localization

- English: `assets/zombiegamereborn/lang/en_us.json`
- Chinese: `assets/zombiegamereborn/lang/zh_cn.json`
- Entity localization key format: `entity.zombiegamereborn.<zombie_type_name>`

### Config Documentation

When adding, modifying, or removing config fields, synchronize both:
- `Documentury/zh_cn/配置文件指南.md`
- `Documentury/en_us/ConfigFileGuide.md`
