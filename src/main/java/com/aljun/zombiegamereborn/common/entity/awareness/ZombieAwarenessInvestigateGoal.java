package com.aljun.zombiegamereborn.common.entity.awareness;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumSet;

/**
 * 感知 → 去调查某个<b>位置</b>。
 *
 * <p>这是与旧实现最本质的区别。旧实现只记录“谁发出了声音”，于是僵尸只能去追那个实体；
 * 一旦玩家跑掉、躲起来或死亡，兴趣点就没了。这里记录的是位置，所以僵尸会：
 * 走到声音传来的地方、顺着气味最浓的方向摸过去，到了之后原地找一圈——
 * 也就是“僵尸意识”那种“它们知道你去哪了”的感觉。
 *
 * <p>两种模式：
 * <ul>
 *   <li><b>定点</b>：声音/冲击/警报 → 走向刺激坐标，到达后观察一段时间；</li>
 *   <li><b>追踪</b>：气味 → 每次重寻路时比较周围 10 格的气味浓度，往更浓的一格走。</li>
 * </ul>
 *
 * <p>开销控制：每次重新寻路有最小间隔（{@code investigateRepathIntervalTicks}），
 * 并且只有“当前没有攻击目标”的僵尸才会进入调查，避免和战斗逻辑抢寻路。
 */
public class ZombieAwarenessInvestigateGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Zombie zombie;
    private final int phaseSeed;

    /** 当前调查是由哪个通道触发的，只用于调试日志。 */
    @Nullable
    private AwarenessChannel channel;

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean arrived;
    private boolean scentMode;
    private long investigateUntil;
    private long lookAroundUntil;
    private long lastRepathTick;
    /** 调试用：上次打印“推进中”日志的 tick。 */
    private long lastProgressLogTick;
    /** 上次寻路的目标格，只有换格才重新寻路。 */
    private long lastPathCell = Long.MIN_VALUE;
    /** 连续“路径走完但没到达”的 tick 数，用于放弃不可达目标。 */
    private int stuckTicks;

    public ZombieAwarenessInvestigateGoal(Zombie zombie) {
        this.zombie = zombie;
        this.phaseSeed = zombie.getId();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!AwarenessTuning.enabled()) {
            return false;
        }
        long now = this.zombie.level().getGameTime();
        if (Math.floorMod(now + this.phaseSeed, AwarenessTuning.pollIntervalTicks()) != 0) {
            return false;
        }
        // 有攻击目标的僵尸正在打架，不参与调查
        if (this.zombie.getTarget() != null) {
            return false;
        }
        LevelAwareness awareness = AwarenessManager.of(this.zombie.level());
        if (awareness == null) {
            return false;
        }

        // 1) 先看附近有没有定点刺激（声音 / 冲击 / 警报 / 光照）
        Stimulus stimulus = awareness.findStrongest(this.zombie.getX(), this.zombie.getY(),
                this.zombie.getZ(), now, AwarenessManager.MASK_SOUND | AwarenessManager.MASK_IMPACT
                        | AwarenessManager.MASK_ALERT | AwarenessManager.MASK_LIGHT);
        if (stimulus != null) {
            this.targetX = stimulus.x;
            this.targetY = stimulus.y;
            this.targetZ = stimulus.z;
            this.scentMode = false;
            this.channel = stimulus.channel;
            return true;
        }

        // 2) 再看有没有气味轨迹可以跟
        AwarenessSettings settings = AwarenessTuning.settings();
        ScentGrid.ScentCell cell = awareness.scent().bestNeighbour(this.zombie.getX(), this.zombie.getY(),
                this.zombie.getZ(), now);
        if (cell != null && this.zombie.distanceToSqr(cell.centerX(), cell.centerY(), cell.centerZ())
                <= settings.scentFollowRadius * settings.scentFollowRadius) {
            this.targetX = cell.centerX();
            this.targetY = cell.centerY();
            this.targetZ = cell.centerZ();
            this.scentMode = true;
            this.channel = AwarenessChannel.SCENT;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        AwarenessSettings settings = AwarenessTuning.settings();
        long now = this.zombie.level().getGameTime();
        this.arrived = false;
        this.lookAroundUntil = 0L;
        this.investigateUntil = now + settings.investigateTimeoutTicks;
        this.lastRepathTick = 0L;
        this.lastPathCell = Long.MIN_VALUE;
        this.stuckTicks = 0;
        this.repath(now, true);
        if (AwarenessTuning.debugLog()) {
            LOGGER.info("[ZGR awareness] zombie#{} starts investigating {} -> target {} | zombie {}",
                    this.zombie.getId(), this.channel, fmt(this.targetX, this.targetY, this.targetZ),
                    fmt(this.zombie.getX(), this.zombie.getY(), this.zombie.getZ()));
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.zombie.getTarget() != null) {
            return false;       // 战斗中途中发现了目标，交给战斗 Goal
        }
        long now = this.zombie.level().getGameTime();
        if (now >= this.investigateUntil) {
            return false;       // 超时放弃，避免僵尸被一个旧声音永久牵走
        }
        if (this.lookAroundUntil > 0L) {
            return now < this.lookAroundUntil;
        }
        return true;
    }

    @Override
    public void tick() {
        long now = this.zombie.level().getGameTime();

        if (AwarenessTuning.debugLog() && now - this.lastProgressLogTick >= 40L) {
            this.lastProgressLogTick = now;
            LOGGER.info("[ZGR awareness] zombie#{} investigating {} | zombie {} | distance={} | navDone={} hasPath={} delta={} noAi={}",
                    this.zombie.getId(), this.channel,
                    fmt(this.zombie.getX(), this.zombie.getY(), this.zombie.getZ()),
                    String.format("%.1f", Math.sqrt(
                            this.zombie.distanceToSqr(this.targetX, this.targetY, this.targetZ))),
                    this.zombie.getNavigation().isDone(),
                    this.zombie.getNavigation().getPath() != null,
                    String.format("%.4f", this.zombie.getDeltaMovement().horizontalDistance()),
                    this.zombie.isNoAi());
        }

        if (this.arrived) {
            // 到达后原地环视，模拟“找了一圈”
            this.zombie.getLookControl().setLookAt(
                    this.zombie.getX() + (this.zombie.getRandom().nextDouble() - 0.5D) * 8.0D,
                    this.zombie.getY() + this.zombie.getRandom().nextDouble() * 3.0D,
                    this.zombie.getZ() + (this.zombie.getRandom().nextDouble() - 0.5D) * 8.0D);
            return;
        }

        // 注意：distanceToSqr 返回的是平方距离，必须和半径的平方比较。
        // 之前误与原始半径比较（等价于要求实际距离 ≤ 1.58 格），导致"到达后环视"从不触发。
        double arriveDistance = AwarenessTuning.settings().investigateArriveDistance;
        if (this.zombie.distanceToSqr(this.targetX, this.targetY, this.targetZ)
                <= arriveDistance * arriveDistance) {
            this.arrived = true;
            this.lookAroundUntil = now + AwarenessTuning.settings().investigateLookAroundTicks;
            // 保证环视能完整进行：到达得晚时，环视不能被总超时提前掐断
            this.investigateUntil = Math.max(this.investigateUntil, this.lookAroundUntil);
            this.zombie.getNavigation().stop();
            if (AwarenessTuning.debugLog()) {
                LOGGER.info("[ZGR awareness] zombie#{} ARRIVED at target {} (channel {})",
                        this.zombie.getId(), fmt(this.targetX, this.targetY, this.targetZ), this.channel);
            }
            return;
        }

        // 路径已经走完【且确实没在动】（目标不可达，比如声音来自墙里/地底）：
        // 连续卡住够久就放弃，别把 MOVE 一直攥着让僵尸站着发呆。
        // 必须带“没在动”这个条件：正常接近途中路径也会短暂走完，此时僵尸仍在移动，不能算卡住。
        boolean notMoving = this.zombie.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
        if (this.zombie.getNavigation().isDone() && notMoving) {
            if (++this.stuckTicks >= AwarenessTuning.settings().investigateGiveUpTicks) {
                this.investigateUntil = now;
                return;
            }
        } else {
            this.stuckTicks = 0;
        }

        if (this.scentMode) {
            this.followScent(now);
        } else {
            this.repath(now, false);
        }
    }

    @Override
    public void stop() {
        if (AwarenessTuning.debugLog()) {
            LOGGER.info("[ZGR awareness] zombie#{} STOPPED investigating {} at {} | target was {}",
                    this.zombie.getId(), this.channel,
                    fmt(this.zombie.getX(), this.zombie.getY(), this.zombie.getZ()),
                    fmt(this.targetX, this.targetY, this.targetZ));
        }
        this.zombie.getNavigation().stop();
        this.scentMode = false;
        this.arrived = false;
        this.lookAroundUntil = 0L;
        this.lastPathCell = Long.MIN_VALUE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    // ------------------------------------------------------------------

    /** 定点模式：仅在“目标格变了”或“超过重寻路间隔”时才真的调用寻路。 */
    private void repath(long now, boolean force) {
        long cell = packCell(this.targetX, this.targetY, this.targetZ);
        if (!force && cell == this.lastPathCell
                && now - this.lastRepathTick < AwarenessTuning.settings().investigateRepathIntervalTicks) {
            return;
        }
        this.lastPathCell = cell;
        this.lastRepathTick = now;
        this.zombie.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ,
                AwarenessTuning.settings().investigateSpeed);
    }

    /** 追踪模式：沿气味梯度走，天然不会产生“每 tick 重新寻路”的风暴。 */
    private void followScent(long now) {
        if (now - this.lastRepathTick < AwarenessTuning.settings().investigateRepathIntervalTicks) {
            return;
        }
        this.lastRepathTick = now;
        LevelAwareness awareness = AwarenessManager.of(this.zombie.level());
        if (awareness == null) {
            return;
        }
        ScentGrid.ScentCell next = awareness.scent().bestNeighbour(this.zombie.getX(), this.zombie.getY(),
                this.zombie.getZ(), now);
        if (next == null) {
            // 气味断了：把最后闻到的位置当作调查点，过去看一眼
            this.scentMode = false;
            this.repath(now, true);
            return;
        }
        this.targetX = next.centerX();
        this.targetY = next.centerY();
        this.targetZ = next.centerZ();
        this.zombie.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ,
                AwarenessTuning.settings().investigateSpeed);
    }

    private static long packCell(double x, double y, double z) {
        return net.minecraft.core.BlockPos.asLong((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    private static String fmt(double x, double y, double z) {
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
    }
}
