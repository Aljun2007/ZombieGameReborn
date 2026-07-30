package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import com.aljun.zombiegamereborn.utils.MathUtils;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * 僵尸空闲时破坏光源方块的 Goal
 * <p>
 * canUse 中全量扫描找到目标 → start 开始走向 → tick 破坏 → 破坏完成自动 stop。
 */
public class ZombieRemoveLightSourceGoal extends Goal {

    private static final int SCAN_RADIUS = 5;
    private static final int SCAN_SIZE = SCAN_RADIUS * 2 + 1;              // 7
    private static final int SCAN_Y_START = -1;
    private static final int SCAN_Y_END = 2;
    private static final int SCAN_Y_RANGE = SCAN_Y_END - SCAN_Y_START + 1; // 4
    private static final int SCAN_TOTAL = SCAN_SIZE * SCAN_SIZE * SCAN_Y_RANGE; // 196
    private static final long SCAN_COOLDOWN = 20;

    private final Zombie zombie;
    private final IZombieData data;

    /** 当前要破坏的目标光源方块，null 表示空闲 */
    private BlockPos targetPos = null;
    /** 上次全量扫描时间 */
    private long lastScanTime = 0;

    private boolean positionVerification(BlockPos pos) {
        return this.zombie.getEyePosition().distanceToSqr(MathUtils.blockPosToVec3(pos)) <= ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR;
    }

    public ZombieRemoveLightSourceGoal(Zombie zombie, IZombieData data) {
        this.zombie = zombie;
        this.data = data;
        //this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    // ==================== canUse / canContinueToUse ====================

    @Override
    public boolean canUse() {
        // 已有目标则直接接手
        if (this.targetPos != null) return true;

        // 配置检查
        if (!ZGRGame.getGameProperty().getStageProperty(
                (ServerLevel) this.zombie.level(), this.zombie.blockPosition())
                .zombieProperty.breakLightSources)
            return false;

        // 前置条件
        if (this.zombie.getTarget() != null) return false;
        if (!this.zombie.isAlive()) return false;
        if (!this.data.isEmpowered()) return false;
        if (this.data.getZombieBreakBlockGoal() == null) return false;

        // 冷却
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime - this.lastScanTime < SCAN_COOLDOWN) return false;
        this.lastScanTime = gameTime;

        // 全量扫描寻找目标
        this.targetPos = this.findTargetBlock();
        return this.targetPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPos != null;
    }

    // ==================== start / stop ====================

    @Override
    public void start() {
        if (this.targetPos != null) {
            Path path = this.zombie.getNavigation().createPath(this.targetPos, 0);
            if (path != null) {
                this.zombie.getNavigation().moveTo(path, 1.0);
            }
        }
    }

    @Override
    public void stop() {
        this.targetPos = null;
        ZombieBreakBlockGoal breakGoal = this.data.getZombieBreakBlockGoal();
        if (breakGoal != null && !breakGoal.isDone()) {
            breakGoal.stopBreak();
        }
        this.zombie.getNavigation().stop();
    }

    // ==================== tick ====================

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.targetPos == null) {
            if (this.zombie.getTarget() != null) {
                ZombieBreakBlockGoal breakGoal = this.data.getZombieBreakBlockGoal();
                if (breakGoal != null && breakGoal.isDone()) {
                    BlockPos found = this.findTargetBlock(3);
                    if (found != null) {
                        breakGoal.tryToBreak(found);
                    }
                }
            }
            return;
        }

        ZombieBreakBlockGoal breakGoal = this.data.getZombieBreakBlockGoal();
        if (breakGoal == null) {
            this.targetPos = null;
            return;
        }

        double distSqr = this.zombie.blockPosition().distSqr(this.targetPos);

        if (positionVerification(this.targetPos)) {
            // 距离足够，尝试破坏
            if (breakGoal.isDone()) {
                breakGoal.tryToBreak(this.targetPos);
            }

            // 如果已破坏完成（瞬间破坏或挖掘结束），清除目标
            if (breakGoal.isDone()) {
                this.targetPos = null;
                this.zombie.getNavigation().stop();
            }
        } else {
            // 距离不够，继续寻路
            if (this.zombie.getNavigation().isDone()) {
                Path path = this.zombie.getNavigation().createPath(this.targetPos, 0);
                if (path != null) {
                    this.zombie.getNavigation().moveTo(path, 1.0);
                } else {
                    this.targetPos = null;
                }
            }
        }
    }

    // ==================== 方块查找 ====================

    /**
     * 全量扫描周围 5 格内所有方块，返回第一个符合条件的光源方块
     */
    private BlockPos findTargetBlock() {
        return findTargetBlock(SCAN_RADIUS);
    }

    /**
     * 全量扫描周围指定半径内所有方块，返回第一个符合条件的光源方块
     */
    private BlockPos findTargetBlock(int radius) {
        int size = radius * 2 + 1;
        int total = size * size * SCAN_Y_RANGE;

        BlockPos feetPos = this.zombie.blockPosition();
        int startOffset = this.zombie.getRandom().nextInt(total);

        for (int j = 0; j < total; j++) {
            int index = (startOffset + j) % total;

            int dx = (index % size) - radius;
            int dz = ((index / size) % size) - radius;
            int dy = (index / (size * size)) + SCAN_Y_START;

            BlockPos checkPos = feetPos.offset(dx, dy, dz);

            if (checkPos.equals(feetPos)) continue;
            if (this.zombie.level().isOutsideBuildHeight(checkPos)) continue;

            BlockState state = this.zombie.level().getBlockState(checkPos);
            if (!this.isBreakable(state, checkPos)) continue;
            if (!this.isZombieDesiredBlock(state)) continue;

            return checkPos;
        }

        return null;
    }

    // ==================== 方块判定 ====================

    private boolean isBreakable(BlockState state, BlockPos pos) {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        float hardness = state.getDestroySpeed(this.zombie.level(), pos);
        return hardness >= 0;
    }

    private boolean isZombieDesiredBlock(BlockState state) {
        return state.getBlock() instanceof TorchBlock
                || state.getBlock() instanceof CarvedPumpkinBlock
                || state.getBlock() instanceof LanternBlock
                || state.getBlock() instanceof CandleBlock;
    }
}
