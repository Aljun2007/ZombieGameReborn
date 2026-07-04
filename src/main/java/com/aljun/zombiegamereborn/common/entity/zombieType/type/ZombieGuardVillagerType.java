package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieBowAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieCrossbowAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieShieldAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieGuardVillagerType extends ZombieType {

    public ZombieGuardVillagerType() {
        super(ZGRZombieTypes.IDs.ZOMBIE_GUARD_VILLAGER);
    }

    @Override
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
        data.setZombieContinueUseWeaponsInHand(ZGRGame.getGameProperty().getStageProperty(zombie.getServer()).zombieProperty.canZombieGuardContinueUseWeapons);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie, IZombieData data) {

        if (data.canZombieContinueUseWeaponsInHand()) {
            ZombieShieldGoal shieldUsingGoal = new ZombieShieldGoal(zombie, data);
            zombie.goalSelector.addGoal(1, shieldUsingGoal);
            zombie.goalSelector.addGoal(2, new ZombieBowAttackGoal(zombie));
            zombie.goalSelector.addGoal(2, new ZombieCrossbowAttackGoal(zombie));
            zombie.goalSelector.addGoal(2, ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.createGunnerGoal(zombie));
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal,() -> new ZombieShieldAttackGoal(zombie, shieldUsingGoal), 4);

        } else {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal,
                    () -> new EnhancedZombieAttackGoal(zombie), 2);
        }

    }

    @Override
    public boolean onlyMelee() {
        return false;
    }
}
