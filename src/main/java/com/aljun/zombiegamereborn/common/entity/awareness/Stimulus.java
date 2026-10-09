package com.aljun.zombiegamereborn.common.entity.awareness;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 一条感知刺激：某个时间、某个位置发生了某类事件，在有限半径内对僵尸可见。
 * <p>
 * 这是整个感知系统的<b>唯一数据单元</b>，对象会被池化复用（见 {@link LevelAwareness}），
 * 因此这里全部是可变公有字段，且<b>不持有集合、不分配</b>。
 * <p>
 * 与旧实现的关键区别：旧实现记录的是“制造刺激的实体”（{@code InterestPoint.creator}），
 * 于是僵尸只能去追那个实体；这里记录的是<b>位置</b>，实体只是可选来源信息，
 * 僵尸可以去调查一个点（声音传来的地方、气味浓的地方），即使那里已经没人。
 */
public final class Stimulus {

    /** 池化标记：true 表示这个槽位可以被复用。 */
    boolean free = true;

    public AwarenessChannel channel = AwarenessChannel.SOUND;

    public double x;
    public double y;
    public double z;

    /** 生效半径（格）。僵尸必须在这个半径内才可能感知到。 */
    public double radius;

    /** 强度/优先级，越大越优先，用于同 tick 多条刺激的取舍与合并。 */
    public int strength;

    /** 生成时刻（level.getGameTime()）。 */
    public long bornTick;

    /** 存活时长（tick），到期即失效。 */
    public int lifespan;

    /** 警报跳数，0 表示直接来源；超过上限的不再二次传播。 */
    public int generation;

    /** 可选的来源实体。可能为 null（纯位置刺激），也可能已失效。 */
    @Nullable
    public Entity source;

    /** 来源实体的 id，用于避免同一实体同一通道反复覆盖（不持有强引用）。 */
    public int sourceId = -1;

    /** 最近一次被刷新的 tick，用于合并判定。 */
    public long refreshTick;

    boolean isExpired(long now) {
        return now - bornTick >= lifespan;
    }

    void set(AwarenessChannel channel, double x, double y, double z, double radius, int strength,
             int lifespan, int generation, @Nullable Entity source, long now) {
        this.free = false;
        this.channel = channel;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.strength = strength;
        this.bornTick = now;
        this.refreshTick = now;
        this.lifespan = lifespan;
        this.generation = generation;
        this.source = source;
        this.sourceId = source == null ? -1 : source.getId();
    }

    /** 是否与另一条刺激足够接近，可以合并（同通道、近距离、还在新鲜期内）。 */
    boolean canMergeWith(AwarenessChannel otherChannel, double ox, double oy, double oz,
                         int otherSourceId, double mergeRadiusSqr, long now, int mergeWindowTicks) {
        if (this.free || this.channel != otherChannel) return false;
        if (now - this.refreshTick > mergeWindowTicks) return false;
        // 同来源优先合并，避免同一个玩家连续开枪塞满整个数组
        if (otherSourceId != -1 && this.sourceId == otherSourceId) return true;
        double dx = this.x - ox;
        double dy = this.y - oy;
        double dz = this.z - oz;
        return dx * dx + dy * dy + dz * dz <= mergeRadiusSqr;
    }

    void merge(double ox, double oy, double oz, double otherRadius, int otherStrength, long now) {
        // 位置取加权中点，半径取大者，强度累加但设上限，寿命刷新
        this.x = (this.x + ox) * 0.5D;
        this.y = (this.y + oy) * 0.5D;
        this.z = (this.z + oz) * 0.5D;
        if (otherRadius > this.radius) this.radius = otherRadius;
        this.strength = Math.min(this.strength + otherStrength, 1000);
        this.refreshTick = now;
        this.bornTick = now;
    }

    void reset() {
        this.free = true;
        this.source = null;
        this.sourceId = -1;
        this.generation = 0;
        this.strength = 0;
    }
}
