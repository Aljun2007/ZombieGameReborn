package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.accessor.IZombieAccessor;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieRestrictSunGoal extends RestrictSunGoal {
    private final Zombie zombie;

    public ZombieRestrictSunGoal(Zombie zombie) {
        super(zombie);
        this.zombie = zombie;
    }

    @SuppressWarnings("all")
    @Override
    public boolean canUse() {
        if (!((IZombieAccessor) this.zombie).zgr_invokeIsSunSensitive()) {
            return false;
        }
        if (this.zombie.getTarget() != null) {
            double r = ZGRZombieAttributesAPI.getFollowRange(zombie) / 2;
            if (zombie.distanceToSqr(zombie.getTarget()) <= r * r) return false;
        }
        return super.canUse();
    }

}
