package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieCrossbowAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CrossbowAttackerType extends ZombieType {

    public CrossbowAttackerType() {
        super(ZGRZombieTypes.IDs.CROSSBOW_ATTACKER_ID);
    }

    @Override
    public void onInitializeZombieWeaponsAndArmors(Zombie zombie, IZombieData data) {
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal,
                () -> new ZombieCrossbowAttackGoal(zombie), 2);
        zombie.goalSelector.addGoal(4, new EnhancedZombieAttackGoal(zombie));
    }
}
