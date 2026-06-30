package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class ZombieWaterBridgeBuildGoal extends Goal {

    private static final int PLACE_COOLDOWN = 12;
    private static final int SLOW_DURATION = 30;
    private static final double SLOW_MODIFIER = 4.0;

    private static final int[] SEARCH_Y_OFFSETS = {-1, 0, 1, 2};
    private static final int SEARCH_RANGE = 3;

    private final Zombie zombie;
    private final IZombieData data;
    private long lastPlaceTime = 0;
    private int slowTicksRemaining = 0;

    public ZombieWaterBridgeBuildGoal(Zombie zombie) {
        this.zombie = zombie;
        this.data = ZGRZombieAttributesAPI.getZombieData(zombie);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!ZGRGame.Rules.canZombiePlaceBlock(zombie.getServer())) return false;
        if (!this.zombie.isInWater() && !this.zombie.isInLava() && !isFluidAt(this.zombie.blockPosition())) return false;
        return this.findPlacePos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.zombie.getTarget() != null
                && this.zombie.getTarget().isAlive()
                && ZGRGame.Rules.canZombiePlaceBlock(zombie.getServer());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.decaySlow();

        if (this.zombie.level().getGameTime() - this.lastPlaceTime < PLACE_COOLDOWN) return;

        BlockPos placePos = this.findPlacePos();
        if (placePos == null) return;

        this.lastPlaceTime = this.zombie.level().getGameTime();
        ZGRZombieControlAPI.startPlaceBlock(this.zombie, placePos);
        if (!this.zombie.isSwimming()) {
            this.zombie.swing(InteractionHand.OFF_HAND);
        }
        this.applySlow();
    }

    @Override
    public void stop() {
        this.restoreSpeed();
    }

    private void decaySlow() {
        if (this.slowTicksRemaining > 0 && --this.slowTicksRemaining <= 0) {
            this.restoreSpeed();
        }
    }

    private void applySlow() {
        this.data.setMovementSpeedModify(SLOW_MODIFIER);
        this.slowTicksRemaining = SLOW_DURATION;
    }

    private void restoreSpeed() {
        this.data.setMovementSpeedModify(1.0d);
        this.slowTicksRemaining = 0;
    }

    @Nullable
    private BlockPos findPlacePos() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) return null;

        BlockPos targetPos = target.blockPosition();

        for (int yOffset : SEARCH_Y_OFFSETS) {
            BlockPos base = this.zombie.blockPosition().above(yOffset);
            if (isFluidAt(base)) return base;

            Direction dir = getDirection(base, targetPos);
            for (int i = 1; i <= SEARCH_RANGE; i++) {
                BlockPos pos = base.relative(dir, i);
                if (isFluidAt(pos)) return pos;
            }
        }

        return null;
    }

    private boolean isFluidAt(BlockPos pos) {
        if (this.zombie.level().isOutsideBuildHeight(pos)) return false;
        FluidState state = this.zombie.level().getFluidState(pos);
        return !state.is(Fluids.EMPTY);
    }

    private static Direction getDirection(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }
}
