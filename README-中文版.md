# ZombieGame:Reborn

**版本**: 1.4 | **Minecraft**: 1.20.1 | **Forge**: 47.4.20

**作者**:
- **Aljun2007**: 设计 & 开发
- **DeepSeek**: 助手 & 顾问

---

## 技术栈

- **Minecraft Forge 1.20.1** (MDK 47.4.20)
- **Java 17+** (Gradle JVM 参数 3G)
- **Gradle** (ForgeGradle 构建系统)
- **Mixin** (SpongePowered Mixin 0.8.5，运行时字节码注入)
- **Gson** (配置文件的序列化/反序列化)

### 外部依赖

| 依赖 | 类型 | 用途 |
|------|------|------|
| Cloth Config 11.1.136 | 编译期 | 客户端配置 GUI 渲染 |
| CorgiLib 4.0.3.4 | 编译期 | 工具库 |
| Data Anchor 1.0.0.20 | 编译期 | 数据锚点 |
| Enhanced Celestials 5.0.3.2 | 编译期 | 血月联动 |
| Guard Villagers 1.6.18 | 编译期 | 警卫感染系统 |
| Musket Mod 1.5.4 | 编译期 | 火枪手僵尸 + 枪声感知 |
| Spartan Shields 3.1.1 | 编译期 | 盾牌适配 |
| Spartan Weaponry 3.2.1 | 编译期 | 武器适配 |
| TACZ 1.1.7 | 编译期 | 枪声感知 |
| GeckoLib 4.8.4 | 编译期 | 动画系统 |
| Point Blank 1.11.1 | 编译期 | 枪声感知 |
| MineTraps 2.3.0 | 编译期 | 陷阱联动 |

> 编译期依赖无需在最终运行环境中安装，但若安装了对应模组则会启用对应联动功能。

---

## 项目结构

```
src/main/java/com/aljun/zombiegamereborn/
├── ZombieGameReborn.java           # Mod 主入口（@Mod 注解）
├── api/                            # 公开 API
│   ├── ZGRCommonAPI.java
│   ├── ZGRPlayerAPI.java
│   ├── ZGRZombieAttributesAPI.java
│   └── ZGRZombieControlAPI.java
├── common/                         # 核心逻辑
│   ├── client/ResourcePackDetector.java
│   ├── commands/                   # 指令系统
│   │   ├── ConfigCommand.java      # /zombiegamereborn config
│   │   ├── PlayerCommand.java
│   │   ├── SummonZombieCommand.java
│   │   └── ZGRCommands.java
│   ├── config/                     # 配置系统（核心）
│   │   ├── GameProperty.java       # 游戏主配置
│   │   ├── MobReplacement.java     # 生物替换配置
│   │   ├── StageProperty.java      # 阶段配置
│   │   ├── ZombieProperty.java     # 僵尸属性配置
│   │   ├── ZGRConfigFileManager.java # 配置文件管理器
│   │   └── ZombieSpawnChooser.java # 生成选择器
│   ├── game/                       # 游戏运行时
│   │   ├── DayTime.java            # 时间系统
│   │   ├── ZGRGame.java            # 游戏状态单例
│   │   └── ZombieStatic.java       # 僵尸静态数据
│   ├── optimizer/                  # 性能优化
│   │   └── ZombieGoalOptimizer.java
│   └── player/                     # 玩家管理
│       ├── PlayerStatic.java
│       ├── ReginalStageDetector.java # 区域阶段检测
│       └── TimeBroadcast.java      # 时间广播
├── debug/                          # 调试模式
│   ├── ZGRDebug.java
│   └── events/ZGRDebugEvents.java
├── diplomat/                       # 模组联动层（多态外交系统）
│   ├── Diplomat.java               # 外交官接口
│   ├── ZGRDiplomacyCenter.java     # 外交中心（初始化所有外交官）
│   ├── enhancedcelestials/         # 血月联动
│   ├── guardvillagers/             # 警卫联动
│   ├── musketmod/                  # 火枪模组联动
│   ├── pointblank/                 # Point Blank 联动
│   └── tacz/                       # TACZ 联动
├── mixins/                         # Mixin 注入
│   ├── client/                     # 客户端 Mixin
│   │   ├── AbstractZombieModelMixin.java
│   │   ├── HumanoidModelMixin.java
│   │   └── ZombieVillagerModelMixin.java
│   ├── musketmod/                  # Musket Mod Mixin
│   │   ├── BulletEntityMixin.java
│   │   └── GunItemFireMixin.java
│   └── pointblank/                 # Point Blank Mixin
│       └── MainHeldSimplifiedStateSyncRequestMixin.java
├── network/                        # 网络同步
│   ├── ZGRNetwork.java             # 网络通道注册
│   └── packet/                     # 数据包
│       ├── GamePropertyDownloadPacket.java
│       ├── GamePropertyUploadPacket.java
│       ├── LoginWelcomePacket.java
│       ├── OpenClientConfigScreenPacket.java
│       ├── TimeBroadcastPacket.java
│       └── ZombieCapacitySyncPacket.java
├── register/                       # 注册系统
│   ├── ZGRCommonRegister.java
│   ├── ZGRRegistries.java
│   └── ZGRSpecialRegisterEvents.java
├── sounds/                         # 音效
│   └── ZGRSoundEvents.java
└── utils/                          # 工具类
    ├── GamePropertyPresentUtils.java
    ├── JsonUtils.java
    ├── MathUtils.java
    ├── PathConstructor.java
    ├── RandomUtils.java
    └── ZombieUtils.java
```

