# ZombieGame:Reborn

**Version**: 2.1.0 | **Minecraft**: 1.21.1 | **NeoForge**: 21.1.255

**Branch**: `neoforge-1.21.1` — NeoForge 1.21.1 port of the 1.20.1 Forge version (`zgr-forge-1.20.1`)

**Authors**:
- **Aljun2007**: Design & Development
- **DeepSeek**: Assistant & Advisor

---

## Overview

The rebirth of ZombieGame [Mod]: an apocalypse mod that enhances vanilla zombie AI.

Zombies **mine through blocks** to reach the player and **bridge / build with blocks** to cross gaps,
growing stronger in **stages** as the survived-day counter advances. It also adds infection
(villagers / piglins), in-game time broadcasts with bell sounds, and a pluggable integration layer
("Diplomat") for gun mods.

This branch is a **loader port only**: class names, method names, config keys, packet names, command
names and gameplay values are kept identical to the 1.20.1 Forge version. Only loader APIs and the
vanilla 1.21.1 changes are adapted. See `移植说明.md` (Chinese) and `MIGRATION_NOTES.md` (API mapping
table) for the full change list and verification status.

---

## Tech Stack

- **Minecraft NeoForge 1.21.1** (NeoForge 21.1.255)
- **Java 21** (Gradle JVM 3G)
- **ModDevGradle 2.0.148** + **Gradle 9.5**
- **Parchment** mappings 2024.11.17
- **Mixin** (SpongePowered Mixin 0.8.5, `compatibilityLevel = JAVA_21`)
- **Gson** (Config serialization/deserialization)

### Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| TaCZ 1.1.8-hotfix-r7 (NeoForge 1.21.1 port) | Compile-only | Gunshot sensing (`GunFireEvent` / `EntityHurtByGunEvent`), silencer check |
| Point Blank 1.11.1 | Compile-only | Gun-holding zombie client state sync + sound features |
| Musket Mod 1.5.4 | Compile-only | Musket gunner zombie target selection and firing |
| Enhanced Celestials 6.0.2.6 | Compile-only | Blood moon integration |
| Enhanced Celestials 2: Core 2.0.3.3 | Compile-only | Blood moon integration |
| Data Anchor 2.0.0.17 | Compile-only | Data persistence |
| GeckoLib 4.9.3 | Compile-only | Animation system |
| Guard Villagers | Runtime reflection only | Guard infection system |

> Compile-only dependencies do not need to be installed at runtime. Integration features are enabled
> automatically when the corresponding mod is detected (`ModList.get().isLoaded(...)` guards); a
> missing mod never crashes the game.

---

## Project Structure

