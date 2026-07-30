package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Zombie;

public class DrownedWaterBridgeBuildGoal extends ZombieWaterBridgeBuildGoal {
    public DrownedWaterBridgeBuildGoal(Zombie zombie, IZombieData data) {
        super(zombie, data);
    }

    @Override
    public boolean canUse() {
        if (this.zombie.getNavigation() instanceof GroundPathNavigation) {
            return super.canUse();
        } else return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.zombie.getNavigation() instanceof GroundPathNavigation) {
            return super.canContinueToUse();
        } else return false;
    }
}