### 资源文件

```
src/main/resources/
├── META-INF/mods.toml              # Mod 元信息
├── pack.mcmeta                     # 资源包描述
├── mixins.zombiegamereborn.json    # Mixin 配置
├── logo.png                        # Mod 图标
└── assets/zombiegamereborn/
    ├── sounds.json                  # 音效注册
    ├── lang/
    │   ├── en_us.json               # 英文语言文件
    │   └── zh_cn.json               # 中文语言文件
    └── sounds/                      # 音频资源
        ├── clock_ring.ogg
        ├── evening_howl.ogg
        └── morning_roast.ogg
```

```
Documentury/                         # 文档目录
├── en_us/ConfigFileGuide.md         # 英文配置文件指南
└── zh_cn/配置文件指南.md             # 中文配置文件指南
```

---

## 架构设计与核心原则

### 1. 配置系统（Config System）

配置采用 **三层级结构**：

```
GameProperty (游戏主配置)
├── 全局字段（max_empowered_*、global_*、behavior switches）
└── stages[] (阶段列表)
    └── stage (阶段序号)
```

- **序列化**：使用自定义 `GamePropertyAdapter`（Gson TypeAdapter）处理 JSON 序列化/反序列化
- **配置加载优先级**：服务器世界存档 > 全局默认配置 > 内置初始默认值
- **配置目录**：`config/zombiegamereborn/`（客户端配置 + 预设）
- **预设管理器**：游戏内 GUI 可通过 `/zombiegamereborn config gameProperty` 编辑所有参数

### 2. 僵尸类型系统（Zombie Type System）

每种僵尸类型对应一个 `ZombieProperty`，通过 `zombie_type` 字段区分（非原生的 NBT 标签）。共有 12 种类型：

| 类型 | 核心 AI |
|------|---------|
| `dummy` | 无 AI，站桩 |
| `vanilla` | 原版行为 + 装备/附魔增强 |
| `enhanced_vanilla` | 近战 AI 增强 |
| `builder` | `ZombieBreakBlockGoal` + `ZombiePlaceBlockGoal` + 搭桥 |
| `miner` | `ZombieSmartBreakAttackGoal`（智能挖掘路径） |
| `bow_attacker` | `ZombieBowAttackGoal` |
| `crossbow_attacker` | 远程弩攻击（猪灵持弩自动变身） |
| `shield_user` | `ZombieShieldGoal` + 格挡 + 盾击 |
| `tnt_attacker` | `ZombieTNTAttackGoal`（投掷/自爆 TNT） |
| `musket_mod_gunner` | 火枪远程射击（需 Musket Mod） |
| `zombie_guard_villager` | 感染警卫，可持有弓/弩/火枪/盾牌 |

Builder 和 Miner 类型使用 **Empower 机制**：通过配置竞争机制动态决定哪些僵尸被激活为"强化状态"（`isEmpowered`），受 `max_empowered_builder_count` / `max_empowered_miner_count` 上限控制。

### 3. 模组联动系统（Diplomat System — 多态外交）

采用 **策略模式 + 运行时检测** 实现可插拔联动：

