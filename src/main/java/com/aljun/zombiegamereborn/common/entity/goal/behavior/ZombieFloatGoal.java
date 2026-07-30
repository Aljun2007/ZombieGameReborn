package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public class ZombieFloatGoal extends FloatGoal {
    private final Mob mob;
    private boolean waterMalusSet;

    public ZombieFloatGoal(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        // 如果目标在上方，才触发上浮
        if (this.mob.getTarget() != null) {
            if (this.mob.getTarget().getEyeY() <= this.mob.getEyeY()) {
                return false;
            }
        }
        return super.canUse();
    }

    @Override
    public void start() {
        if (!this.waterMalusSet) {
            this.mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
            this.waterMalusSet = true;
        }
    }

    @Override
    public void stop() {
        super.stop();
        // 离开水时恢复寻路代价（可选）
        if (this.waterMalusSet) {
            this.mob.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
            this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1.0F);
            this.waterMalusSet = false;
        }
    }
}