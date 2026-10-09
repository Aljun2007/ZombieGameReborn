# 僵尸游戏 (ZombieGame:Reborn)

Minecraft **1.21.1 / NeoForge** 版，从 1.20.1 Forge 版 `ZombieGame:Reborn` 2.1.0 移植而来（原作：Aljun2007）。

强化僵尸 AI 的末日模组：僵尸会**挖穿方块**接近玩家、拿方块**搭桥建造**，并随存活天数推进**阶段**逐步变强；
带感染机制（村民 / 猪灵）、时间播报与铃声，另有一层与枪械模组联动的「外交层」（Diplomat）。

## 安装

1. 把 `zombiegamereborn-2.1.0.jar` 放进 `mods/`。
2. **没有硬前置**：`neoforge.mods.toml` 只声明 `neoforge >= 21.1.150` 与 `minecraft 1.21.1`。
3. 单机 / 服务端都可装；配置界面是客户端功能，纯服务端也能正常运行。

## 可选联动（软依赖，缺任何一个都能正常启动）

| 模组 | 联动内容 |
|---|---|
| TaCZ（1.21.1 NeoForge 移植版 1.1.8） | 僵尸持枪的 `GunFireEvent` / `EntityHurtByGunEvent`、消音器判断 |
| PointBlank 1.11.1 | 僵尸持枪的客户端状态同步、枪械音效特性 |
| MusketMod 1.5.4 | 火枪僵尸的攻击目标与开火行为 |
| Enhanced Celestials 6.x / 2-Core 2.x | 血月联动（`debug setBloodMoon` 需要它，未装时命令会提示「未安装月亮事件模组」） |
| GuardVillagers | 纯反射调用，装了即生效 |

以上全部用 `ModList.get().isLoaded(...)` 守卫，不装不会崩。

## 玩法速览

- **挖掘 / 建造**：`can_zombie_break_block`、`can_zombie_place_block`，并限制同时激活的数量
  （`max_empowered_builder_count`、`max_empowered_miner_count`）。
- **阶段系统**：`stage_properties` 按天数定义各阶段的僵尸规模与属性，`max_zombie_count` 是全局上限。
- **感染**：`can_piglin_infection`（猪灵与疣猪兽可被感染）、`infected_villager_can_break_blocks`。
- **寻路 / 性能**：`rough_pathfinding_threshold`、`rough_pathfinding_interval`、`simplified_builder_movement`。
- **时间**：清晨 / 上午 / 正午 / 下午 / 黄昏 / 入夜 / 深夜 / 午夜 / 破晓 播报 + 铃声音效（客户端可关）。
- **成就**：9 个（尸潮、九百九十九・腐肉杀无休、大人，时代变了 ……）。

## 感知系统

僵尸不是“谁出声就追谁”。感知层是事件驱动的刺激系统：世界只写下**发生了什么、发生在哪**，
每只僵尸按自己的相位决定附近有没有值得去看的东西。

| 通道 | 来源 | 反应 |
|---|---|---|
| 声音 | 服务端每一次播音（过滤环境音与脚步声） | 走到声音位置调查；来源可攻击时直接锁定 |
| 气味 | 玩家走过留下的衰减轨迹（疾跑更浓、重伤更浓、潜行更淡） | 沿浓度梯度追踪；断味后去最后闻到的位置看一眼 |
| 冲击 | 方块破坏、爆炸 | 半径更大、优先级高于普通声音 |
| 警报 | 锁定目标的僵尸通知附近同类 | 只提供位置，且有冷却 / 每 tick 配额 / 跳数上限 |
| 光照 | 玩家所处亮度（每 20 tick 采样） | 亮处被发现得更远，黑暗里被隐藏 |

最关键的性质是**空载零开销**：没有任何刺激时，每只僵尸每 tick 只做两次整数比较；
事件写入是 O(1) 且从不查询实体；气味是惰性衰减的稀疏网格而不是实体。
旧实现每来一个声音、一次破坏、一枪、一次爆炸，都会遍历 512 格立方体内的全部实体。

实测（300 只僵尸、真实服务端、MSPT 在 tick 内部采样）：感知系统空载成本**低于 1 ms/tick**，
300 只僵尸的基线是约 6.9 ms/tick（关掉感知约 6.0）。有意思的是，僵尸调查完站定不动时
反而比全场游荡更省 —— **寻路才是负载大头，感知系统不是**。原始数字与误差范围见 `感知系统设计.md`。

另外，僵尸**锁定你的时候会低吼一声**（原版音效，可用 `awareness_feedback_sounds` 关闭），
让玩家能听见"它们注意到我了"。模组自己播的这个音效会被自身的感知系统过滤掉，不会形成连锁。

完整设计说明与复杂度对比见 `感知系统设计.md`。

## 命令

根命令 `/zombiegamereborn`（OP；改服务器游戏配置另需权限等级 2）：

| 子命令 | 说明 |
|---|---|
| `summonZombie <类型> [数量] [坐标]` | 生成指定类型的僵尸 |
| `player day get` / `player day set <天数>` / `player day getGlobalAverageDouble` | 查询 / 设置存活天数、全体玩家平均天数 |
| `player isOnSurface` | 查询玩家是否在地表 |
| `player playTimeBroadCast` | 触发一次时间播报 |
| `config gameProperty` / `config client` | 打开游戏规则 / 客户端设置界面 |
| `debug heal` / `debug clean_all_zombies` / `debug setBloodMoon` / `debug debug_items` | 调试用 |

（子命令名以 `src/main/java/com/aljun/zombiegamereborn/common/commands/` 为准。）

## 配置

| 作用域 | 路径 |
|---|---|
| 全局默认游戏规则 | `config/zombiegamereborn/default_game_property.json` |
| 自定义预设 | `config/zombiegamereborn/game_properties/` |
| 存档级 | `<存档>/serverconfig/zgr_game_property.json` |
| 客户端 | `config/zombiegamereborn-client.json`（`time_broadcast_enabled` / `time_alarm_enabled` / `login_message_enabled`） |

游戏规则也可以在游戏内用 `/zombiegamereborn config gameProperty` 的图形界面修改、导入 / 导出、上传到服务器。

## 从源码构建

```
./gradlew jar        # 需要 JDK 21，产物 build/libs/zombiegamereborn-2.1.0.jar
```

本仓库**已含**编译期用到的第三方 jar，就在 `libs/`（`build.gradle` 里以 `compileOnly` + `flatDir libs/` 引用），
克隆下来直接 `gradlew build` 即可，不需要自备。7 个 jar 的版本、来源与 sha512 见 `libs/SOURCES.md`：

```
tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar
pointblank-neoforge-1.21-1.11.1.jar
musketmod-1.21.1-neoforge-1.5.4.jar
Enhanced-Celestials-NeoForge-1.21.1-6.0.2.6.jar
Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.3.jar
data-anchor-neoforge-1.21.1-2.0.0.17.jar
geckolib-neoforge-1.21.1-4.9.3.jar
```

文件名必须和上面完全一致：`build.gradle` 用的是 `flatDir`，Gradle 只会按 `<name>-<version>.jar` 去找文件。

## 许可

All Rights Reserved（沿用原作）。原作仓库：https://github.com/Aljun2007/ZombieGameReborn

`libs/` 里的第三方 jar 版权归各自作者，仓库中仅作编译期引用，遵循各自的开源许可
（每个 jar 的版本、上游来源与 sha512 见 `libs/SOURCES.md`）。

移植改动清单与验证状态见 `移植说明.md`；API 迁移对照表见 `MIGRATION_NOTES.md`。