```
ZGRDiplomacyCenter
├── init() → 依次初始化各 Diplomat
│
├── EnhancedCelestialsDiplomat
│   └── IEnhancedCelestialsProvider (接口)
│       ├── EnhancedCelestialsProviderImpl (有模组时)
│       └── (无模组时返回空实现)
├── TaczDiplomat → ITaczProvider
├── MusketmodDiplomat → IMusketmodProvider
├── PointblankDiplomat → IPointblankProvider
└── GuardVillagersDiplomat
```

每个外交官在 `init()` 时检测对应模组是否加载（`ModList.get().isLoaded()`），若未加载则降级为空实现，对外提供一致的 API 接口，调用方无需关心模组是否存在。

### 4. 网络同步

基于 **Forge SimpleChannel** 实现服务端↔客户端双向同步：

| 数据包 | 方向 | 用途 |
|--------|------|------|
| `GamePropertyUploadPacket` | C→S | 客户端上传配置 |
| `GamePropertyDownloadPacket` | S→C | 服务端下发配置 |
| `LoginWelcomePacket` | S→C | 登录时初始化配置 |
| `TimeBroadcastPacket` | S→C | 游戏内时间广播 |
| `ZombieCapacitySyncPacket` | S→C | 僵尸容量同步 |
| `OpenClientConfigScreenPacket` | S→C | 请求客户端打开配置界面 |

### 5. Goal 系统与优化

所有僵尸类型的行为继承/重写了原版 `Zombie` 的 Goal（目标）系统。`ZombieGoalOptimizer` 对寻路调用进行了大幅优化。

Goal 优先级参考（任务优先级表）：
| 优先级 | Goal 列表 |
|--------|-----------|
| 1（最高） | `ZombieShieldGoal`, `ZombieBreakBlockGoal`, `ZombiePlaceBlockGoal`, `ZombieRemoveLightSourceGoal`, `ZombieFloatGoal` |
| 2 | `ZombieRestrictSunGoal`, `ZombieWaterBridgeBuildGoal`, `ZombieTNTAttackGoal` |
| 3 | `ZombieBowAttackGoal`, `ZombieMeleeAttackGoal` |
| ... | ... |

---

## 构建与开发

### 前置要求

- JDK 17+
- Git
- 至少 4GB 可用内存（IDE + Gradle 并行）

### 构建步骤

```bash
# Windows (PowerShell)
gradlew build

# 构建完成后 Mod JAR 位于 build/libs/
```

### 开发运行

```bash
# 启动 Minecraft 客户端
gradlew runClient

# 启动专用服务器
gradlew runServer

# 运行数据生成器（自动生成部分资源文件）
gradlew runData
```

### IDE 配置

推荐使用 IntelliJ IDEA：
1. 克隆仓库后执行 `gradlew idea` 生成项目文件
2. 打开 `build.gradle` 作为项目
3. ForgeGradle 会自动配置运行配置（`runClient`、`runServer`）

---

## 开发注意事项

### Mixin

- Mixin 配置文件：`src/main/resources/mixins.zombiegamereborn.json`
- 客户端 Mixin 类位于 `mixins/client/` 包下（必须在服务端兼容，通过 `@OnlyIn(Dist.CLIENT)` 限制）
- 对第三方模组的 Mixin（如 Musket Mod、Point Blank）位于对应的子包中
- 调试模式：`mixins.zombiegamereborn.json` 中 `debug.verbose = true` 可开启详细输出

### 客户端类引用

客户端特定的类（如 GUI 屏幕、模型）通过反射调用（参见 `ZGRNetwork.java` 中 `Class.forName()` 模式），避免服务端加载时 `ClassNotFoundException`。如需新增客户端功能，请遵循以下模式：
- 客户端处理类放在 `common.client` 包下
- 服务端通过反射调用客户端方法
- 在 `ZGRNetwork.java` 中使用 `ctx.get().enqueueWork()` + `Class.forName()` 分发

### 语言文件

- 英文：`assets/zombiegamereborn/lang/en_us.json`
- 中文：`assets/zombiegamereborn/lang/zh_cn.json`
- 所有实体本地化键名格式：`entity.zombiegamereborn.<zombie_type_name>`

### 配置文件文档

- 如果新增、修改或删除配置字段，请同步更新 `Documentury/zh_cn/配置文件指南.md` 和 `Documentury/en_us/ConfigFileGuide.md`