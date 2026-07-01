package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieMeleeAndPathBuildGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class ZombieWaterBridgeBuildGoal extends Goal {

    private static final int PLACE_COOLDOWN = 5;
    private static final int BRIDGE_BUILD_COOLDOWN = 40;
    private static final int FAIL_COOLDOWN = 80;

    private final Zombie zombie;
    private final IZombieData data;
    private long lastPlaceTime = 0;
    private long lastFailTime = 0;
    private Boolean pathBuildGoalChecked = false;
    private ZombieMeleeAndPathBuildGoal pathBuildGoal = null;

    public ZombieWaterBridgeBuildGoal(Zombie zombie) {
        this.zombie = zombie;
        this.data = ZGRZombieAttributesAPI.getZombieData(zombie);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean isPathBuildCooldown() {
        return this.zombie.level().getGameTime() < this.lastPlaceTime + BRIDGE_BUILD_COOLDOWN;
    }

    @Override
    public boolean canUse() {
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime < this.lastFailTime + FAIL_COOLDOWN) return false;

        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!ZGRGame.Rules.canZombiePlaceBlock(zombie.getServer())) return false;

        this.tryGetPathBuildGoal();
        if (this.pathBuildGoal != null && this.pathBuildGoal.isInBuildState()) return false;


        return this.chooseBlockPos() != null;
    }

    private void tryGetPathBuildGoal() {
        if (!this.pathBuildGoalChecked) {
            this.pathBuildGoal = (ZombieMeleeAndPathBuildGoal) ZGRZombieControlAPI.getGoal(
                    this.zombie, goal -> goal instanceof ZombieMeleeAndPathBuildGoal);
            this.pathBuildGoalChecked = true;
        }
    }

    @Nullable
    private BlockPos chooseBlockPos() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) return null;

        BlockPos targetPos = target.blockPosition();
        BlockPos pos = this.zombie.blockPosition();

        pos = pos.below();
        if (isFluidPos(pos)) return pos;

        pos = pos.above();
        for (int i = 1; i <= 3; i++) {
            Direction dir = getDirection(pos, targetPos);
            pos = pos.relative(dir);
            if (isFluidPos(pos)) return pos;
        }

        pos = zombie.blockPosition().below();
        for (int i = 1; i <= 3; i++) {
            Direction dir = getDirection(pos, targetPos);
            pos = pos.relative(dir);
            if (isFluidPos(pos)) return pos;
        }

        pos = zombie.blockPosition().above();
        for (int i = 1; i <= 3; i++) {
            Direction dir = getDirection(pos, targetPos);
            pos = pos.relative(dir);
            if (isFluidPos(pos)) return pos;
        }

        pos = zombie.blockPosition().above(2);
        for (int i = 1; i <= 3; i++) {
            Direction dir = getDirection(pos, targetPos);
            pos = pos.relative(dir);
            if (isFluidPos(pos)) return pos;
        }

        return null;
    }

    private boolean isFluidPos(BlockPos pos) {
        if (this.zombie.level().isOutsideBuildHeight(pos)) return false;
        BlockState blockState = this.zombie.level().getBlockState(pos);
        return !blockState.getFluidState().is(Fluids.EMPTY) && !blockState.isAir();
    }

    private static Direction getDirection(BlockPos selfPos, BlockPos targetPos) {
        double x = selfPos.getX() - targetPos.getX();
        double z = selfPos.getZ() - targetPos.getZ();
        if (z <= x && z < -x) {
            return Direction.SOUTH;
        } else if (z > x && z <= -x) {
            return Direction.EAST;
        } else if (z >= x && z > -x) {
            return Direction.NORTH;
        } else {
            return Direction.WEST;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.zombie.getTarget() != null && this.zombie.getTarget().isAlive();
    }

    @Override
    public void stop() {
        this.data.setMovementSpeedModify(1.0d);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.zombie.level().getGameTime() - this.lastPlaceTime < PLACE_COOLDOWN) return;

        BlockPos blockPos = this.chooseBlockPos();
        if (blockPos == null) return;


        if (!ZGRZombieControlAPI.startPlaceBlock(this.zombie, blockPos)) {
            this.lastFailTime = this.zombie.level().getGameTime();
            return;
        }
        this.lastPlaceTime = this.zombie.level().getGameTime();
        if (!this.zombie.isSwimming()) {
            this.zombie.swing(InteractionHand.OFF_HAND);
        }
    }
}
