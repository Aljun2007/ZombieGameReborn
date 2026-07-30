package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.equipement.ZombieEquipmentHelper;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieSmartBreakAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.drowned.DrownedSmartBreakAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.drowned.EnhancedDrownedAttackGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class DrownedMinerType extends ZombieType {

    public DrownedMinerType() {
        super(ZGRZombieTypes.IDs.DROWNED_MINER_ID);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie, IZombieData data) {
        if (zombie instanceof Drowned drowned) {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new DrownedSmartBreakAttackGoal(drowned, data), 2);
            zombie.goalSelector.addGoal(3, new EnhancedDrownedAttackGoal(drowned));
        } else {
            ZombieType.replaceGoal(zombie.goalSelector, goal -> goal instanceof ZombieAttackGoal, () -> new ZombieSmartBreakAttackGoal(zombie, data), 2);
            zombie.goalSelector.addGoal(3, new EnhancedZombieAttackGoal(zombie));
        }

    }

    @Override
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
        data.setEmpowered(false);
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        if (RandomUtils.booleanByChance(0.4d)) {
            ZombieEquipmentHelper.applyFullEquipmentWithPickaxe(zombie);
        } else {
            ZombieEquipmentHelper.applyFullEquipment(zombie);
        }
        if (zombie instanceof Drowned drowned) {
            if (RandomUtils.booleanByChance(0.08d)) {
                drowned.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
            }
            if (RandomUtils.booleanByChance(0.0085F)) {
                drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            }
        }
    }

    @Override
    public void onTick(Zombie zombie, IZombieData data, int tickCount) {
        if (!data.isEmpowered()) {
            ZombieGoalOptimizer.requestForEmpowerment(data);
        }
        super.onTick(zombie, data, tickCount);
    }

    @Override
    public boolean canBreakBlocks() {
        return true;
    }

    @SuppressWarnings("all")
    @Override
    public void onZombieHurt(LivingHurtEvent event, Zombie zombie, IZombieData data) {
        Entity entity = event.getSource().getEntity();
        if (entity instanceof Player player) {
            ZombieSmartBreakAttackGoal goal = (ZombieSmartBreakAttackGoal) ZGRZombieControlAPI.getGoal(zombie, (goa1l) -> goa1l instanceof ZombieSmartBreakAttackGoal);
            if (goal != null) {
                goal.onZombieHurt();
            }
        }
    }
}
