package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.drowned.EnhancedDrownedAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


public class DrownedTridentAttackType extends ZombieType {
    public DrownedTridentAttackType() {
        super(ZGRZombieTypes.IDs.TRIDENT_DROWNED_ID);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie, IZombieData data) {
        if (zombie instanceof Drowned drowned) {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new EnhancedDrownedAttackGoal(drowned), 2);
        } else {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new EnhancedZombieAttackGoal(zombie), 2);
        }
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        super.onInitializeZombieEquipment(zombie, data);
        zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));
    }
}
