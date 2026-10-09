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
| 40 | `setup`：forceload 测试区、关刷怪/昼夜/天气、在 y=249 铺平台、在 (8.5, 250, 8.5) 生成一只僵尸 |
| 240 | 打印基线标记 |
| 260 | `impact`：在 (30.5, 250, 8.5) 生成 TNT —— **距僵尸 22 格**，约 4 秒后爆炸 |
| 760 | 打印标记 |
| 1160 | `bait`：在爆炸点放一只村民（观察有目标时的行为） |
| 1400 / 2400 | 打印标记 / 结束 |

> **平台必须建在空气里。** 第一次把平台建在 y=100，而世界出生点是一座 y≈136 的山 ——
> 僵尸被直接砌在石头里（`inWall=true blockAt=stone`），一步都走不动，看起来像模组坏了。
> 排查这类问题就用下面的调试日志。

## 期望看到什么

```
[ZGR awareness] EMIT IMPACT at (30.5, 250.0, 8.5) r=96.0 strength=40 gen=0 source=Primed TNT
[ZGR awareness] zombie#39 starts investigating IMPACT -> target (30.5, 250.0, 8.5) | zombie (8.5, 250.0, 8.5)
[ZGR awareness] zombie#39 investigating IMPACT | zombie (12.9, 250.0, 8.5) | distance=17.6 | navDone=false hasPath=true delta=0.0623
...
[ZGR awareness] zombie#39 investigating IMPACT | zombie (25.3, 250.0, 8.5) | distance=5.2
```

即：僵尸的 x 坐标从 8.5 一路涨到 28+，`distance` 单调下降。

实测结果见 `感知系统设计.md` 的「验证」一节。

## 调试日志都打印什么

| 日志 | 作用 |
|---|---|
| `EMIT <通道> at (...) r=... strength=... source=...` | 世界里有没有真的产生刺激（爆炸/声音有没有被接住） |
| `starts investigating <通道> -> target ... \| zombie ...` | 僵尸有没有真的开始调查，以及目标点解析对不对 |
| `investigating ... distance=... navDone=... hasPath=... delta=...` | 有没有在动。`hasPath=true` 但 `delta=0` 说明路径存在却走不动（多半是地形卡住） |
| `ARRIVED at target ...` | 到达并开始原地环视 |
| `STOPPED investigating ... \| target was ...` | 为什么结束（超时 / 转而战斗） |
| `DEBUG zombie#... inWall=... blockAt=... blockBelow=...` | 僵尸所处环境，排查"卡在方块里"这类问题 |
