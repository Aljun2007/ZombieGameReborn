package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.equipement.ZombieEquipmentHelper;
import com.aljun.zombiegamereborn.common.entity.goal.attack.EnhancedZombieAttackGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieMeleeAndPathBuildGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class BuilderType extends ZombieType {


    public BuilderType() {
        super(ZGRZombieTypes.IDs.BUILDER_ID);
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        if (RandomUtils.booleanByChance(0.4d)) {
            ZombieEquipmentHelper.applyFullEquipmentWithPickaxe(zombie);
        } else {
            ZombieEquipmentHelper.applyFullEquipment(zombie);
        }
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ZombieUtils.randomPathBlock((ServerLevel) zombie.level(), zombie.blockPosition()));
    }

    @Override
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
        data.setEmpowered(false);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieType.replaceGoal(zombie.goalSelector,goal-> goal instanceof ZombieAttackGoal, ()->new ZombieMeleeAndPathBuildGoal(zombie,data),2);
        zombie.goalSelector.addGoal(3,new EnhancedZombieAttackGoal(zombie));
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

    @Override
    public boolean canPlaceBlock() {
        return true;
    }

    @SuppressWarnings("all")
    @Override
    public void onZombieHurt(LivingHurtEvent event, Zombie zombie, IZombieData data) {
        Entity entity = event.getSource().getEntity();
        if (entity instanceof Player player) {
            ZombieMeleeAndPathBuildGoal goal = (ZombieMeleeAndPathBuildGoal) ZGRZombieControlAPI.getGoal(zombie, (goa1l) -> goa1l instanceof ZombieMeleeAndPathBuildGoal);
            if (goal != null) {
                goal.onZombieHurt();
            }
        }
    }
}
