package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;

public class EnhancedVanillaType  extends ZombieType {

    public EnhancedVanillaType() {
        super(ZGRZombieTypes.IDs.ENHANCED_VANILLA_ID);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie) {
        ZombieType.replaceGoal(zombie.goalSelector,goal-> goal instanceof ZombieAttackGoal, ()->new EnhancedZombieAttackGoal(zombie),2);
    }
}
