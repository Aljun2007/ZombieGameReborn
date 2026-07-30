package com.aljun.zombiegamereborn.common.entity.goal.attack.drowned;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieSmartBreakAttackGoal;
import net.minecraft.world.entity.monster.Drowned;

public class DrownedSmartBreakAttackGoal extends ZombieSmartBreakAttackGoal {
    private final Drowned drowned;

    public DrownedSmartBreakAttackGoal(Drowned drowned, IZombieData data) {
        super(drowned,data);
        this.drowned = drowned;
    }

    private boolean okTarget() {
        return this.drowned.okTarget(this.drowned.getTarget());
    }

    public boolean canUse() {
        return super.canUse() && this.okTarget();
    }

    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.okTarget();
    }
}
