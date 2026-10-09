package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieTNTAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TNTAttackType extends ZombieType {

    public TNTAttackType() {
        super(ZGRZombieTypes.IDs.TNT_ATTACKER_ID);
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        super.onInitializeZombieEquipment(zombie, data);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TNT,3));
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal,
                () -> new ZombieTNTAttackGoal(zombie, data), 2);
    }

}