```
src/main/java/com/aljun/zombiegamereborn/
├── ZombieGameReborn.java               # Mod entry point (@Mod + IEventBus / ModContainer injection)
├── api/                                # Public API
│   ├── ZGRCommonAPI.java
│   ├── ZGRPlayerAPI.java
│   ├── ZGRZombieAttributesAPI.java
│   └── ZGRZombieControlAPI.java
├── common/
│   ├── attachment/ZGRAttachments.java  # Data Attachment types (zombie / player data)
│   ├── client/                         # Client-only code
│   │   ├── config/                     # Client config (ClientConfig / ClientConfigManager)
│   │   ├── events/ClientHandler.java
│   │   ├── gui/                        # Config screens (client / core / stage)
│   │   ├── handler/ClientPacketHandlers.java
│   │   └── ResourcePackDetector.java
│   ├── commands/                       # /zombiegamereborn (+ config command, debug command)
│   ├── config/                         # GameProperty / StageProperty / ZombieProperty /
│   │                                   # MobReplacement / ZombieSpawnChooser / file manager
│   ├── entity/
│   │   ├── accessor/                   # Mixin accessors
│   │   ├── capability/                 # IZombieData / ZombieData (serialized via Data Attachment)
│   │   ├── equipement/ZombieEquipmentHelper.java
│   │   ├── goal/attack/                # Melee / bow / crossbow / shield / TNT / smart-break goals
│   │   ├── goal/attack/drowned/        # Drowned variants
│   │   ├── goal/behavior/              # Break block, place block, bridge, float, flee sun, shield
│   │   ├── goal/target/                # Target selection goals
│   │   ├── sense/                      # PointblankCallback (gun-sync callback; the old sense
│   │   │                               # system was replaced by entity/awareness/)
│   │   ├── awareness/                  # Event-driven awareness: stimulus pool, scent grid,
│   │   │                               # investigate/target goals, sound + light hooks
│   │   └── zombieType/                 # Zombie type registry + per-type implementations
│   ├── events/handler/                 # Gameplay / lifecycle event handlers
│   ├── game/                           # DayTime / ZGRGame / ZombieStatic
│   ├── optimizer/                      # ZombieGoalOptimizer / ZombieBlockOperationQueue
│   └── player/                         # PlayerStatic / ReginalStageDetector / TimeBroadcast
├── debug/                              # Debug mode (items, blood moon, zombie cleanup)
├── diplomat/                           # Mod integration layer (polymorphic diplomat)
│   ├── Diplomat.java
│   ├── ZGRDiplomacyCenter.java
│   ├── enhancedcelestials/             # Blood moon integration (1.x + 2-Core)
│   ├── guardvillagers/                 # Pure reflection
│   ├── musketmod/                      # Musket Mod integration
│   ├── pointblank/                     # Point Blank integration
│   └── tacz/                           # TaCZ integration
├── mixins/                             # Mixin injection
│   ├── client/                         # Zombie / humanoid / piglin / villager model mixins
│   ├── minecraft/entity/               # Entity, Mob, Zombie, Drowned, LivingEntity patches
│   ├── minecraft/entity/goal/          # Goal patches
│   ├── minecraft/item/ItemMixin.java
│   ├── minecraft/pathfinding/          # PathNavigation / WalkNodeEvaluator patches
│   ├── musketmod/                      # Bullet entity + gun fire mixins
│   └── pointblank/                     # Held state sync mixin
├── network/                            # Network sync
│   ├── ZGRNetwork.java                 # Payload registration (CustomPacketPayload + StreamCodec)
│   └── packet/                         # GameProperty upload/download, login welcome,
│                                       # time broadcast, capacity sync, client config screen
├── register/                           # ZGRCommonRegister / ZGRRegistries / special events
├── sounds/ZGRSoundEvents.java
└── utils/                              # Json / Math / Path / Random / Zombie / GameProperty utils
```

### Resources

```
src/main/resources/
├── META-INF/neoforge.mods.toml      # Mod metadata (NeoForge)
├── pack.mcmeta                      # pack_format 34
├── mixins.zombiegamereborn.json     # Mixin config (compatibilityLevel JAVA_21)
├── logo.png                         # Mod icon
├── assets/zombiegamereborn/
│   ├── sounds.json
│   ├── lang/{en_us,zh_cn}.json      # Localization
│   └── sounds/                      # clock_ring / evening_howl / morning_roast
└── data/zombiegamereborn/
    └── advancement/                 # 9 epic advancements (singular dir — 1.21.1 registry key)
```

---

## Architecture & Design Principles

### 1. Config System

Three-tier configuration structure, identical keys to the 1.20.1 version:

```
GameProperty (master config)
├── Global fields (max_empowered_*, global_*, behavior switches)
└── stages[] (stage list)
    └── stage (index)
```

- **Serialization**: custom `GamePropertyAdapter` (Gson TypeAdapter)
- **Load priority**: server world save > global default config > built-in initial defaults
- **Config directory**: `config/zombiegamereborn/` (client config + presets)
- **Preset manager**: in-game GUI via `/zombiegamereborn config gameProperty`
- **Loader API**: `ForgeConfigSpec` → `ModConfigSpec`, registered through `ModContainer.registerConfig`

### 2. Zombie Type System

Each zombie type has its own `ZombieProperty`, differentiated by the `zombie_type` field
(not native NBT). Type classes live in `common/entity/zombieType/type/`:

`dummy`, `enhanced_vanilla`, `builder`, `miner`, `bow_attacker`, `crossbow_attacker`,
`shield_user`, `tnt_attacker`, `musket_mod_gunner`, `zombie_guard_villager`, plus the drowned
variants (builder / miner / enhanced-vanilla / trident).

Builder and Miner types use the **Empower system**: zombies compete dynamically for "empowered"
status (`isEmpowered`), bounded by `max_empowered_builder_count` / `max_empowered_miner_count`.

### 3. Diplomat System (Polymorphic Mod Integration)

Strategy pattern + runtime detection:

