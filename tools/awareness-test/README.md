# 感知系统功能测试（可复现）

这是对 `common/entity/awareness` 的端到端行为验证：**在僵尸旁边制造一次爆炸，看它是否会朝爆炸点走过去**。

它不是单元测试，而是真的跑一个服务端 + 真世界 + 真僵尸，所以能验证到"感知 → 决策 → 寻路 → 移动"整条链路。

## 为什么不用 `/summon` + 控制台命令

两个坑（都踩过）：

1. **Gradle 的 `runServer` 不转发 stdin**，往控制台喂命令是无效的；
2. **函数里执行的命令输出会被静默丢弃**（`say` 例外，因为它走的是广播）。所以 `data get entity ... Pos`
   打印不出坐标，得靠模组自己的调试日志。

因此测试用**数据包的 tick 函数**自己按 tick 计时驱动，完全不需要任何控制台输入。

## 怎么跑

1. 把本目录拷进测试世界的 `datapacks/`：

   ```
   run/world/datapacks/zgrtest/
   ```

   （新世界会自动启用；旧世界需要 `datapack enable`，而这就又要控制台输入了，所以**建议删掉 `run/world` 重新生成**。）

2. 打开感知调试日志（`build.gradle` 会把环境变量转发成系统属性）：

   ```powershell
   $env:ZGR_AWARENESS_DEBUG="true"
   .\gradlew.bat runServer
   ```

3. 观察 `run/logs/latest.log`。

## 测试时序

| tick | 动作 |
|---|---|
| 40 | `setup`：forceload 测试区、关刷怪/昼夜/天气、**杀掉所有会发声的生物**、在 y=249 铺平台、在 (8.5, 250, 8.5) 生成一只僵尸 |
| 40~400 | **完全安静期（18 秒）**——见下方说明 |
| 400 | 打印基线标记 |
| 420 | `impact`：在 (30.5, 250, 8.5) 生成 TNT —— **距僵尸 22 格**，约 4 秒后爆炸 |
| 800 | 打印标记 |
| 1400 | `finish`：结束 |

### 为什么要留 18 秒安静期

这是一条**回归测试**，专门覆盖曾经的一个严重 bug：

> `sweep()` 在“所有刺激都过期”时把内部高水位 `allocated` 归零，
> 但 `emit()` 复用空闲槽位时没有恢复它 —— 下一次写入停在 0 号槽、永远走不到“新建槽位”分支，
> `allocated` 再也涨不回来；而空载判据依赖 `allocated == 0`，
> **从此所有查询都返回 null，感知永久失效**。玩家只要安静十秒钟就无法再被感知到。

所以测试**故意**先制造一段没有任何声源的安静期，让所有刺激过期、触发那次 sweep，
然后再引爆 TNT。用调试日志里的 `DEBUG state[...]` 行判定：

```
DEBUG state[minecraft:overworld] live=0 allocated=3   ← 正确：allocated 保住了
DEBUG state[minecraft:overworld] live=0 allocated=0   ← 错误：感知已永久失明
```

`allocated` 在安静期后仍大于 0、且日志里没有 `STATE INCONSISTENT` 告警，才算通过。

> **平台必须建在空气里。** 第一次把平台建在 y=100，而世界出生点是一座 y≈136 的山 ——
> 僵尸被直接砌在石头里（`inWall=true blockAt=stone`），一步都走不动，看起来像模组坏了。
> 排查这类问题就用下面的调试日志。

## 期望看到什么

```
[ZGR awareness] EMIT IMPACT at (30.5, 250.0, 8.5) r=96.0 strength=40 gen=0 source=Primed TNT
[ZGR awareness] zombie#32 starts investigating IMPACT -> target (30.5, 250.0, 8.5) | zombie (8.5, 250.0, 8.5)
[ZGR awareness] zombie#32 investigating IMPACT | zombie (12.9, 250.0, 8.5) | distance=17.6 | navDone=false hasPath=true delta=0.0623
...
[ZGR awareness] zombie#32 ARRIVED at target (30.5, 250.0, 8.5) (channel IMPACT)
[ZGR awareness] zombie#32 STOPPED investigating IMPACT at (28.2, 250.0, 8.2) | target was (30.5, 250.0, 8.5)
```

即：僵尸的 x 坐标从 8.5 一路涨到 28+，`distance` 单调下降，最后 `ARRIVED`。

