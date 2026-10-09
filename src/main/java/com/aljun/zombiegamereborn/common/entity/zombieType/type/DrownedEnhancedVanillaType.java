package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.drowned.EnhancedDrownedAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DrownedEnhancedVanillaType extends ZombieType {
    public DrownedEnhancedVanillaType() {
        super(ZGRZombieTypes.IDs.ENHANCED_DROWNED_ID);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie, IZombieData data) {
        if (zombie instanceof Drowned drowned) {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new EnhancedDrownedAttackGoal(drowned), 3);
        } else {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new EnhancedZombieAttackGoal(zombie), 3);
        }
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        super.onInitializeZombieEquipment(zombie, data);
        if (zombie instanceof Drowned drowned) {

            if (RandomUtils.booleanByChance(0.08d)) {
                drowned.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            }


            if (RandomUtils.booleanByChance(0.0085F)) {
                drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            }


        }
    }
}
