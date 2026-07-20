package com.aljun.zombiegamereborn.common.entity.goal.target;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 可配置多目标类型的 NearestAttackableTargetGoal 替代方案。
 * <p>
 * 将多个原版 NearestAttackableTargetGoal 合并为一个，通过优先级顺序的
 * 目标条目列表统一管理，减少实体扫描次数和 Goal 评估开销。
 * <p>
 * 目标条目按添加顺序决定优先级，先添加的先扫描命中。
 */
public class ZombieNearestAttackableTargetGoal extends TargetGoal {

    private static final int DEFAULT_RANDOM_INTERVAL = 10;
    /** 运行时高优先度目标重检间隔（tick） */
    private static final int RECHECK_INTERVAL = 10;

    private final int randomInterval;
    private final List<TargetEntry> targetEntries = new ArrayList<>();

    /** 自动递增优先度计数器，越晚添加优先度越低 */
    private int nextPriority = 0;
    /** entries 是否已排序，添加新条目后置 false */
    private boolean sorted = true;

    @Nullable
    protected LivingEntity target;
    @Nullable
    protected TargetEntry matchedEntry;

    /** 运行时重检冷却 */
    private int recheckCooldown = 0;

    public ZombieNearestAttackableTargetGoal(Mob mob, boolean mustSee, boolean mustReach) {
        this(mob, DEFAULT_RANDOM_INTERVAL, mustSee, mustReach);
    }

    public ZombieNearestAttackableTargetGoal(Mob mob, int randomInterval, boolean mustSee, boolean mustReach) {
        super(mob, mustSee, mustReach);
        this.randomInterval = reducedTickDelay(randomInterval);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    // ==================== 目标条目管理 ====================

    /**
     * 添加目标条目，指定优先度（数值越大越优先）。
     *
     * @param priority    优先度，越大越优先
     * @param targetClass 目标实体 Class
     * @param condition   TargetingConditions，可为 null
     */
    public void addTarget(int priority, Class<? extends LivingEntity> targetClass, @Nullable TargetingConditions condition) {
        this.targetEntries.add(new TargetEntry(priority, targetClass, condition));
        this.sorted = false;
    }

    /**
     * 添加目标条目，按添加顺序分配依次递减的默认优先度。
     */
    public void addTarget(Class<? extends LivingEntity> targetClass, @Nullable TargetingConditions condition) {
        this.addTarget(this.nextPriority++, targetClass, condition);
    }

    /**
     * 添加目标条目，无额外过滤条件。
     */
    public void addTarget(Class<? extends LivingEntity> targetClass) {
        this.addTarget(targetClass, null);
    }

    // ==================== Goal 生命周期 ====================

    @Override
    public boolean canUse() {
        if (this.randomInterval > 0 && this.mob.getRandom().nextInt(this.randomInterval) != 0) {
            return false;
        }

        // 按优先度排序（高优先度在前）
        if (!this.sorted) {
            this.targetEntries.sort(Comparator.<TargetEntry>comparingInt(e -> e.priority).reversed());
            this.sorted = true;
        }

        this.findTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null) return false;
        if (!livingentity.isAlive()) return false;
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        super.start();
        this.recheckCooldown = RECHECK_INTERVAL;
    }

    @Override
    public void stop() {
        this.target = null;
        this.matchedEntry = null;
        super.stop();
    }

    /**
     * 每 tick 执行：周期性检查更高优先度的目标。
     * 如果当前正在追逐低优先度目标（如村民），而高优先度目标（如玩家）进入范围，
     * 则立即切换目标，模拟原版多 Goal 的优先度效果。
     */
    @Override
    public void tick() {
        super.tick();

        if (--this.recheckCooldown > 0) return;
        this.recheckCooldown = RECHECK_INTERVAL;

        // 没有匹配条目或已经是最高优先度，无需重检
        if (this.matchedEntry == null) return;

        int currentPriority = this.matchedEntry.priority;
        LivingEntity betterTarget = this.findHigherPriorityTarget(currentPriority);

        if (betterTarget != null && betterTarget != this.mob.getTarget()) {
            this.target = betterTarget;
            this.mob.setTarget(this.target);
        }
    }

    /**
     * 仅扫描优先度高于指定值的条目，找到第一个最近的目标。
     * 用于运行时检查是否有更高优先度的目标出现。
     */
    @Nullable
    protected LivingEntity findHigherPriorityTarget(int higherThanPriority) {
        double followDist = this.getFollowDistance();

        for (TargetEntry entry : this.targetEntries) {
            if (entry.priority <= higherThanPriority) continue; // 只检查更高优先度

            TargetingConditions conditions = entry.condition;
            if (conditions == null) {
                conditions = TargetingConditions.forCombat().range(followDist);
            }

            if (!this.mustSee) {
                conditions.ignoreLineOfSight();  // ← 新增：允许穿墙索敌
            }

            LivingEntity found = this.scanEntry(entry, conditions, followDist);
            if (found != null) {
                this.matchedEntry = entry;
                return found;
            }
        }
        return null;
    }

    /**
     * 按优先度遍历所有目标条目，找到第一个最近的目标
     */
    protected void findTarget() {
        this.target = null;
        this.matchedEntry = null;

        double followDist = this.getFollowDistance();

        for (TargetEntry entry : this.targetEntries) {
            TargetingConditions conditions = entry.condition;
            if (conditions == null) {
                conditions = TargetingConditions.forCombat().range(followDist);
            }

            LivingEntity found = this.scanEntry(entry, conditions, followDist);
            if (found != null) {
                this.target = found;
                this.matchedEntry = entry;
                return;
            }
        }
    }

    /**
     * 扫描单个条目，返回最近的匹配实体
     */
    @Nullable
    private LivingEntity scanEntry(TargetEntry entry, TargetingConditions conditions, double followDist) {
        if (entry.targetClass == Player.class || entry.targetClass == ServerPlayer.class) {
            return this.mob.level().getNearestPlayer(
                    conditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        } else {
            AABB searchArea = this.getTargetSearchArea(followDist);
            return this.mob.level().getNearestEntity(
                    this.mob.level().getEntitiesOfClass(entry.targetClass, searchArea, e -> true),
                    conditions, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        }
    }

    /**
     * 获取目标搜索的 AABB 范围
     */
    protected AABB getTargetSearchArea(double distance) {
        return this.mob.getBoundingBox().inflate(distance, 4.0D, distance);
    }

    /**
     * 获取目标类型（匹配到的条目类型）
     */
    @Nullable
    public Class<? extends LivingEntity> getMatchedTargetClass() {
        return this.matchedEntry != null ? this.matchedEntry.targetClass : null;
    }

    // ==================== 内部数据结构 ====================

    protected record TargetEntry(int priority,
                                                       Class<? extends LivingEntity> targetClass,
                                                       @Nullable TargetingConditions condition) {
    }
}