## 调试日志都打印什么

| 日志 | 作用 |
|---|---|
| `EMIT <通道> at (...) r=... strength=... source=...` | 世界里有没有真的产生刺激（爆炸/声音有没有被接住） |
| `starts investigating <通道> -> target ... \| zombie ...` | 僵尸有没有真的开始调查，以及目标点解析对不对 |
| `investigating ... distance=... navDone=... hasPath=... delta=...` | 有没有在动。`hasPath=true` 但 `delta=0` 说明路径存在却走不动（多半是地形卡住） |
| `ARRIVED at target ...` | 到达并开始原地环视 |
| `STOPPED investigating ... \| target was ...` | 为什么结束（超时 / 转而战斗） |
| `MSPT avg=.. ms, max=.. ms over 200 ticks` | 真实 tick 内部耗时（用 Pre→Post 的墙钟差，而不是含空闲睡眠的 Post 间隔） |
| `DEBUG state[...] live=.. allocated=.. scentCells=..` | 感知表自身状态，用于回归判定与容量观察 |
| `STATE INCONSISTENT` | **不变式被破坏**（`live > 0` 但 `allocated == 0`），一旦出现说明感知表逻辑有 bug |
| `DEBUG zombie#... inWall=... blockAt=... blockBelow=...` | 僵尸所处环境，排查"卡在方块里"这类问题 |

实测结果见 `感知系统设计.md` 的「验证」一节。

## 压力测试（MSPT）

`gen-stress-datapack.py` 生成另一套数据包：300 只僵尸站在 y=249 的开阔平台上，
可选地每 20 tick 引爆一次 TNT，用来测 tick 耗时。

```powershell
python tools\awareness-test\gen-stress-datapack.py     # 写进 <repo>/run/world/datapacks/zgrstress
$env:ZGR_AWARENESS_DEBUG="true"
.\gradlew.bat runServer
```

跑完从 `run\logs\latest.log` 里抓 `MSPT avg` 行。要做「感知开 / 关」对比，
就把 `run\config\zombiegamereborn\default_game_property.json` 里 11 处
`"awareness_enabled"` 一起改成 `false`，重置 `run\world` 后原样再跑一遍。

注意两个坑：

- **每轮之间必须清掉 `run\world`**，否则上一轮死掉的僵尸不算数，实体数对不上；
- **必须杀干净占着 25565 的进程**（`netstat -ano | Select-String ":25565"` 拿 PID 再 `Stop-Process`），
  否则服务端会以 `FAILED TO BIND TO PORT` 静默崩溃，你会读到上一轮的旧日志。

实测数字与不确定性说明见 `感知系统设计.md` 的「压力测试」一节。

## 气味轨迹跟随测试

`gen-scent-datapack.py` 生成第三套数据包，专门验证**气味梯度跟随**。

这条通道最难隔离：气味来源同时是合法攻击目标，僵尸会直接追人而不是跟轨迹。隔离办法是：

1. 把配置里的 `awareness_sound_radius` 压到 **6** —— 受伤音半径 = `soundRadius × 0.5` = 3，
   传不到僵尸身上，于是全世界只剩气味可感知（也就不会有目标可追）；
2. 把 `awareness_scent_decay_ticks` 调到 **2400** —— 否则轨迹会在观察前衰减干净；
3. 让一只村民沿 +X 每 40 tick 走 4 格并被打一下，共 13 格，留下带时间梯度的轨迹；
4. 杀掉村民（消除任何目标可能），把僵尸传送回轨迹头部，等最后一个受伤音过期（约 100 tick）后再观察。

```powershell
$env:ZGR_RUN_DIR="$PWD\run\world"
python tools\awareness-test\gen-scent-datapack.py
$env:ZGR_AWARENESS_DEBUG="true"
.\gradlew.bat runServer
```

判定：日志里应出现 `starts investigating SCENT`，并且僵尸的 x 坐标**持续增大**直到轨迹
最新的一端（`ARRIVED ... (channel SCENT)`）。若它停在原地反复 `ARRIVED`/`STOPPED`，
说明气味模式又被套上了定点模式的到达判定。

> **注意**：这套测试依赖把 `awareness_sound_radius` 压到 6。跑完记得把配置改回来，
> 或者直接删掉 `run\config\zombiegamereborn\` 让它按预设模板重新生成。
