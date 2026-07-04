package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieShieldAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ShieldUserType extends ZombieType {

    public ShieldUserType() {
        super(ZGRZombieTypes.IDs.SHIELD_USER_ID);
    }

    @Override
    public void onInitializeZombieWeaponsAndArmors(Zombie zombie, IZombieData data) {
        zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieShieldGoal shieldUsingGoal = new ZombieShieldGoal(zombie,data);
        zombie.goalSelector.addGoal(1, shieldUsingGoal);
        data.setZombieShieldGoal(shieldUsingGoal);
        ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal,
                () -> new ZombieShieldAttackGoal(zombie, shieldUsingGoal), 2);
        zombie.goalSelector.addGoal(4, new EnhancedZombieAttackGoal(zombie));
    }
}
