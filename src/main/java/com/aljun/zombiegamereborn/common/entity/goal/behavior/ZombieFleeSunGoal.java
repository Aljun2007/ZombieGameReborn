package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.common.entity.accessor.IZombieAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class ZombieFleeSunGoal extends Goal {
    private static final float DEFAULT_WATER_MALUS = -1.0F;

    private final Zombie zombie;
    private Vec3 targetPos;
    private boolean seekingWater;

    public ZombieFleeSunGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!((IZombieAccessor) this.zombie).zgr_invokeIsSunSensitive()) {
            return false;
        }
        if (this.zombie.getTarget() != null) return false;
        if (!this.zombie.level().isDay()) return false;
        if (!this.zombie.isOnFire()) return false;
        if (!this.zombie.level().canSeeSky(this.zombie.blockPosition())) return false;
        if (!this.zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return false;
        return this.findTargetPos();
    }

    @Override
    public boolean canContinueToUse() {
        if (!((IZombieAccessor) this.zombie).zgr_invokeIsSunSensitive()) {
            return false;
        }
        return !this.zombie.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.targetPos != null) {
            this.zombie.getNavigation().moveTo(
                    this.targetPos.x, this.targetPos.y, this.targetPos.z, 1.0d);
        }
    }

    @Override
    public void stop() {
        if (this.seekingWater) {
            this.zombie.setPathfindingMalus(BlockPathTypes.WATER, DEFAULT_WATER_MALUS);
            this.seekingWater = false;
        }
        this.targetPos = null;
    }

    private boolean findTargetPos() {
        // 着火时优先找水
        if (this.zombie.isOnFire() && !this.zombie.isInWater()) {
            Vec3 waterPos = this.findNearbyWater();
            if (waterPos != null) {
                this.targetPos = waterPos;
                this.zombie.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
                this.seekingWater = true;
                return true;
            }
        }
        // 找不到水则找遮荫处
        Vec3 shadePos = this.findShadePos();
        if (shadePos != null) {
            this.targetPos = shadePos;
            return true;
        }
        return false;
    }

    private Vec3 findNearbyWater() {
        Level level = this.zombie.level();
        RandomSource random = this.zombie.getRandom();
        BlockPos pos = this.zombie.blockPosition();

        for (int i = 0; i < 15; ++i) {
            BlockPos targetPos = pos.offset(
                    random.nextInt(14) - 7,
                    random.nextInt(4) - 2,
                    random.nextInt(14) - 7
            );
            if (level.getBlockState(targetPos).getFluidState().is(FluidTags.WATER)) {
                return Vec3.atBottomCenterOf(targetPos);
            }
        }
        return null;
    }

    private Vec3 findShadePos() {
        Level level = this.zombie.level();
        RandomSource random = this.zombie.getRandom();
        BlockPos pos = this.zombie.blockPosition();

        for (int i = 0; i < 10; ++i) {
            BlockPos targetPos = pos.offset(
                    random.nextInt(20) - 10,
                    random.nextInt(6) - 3,
                    random.nextInt(20) - 10
            );
            if (!level.canSeeSky(targetPos) && this.zombie.getWalkTargetValue(targetPos) < 0.0F) {
                return Vec3.atBottomCenterOf(targetPos);
            }
        }
        return null;
    }
}
