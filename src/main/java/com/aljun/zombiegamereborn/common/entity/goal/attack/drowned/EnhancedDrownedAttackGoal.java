package com.aljun.zombiegamereborn.common.entity.goal.attack.drowned;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import net.minecraft.world.entity.monster.Drowned;

public class EnhancedDrownedAttackGoal extends EnhancedZombieAttackGoal {
    private final Drowned drowned;

    public EnhancedDrownedAttackGoal(Drowned drowned) {
        super(drowned);
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
