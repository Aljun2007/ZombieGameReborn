package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;

public class ClearHeadBlockGoal extends Goal {
    private final Zombie zombie;
    private ZombieBreakBlockGoal breakBlockGoal;
    private boolean tried = false;
    private BlockPos targetBlockPos;

    public ClearHeadBlockGoal(Zombie zombie) {
        this.zombie = zombie;
        //this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 如果僵尸死了或者没有窒息，不使用这个Goal
        if (this.zombie == null || !this.zombie.isAlive()) {
            return false;
        }

        // 检查僵尸是否窒息（头部在方块内）
        if (!this.isZombieSuffocating()) {
            return false;
        }

        // 检查是否可以破坏方块
        this.tryToFindBreakGoal();
        if (this.breakBlockGoal == null) {
            return false;
        }

        // 获取头部位置的方块
        BlockPos headPos = this.zombie.blockPosition().above();
        BlockState headBlock = this.zombie.level().getBlockState(headPos);

        // 如果头部已经是空气或液体，不需要破坏
        if (headBlock.isAir() || !headBlock.getFluidState().isEmpty()) {
            return false;
        }

        // 检查这个方块是否可以破坏
        if (this.zombie.level().getBlockState(headPos).getDestroySpeed(this.zombie.level(), headPos) < 0) {
            return false; // 基岩等不可破坏方块
        }

        this.targetBlockPos = headPos;
        return true;
    }

    @Override
    public void stop() {
        this.targetBlockPos = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // 如果目标方块存在且不是空气，尝试破坏
        if (this.targetBlockPos != null) {
            BlockState currentState = this.zombie.level().getBlockState(this.targetBlockPos);
            if (!currentState.isAir()) {
                this.tryToBreakHeadBlock();
            }
        }
    }

    /**
     * 尝试破坏头部方块
     */
    private void tryToBreakHeadBlock() {
        if (this.targetBlockPos == null) {
            return;
        }

        // 检查目标方块是否还存在
        BlockState targetState = this.zombie.level().getBlockState(this.targetBlockPos);
        if (targetState.isAir()) {
            return;
        }
        this.breakBlockGoal.tryToBreak(this.targetBlockPos);
    }


    /**
     * 检查僵尸是否窒息（头部在固体方块中）
     */
    private boolean isZombieSuffocating() {
        if (this.zombie == null) {
            return false;
        }

        // 检查僵尸头部位置
        BlockPos headPos = this.zombie.blockPosition().above();
        BlockState headBlock = this.zombie.level().getBlockState(headPos);

        // 如果头部是固体方块（非空气、非流体），说明正在窒息
        return !headBlock.isAir() && headBlock.getFluidState().isEmpty()
                && headBlock.isSolidRender(this.zombie.level(), headPos);
    }

    private void tryToFindBreakGoal() {
        if (!this.tried) {
            this.breakBlockGoal = ZGRZombieControlAPI.getBreakPlaceGoal(this.zombie);
            this.tried = true;
        }
    }
}
