package com.aljun.zombiegamereborn.common.entity.goal.attack.drowned;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieMeleeAndPathBuildGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;

public class DrownedMeleeAndPathBuildGoal extends ZombieMeleeAndPathBuildGoal {
    private final Drowned drowned;

    public DrownedMeleeAndPathBuildGoal(Drowned drowned, IZombieData data) {
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