```
ZGRDiplomacyCenter
├── init() -> initializes each Diplomat
├── EnhancedCelestialsDiplomat -> IEnhancedCelestialsProvider
│       ├── EnhancedCelestialsProviderImpl   (1.x)
│       └── EnhancedCelestials2ProviderImpl  (2-Core, Default Lunar Events)
├── TaczDiplomat         -> ITaczProvider
├── MusketmodDiplomat    -> IMusketmodProvider
├── PointblankDiplomat   -> IPointblankProvider
└── GuardVillagersDiplomat (pure reflection)
```

Each Diplomat checks `ModList.get().isLoaded()` during `init()` and falls back to a no-op
implementation when the target mod is absent, so callers always see a consistent API.

### 4. Network Sync

Rebuilt on NeoForge **`CustomPacketPayload` + `StreamCodec`** (the 1.20.1 `SimpleChannel` /
`NetworkRegistry` API is gone), registered via `RegisterPayloadHandlersEvent`; sending uses the
static `PacketDistributor` methods.

| Packet | Direction | Purpose |
|--------|-----------|---------|
| `GamePropertyUploadPacket` | C→S | Client uploads config |
| `GamePropertyDownloadPacket` | S→C | Server distributes config |
| `LoginWelcomePacket` | S→C | Init config on login |
| `TimeBroadcastPacket` | S→C | In-game time broadcasting |
| `ZombieCapacitySyncPacket` | S→C | Zombie capacity sync |
| `OpenClientConfigScreenPacket` | S→C | Open client config screen |

### 5. Data Storage

Forge **Capabilities were replaced by NeoForge Data Attachments**
(`common/attachment/ZGRAttachments.java`). Zombie and player data are stored as
`AttachmentType<ZombieData>` / `AttachmentType<PlayerData>` (serialized with `INBTSerializable`, saved
with the entity); call sites use `getData` / `setData` / `hasData` / `removeData`. The original
`IZombieData` / `IPlayerData` interfaces and their field / method signatures are unchanged.

### 6. Goal System & Optimization

All zombie types extend or override the vanilla `Zombie` goal system. `ZombieGoalOptimizer`
significantly reduces pathfinding calls, and `ZombieBlockOperationQueue` moves block operations out
of the AI tick to avoid chunk-lock stalls. Event signatures were updated to the 1.21.1 model
(`PlayerTickEvent.Post`, `LevelTickEvent.Pre/Post`, `ServerTickEvent.Pre/Post`,
`ClientTickEvent.Pre/Post`, `ICancellableEvent`).

## Awareness System

Zombies do not simply chase whatever made a noise. The awareness layer is an event-driven stimulus
system: the world writes *what happened and where*, and every zombie decides for itself — on a
staggered schedule — whether anything nearby is worth investigating.

| Channel | Source | Reaction |
|---|---|---|
| Sound | every server-side sound, minus ambient noise and footsteps | walk to the noise; if it has an attackable source, engage it |
| Scent | players leave a decaying trail (stronger when sprinting or wounded, fainter when crouching) | follow the gradient; when the trail runs out, check the last position |
| Impact | block breaking, explosions | larger radius and higher priority than ordinary noise |
| Alert | a zombie that acquires a target calls nearby kin | position only, rate limited (cooldown + per-tick budget + hop limit) |
| Light | the player's brightness, sampled every 20 ticks | spotted from further away in the light, hidden in the dark |

The important property is that an idle world costs nothing: with no active stimuli the per-zombie
per-tick cost is two integer comparisons, events are O(1) writes that never scan for entities, and
scent trails are a lazily decayed sparse grid rather than entities. The previous implementation
scanned every entity inside a 512-block box on *every* noise, block break, gunshot and explosion.

Measured cost (300 zombies, real dedicated server, MSPT sampled inside the tick): awareness adds
**under 1 ms/tick** while idle, and an idle horde of 300 zombies costs ~6.9 ms/tick with it versus
~6.0 ms/tick without it. Zombies standing still after investigating are *cheaper* than zombies
wandering — pathfinding, not awareness, dominates the load. See `感知系统设计.md` for the raw
numbers and their error bars.

Zombies also **growl when they lock onto you** (vanilla zombie sound, configurable via
`awareness_feedback_sounds`), so you can hear that you have been noticed. The mod's own feedback
sound is filtered out of its own awareness system, so it cannot cascade.

Full design notes and the complexity comparison: `感知系统设计.md`.

---

## Port Notes (1.20.1 Forge → 1.21.1 NeoForge)

