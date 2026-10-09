package com.aljun.zombiegamereborn.common.entity.awareness;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 单个维度（ServerLevel）的感知状态。
 *
 * <h2>为什么这样设计</h2>
 * 旧的 {@code ZombieSenseManager.broadcastSense} 是“事件 → 扫描实体”的模型：
 * 每来一个事件就 {@code level.getEntitiesOfClass(Zombie.class, aabb)}，而 AABB 恒定按 512 格膨胀，
 * 于是玩家挖一个方块会遍历 1024³ 范围内的<b>全部实体</b>。事件越频繁、僵尸越多，代价线性叠加。
 *
 * <p>这里反过来：事件只往一个<b>定长数组</b>里写一条刺激（O(1)、无分配、无实体查询），
 * 僵尸以<b>错峰轮询</b>的方式自己来问“我现在附近有什么值得注意的”。
 * 由此得到两个关键性质：
 * <ul>
 *   <li><b>空载零开销</b>：没有任何刺激时 {@link #findStrongest} 直接返回 null，
 *       全场几百只僵尸的每 tick 成本就是几次 int 比较。</li>
 *   <li><b>成本与刺激数相关，与僵尸数无关</b>：僵尸通过错峰（见 {@code ZombieAwarenessGoal}）
 *       把轮询摊到多个 tick 上，且只读取自己附近的刺激。</li>
 * </ul>
 *
 * <p>数组容量固定，写满时覆盖最旧的一条，而不是扩容或丢新事件：
 * 这样内存恒定、永不 GC 抖动，也天然实现了“只关心最近发生的事”。
 */
public final class LevelAwareness {

    /** 刺激数组容量。同一时刻真实存活的一般在 0~16 条（同源合并后），96 是安全上限。 */
    public static final int MAX_STIMULI = 96;

    /** 可合并的最大距离（平方），6 格。 */
    private static final double MERGE_RADIUS_SQR = 36.0D;

    /** 合并时间窗：15 tick 内同通道同来源只保留一条。 */
    private static final int MERGE_WINDOW_TICKS = 15;

    /** 清理过期槽位的间隔（tick）。摊到每 tick 只有不到 10 次判断。 */
    private static final int SWEEP_INTERVAL = 10;

    private final Stimulus[] stimuli = new Stimulus[MAX_STIMULI];
    private final ScentGrid scent = new ScentGrid();

    /** 已分配的槽位数量（含可能已过期的）。用于减少无谓遍历。 */
    private int allocated;
    /** 上一次 sweep 后仍然存活的刺激数量。0 表示整个维度当前完全空闲。 */
    private int live;
    private long nextSweepTick;

    /** 每 tick 的警报配额，防止“一只发现 → 一群互吼”的警报风暴。 */
    private int alertBudgetUsed;
    private long alertBudgetTick = -1L;

    public ScentGrid scent() {
        return this.scent;
    }

    // ------------------------------------------------------------------
    // 写入
    // ------------------------------------------------------------------

    /**
     * 写入一条刺激。会先尝试与相近的既有刺激合并。
     *
     * @return 实际生效的刺激（可能是被合并的那条），用于调用方判断是否需要后续动作
     */
    public Stimulus emit(AwarenessChannel channel, double x, double y, double z, double radius,
                         int strength, int lifespan, int generation, @Nullable Entity source, long now) {
        if (radius <= 0.0D || lifespan <= 0) {
            return null;
        }

        // 1) 合并：同一个玩家连挖 5 个方块只应该产生 1 条刺激
        final int sourceId = source == null ? -1 : source.getId();
        for (int i = 0; i < this.allocated; i++) {
            Stimulus s = this.stimuli[i];
            if (s == null || s.free) continue;
            if (s.canMergeWith(channel, x, y, z, sourceId, MERGE_RADIUS_SQR, now, MERGE_WINDOW_TICKS)) {
                s.merge(x, y, z, radius, strength, now);
                this.live = Math.max(this.live, 1);
                return s;
            }
        }

        // 2) 找空槽；没有就直接找最旧的槽覆盖
        Stimulus slot = null;
        int oldestIndex = 0;
        long oldestBorn = Long.MAX_VALUE;
        for (int i = 0; i < MAX_STIMULI; i++) {
            Stimulus s = this.stimuli[i];
            if (s == null) {
                slot = new Stimulus();
                this.stimuli[i] = slot;
                this.allocated = Math.max(this.allocated, i + 1);
                break;
            }
            if (s.free) {
                slot = s;
                this.allocated = Math.max(this.allocated, i + 1);
                break;
            }
            if (s.bornTick < oldestBorn) {
                oldestBorn = s.bornTick;
                oldestIndex = i;
            }
        }
        if (slot == null) {
            // 数组写满：覆盖最旧的一条。宁可丢掉最老的事件，也不扩容、不丢新事件。
            slot = this.stimuli[oldestIndex];
        }

        slot.set(channel, x, y, z, radius, strength, lifespan, generation, source, now);
        if (this.live == 0) {
            this.live = 1;
        }
        return slot;
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    /**
     * 查询某个位置附近“最值得注意”的一条刺激。
     * <p>
     * 评分 = 通道权重 × 强度 − 距离惩罚；返回 null 表示附近什么都没有（最常见的情况）。
     * <p>
     * <b>无分配</b>：不使用流、不构造集合、不装箱。
     */
    @Nullable
    public Stimulus findStrongest(double x, double y, double z, long now, long channelFilterMask) {
        return findStrongest(x, y, z, now, channelFilterMask, false);
    }

    /**
     * @param requireSource 只考虑带实体来源的刺激。
     *                      <p>
     *                      “锁定目标”那条链路必须传 true：否则会出现这样一种真实情况 ——
     *                      目标生物自己发出的环境音（无来源、评分略高）把它的受伤音（有来源）挤掉，
     *                      于是僵尸感知到了动静却永远锁不上目标。
     */
    @Nullable
    public Stimulus findStrongest(double x, double y, double z, long now, long channelFilterMask,
                                  boolean requireSource) {
        if (this.live == 0) {
            return null;    // 空载快速通道
        }
        if (now >= this.nextSweepTick) {
            sweep(now);
            if (this.live == 0) {
                return null;
            }
        }

        Stimulus best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.allocated; i++) {
            Stimulus s = this.stimuli[i];
            if (s == null || s.free || s.isExpired(now)) continue;
            if ((channelFilterMask & (1L << s.channel.ordinal())) == 0L) continue;
            if (requireSource && s.source == null) continue;
            double dx = s.x - x;
            double dy = s.y - y;
            double dz = s.z - z;
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr > s.radius * s.radius) continue;
            double score = s.strength * AwarenessTuning.channelWeight(s.channel) - Math.sqrt(distSqr);
            if (score > bestScore) {
                bestScore = score;
                best = s;
            }
        }
        return best;
    }

    /** 清理过期槽位并重算 live。 */
    public void sweep(long now) {
        int alive = 0;
        for (int i = 0; i < this.allocated; i++) {
            Stimulus s = this.stimuli[i];
            if (s == null || s.free) continue;
            if (s.isExpired(now)) {
                s.reset();
            } else {
                // 别让感知表长期强引用已经消失的实体（生命最长的是气味刺激，可达 600 tick）
                if (s.source != null && (s.source.isRemoved() || !s.source.isAlive())) {
                    s.source = null;
                }
                alive++;
            }
        }
        this.live = alive;
        this.nextSweepTick = now + SWEEP_INTERVAL;
        // 注意：allocated 是“已创建槽位的高水位”，绝不在这里归零。
        // 归零会造成【永久失明】：槽位对象仍然存在（只是 free=true），下一次 emit 会直接复用 0 号槽，
        // 走不到“s == null 就新建”那条分支，allocated 再也涨不回来；
        // 而空载判据一旦依赖 allocated == 0，感知就彻底失效了（历史上确实踩过这个坑）。
    }

    /** 申请一次警报配额；超限返回 false（该 tick 不再传播警报）。 */
    public boolean tryConsumeAlertBudget(long now, int perTick) {
        if (now != this.alertBudgetTick) {
            this.alertBudgetTick = now;
            this.alertBudgetUsed = 0;
        }
        if (this.alertBudgetUsed >= perTick) {
            return false;
        }
        this.alertBudgetUsed++;
        return true;
    }

    /** 维度卸载时调用，避免状态跨存档残留。 */
    public void clear() {
        for (int i = 0; i < this.allocated; i++) {
            Stimulus s = this.stimuli[i];
            if (s != null) s.reset();
        }
        this.allocated = 0;
        this.live = 0;
        this.nextSweepTick = 0L;
        this.scent.clear();
    }

    /** 调试用：当前存活刺激数。 */
    public int liveCount() {
        return this.live;
    }

    /** 调试用：已创建槽位的高水位。 */
    public int allocatedCount() {
        return this.allocated;
    }

    /**
     * 状态自检。
     * <p>
     * 不变式：只要有存活刺激，allocated 必须大于 0；
     * 否则 {@link #findStrongest} 会在空载快速通道里直接返回 null，感知永久失明。
     */
    public boolean isStateConsistent() {
        return this.live == 0 || this.allocated > 0;
    }

    /** 调试用：一行状态摘要。 */
    public String debugStats() {
        return "live=" + this.live + " allocated=" + this.allocated
                + " scentCells=" + this.scent.cellCount();
    }
}
