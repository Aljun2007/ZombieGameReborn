package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

public class MusketModGunnerType extends ZombieType {

    public MusketModGunnerType() {
        super(ZGRZombieTypes.IDs.MUSKET_MOD_GUNNER_ID);
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        super.onInitializeZombieEquipment(zombie, data);
        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            ItemStack gun = ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.getGunStack();
            if (!gun.isEmpty()) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, gun);
            }
        }
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            ZombieType.replaceGoal(zombie.goalSelector,(goal -> goal instanceof ZombieAttackGoal), ()->ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.createGunnerGoal(zombie),2);
            zombie.goalSelector.addGoal(4,new EnhancedZombieAttackGoal( zombie));
        }
    }

    @Override
    public boolean onlyMelee() {
        return false;
    }
}