| Change | Scope |
|--------|-------|
| Loader API | `net.minecraftforge.*` → `net.neoforged.*`; constructor-injected `IEventBus` / `ModContainer`; `@EventBusSubscriber`; `NeoForge.EVENT_BUS` |
| Capability → Data Attachment | `common/entity/capability/`, `common/player/capability/` |
| Network | 7 payloads rewritten to `CustomPacketPayload` + `StreamCodec` |
| Config | `ForgeConfigSpec` → `ModConfigSpec` |
| Deobfuscated member names | All classes: 1.20.1 `m_xxxxx_` / `f_xxxxx_` restored to Mojang names |
| Vanilla 1.21.1 API | `ResourceLocation.fromNamespaceAndPath/parse`, `ComponentSerialization.CODEC`, `Attributes.*`, `PathType`, `SoundEvents.*.location()`, hurt/damage variants |
| Mixin | `compatibilityLevel` → `JAVA_21`; all mixins kept |
| Datapack | `pack_format` → 34; data directory `advancements/` → `advancement/` (the plural directory is silently ignored on 1.21.1) |

---

## Build & Development

### Prerequisites

- JDK 21
- Git
- At least 4GB RAM (IDE + Gradle concurrent)

### Compile-only jars

The third-party jars this project compiles against are **vendored in `libs/`** (referenced by
`build.gradle` as `compileOnly` + `flatDir libs/`), so a fresh clone can build without extra setup.
Versions, upstream sources and sha512 checksums are recorded in `libs/SOURCES.md`:

```
tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar
pointblank-neoforge-1.21-1.11.1.jar
musketmod-1.21.1-neoforge-1.5.4.jar
Enhanced-Celestials-NeoForge-1.21.1-6.0.2.6.jar
Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.3.jar
data-anchor-neoforge-1.21.1-2.0.0.17.jar
geckolib-neoforge-1.21.1-4.9.3.jar
```

The file names must match exactly: `build.gradle` uses a `flatDir` repository, which only resolves
`<name>-<version>.jar`.

### Build

```bash
# Windows (PowerShell)
gradlew.bat build

# Output JAR: build/libs/zombiegamereborn-2.1.0.jar
```

### Dev Run

```bash
gradlew.bat runClient
gradlew.bat runServer
```

### IDE Setup

Recommended: IntelliJ IDEA — open `build.gradle` as a project; ModDevGradle configures the
`runClient` / `runServer` run configurations automatically.

---

## Development Notes

### Mixin

- Mixin config: `src/main/resources/mixins.zombiegamereborn.json`
- Client mixins go in the `mixins/client/` package; third-party mod mixins go in their own
  subpackages (`mixins/musketmod/`, `mixins/pointblank/`)
- No refmap / annotation processor is used: NeoForge 1.21.1 runs with official (Mojang) names, so
  mixin targets resolve by name

### Client Class References

Client-only classes (GUI screens, models) must not be loaded on a dedicated server; client handlers
live in `common/client/` and are dispatched through `common/client/handler/ClientPacketHandlers.java`.

### Localization

- English: `assets/zombiegamereborn/lang/en_us.json`
- Chinese: `assets/zombiegamereborn/lang/zh_cn.json`
- Entity localization key format: `entity.zombiegamereborn.<zombie_type_name>`

---

## Verification Status

| Item | Method | Result |
|------|--------|--------|
| Compile | `gradlew build` | Passed — `build/libs/zombiegamereborn-2.1.0.jar` (2.0 MB, 335 entries) |
| Dedicated server | NeoForge 21.1.255 | `Done (2.238s)!`, 0 ERROR |
| Dev client | ModDevGradle client | Atlas built, sound engine started, 0 ERROR |
| Mod init | log | `Zombie Game Reborn initialized!`, default config generated |

**Not yet tested**: in-game gameplay (mining / building, stage progression, infection, blood moon,
gun-mod integration), multiplayer concurrency, and zombie-heavy performance. See `移植说明.md`.

---

## License

All Rights Reserved (inherited from the original work).
Original repository: https://github.com/Aljun2007/ZombieGameReborn

This branch only adapts the mod to the 1.21.1 loader; gameplay values and art assets are unchanged.

The third-party jars vendored in `libs/` remain the property of their respective authors. They are
included only as compile-time references, under their own licenses (see `libs/SOURCES.md` for the
version, upstream source and sha512 checksum of each one).
