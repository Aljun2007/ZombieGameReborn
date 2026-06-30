package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;

public class ZombieFloatGoal extends FloatGoal {
    private final Mob mob;

    public ZombieFloatGoal(Mob mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget()!=null) {
            if (this.mob.getTarget().getEyeY()>=this.mob.getEyeY()) {
                return false;
            }
        }
        return super.canUse();
    }
}
