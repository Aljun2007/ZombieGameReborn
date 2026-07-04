package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieMeleeAndPathBuildGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class BuilderZombieType extends ZombieType {


    public BuilderZombieType() {
        super(ZGRZombieTypes.IDs.BUILDER_ID);
    }

    @Override
    public void onInitializeZombieWeaponsAndArmors(Zombie zombie, IZombieData data) {
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.DIRT));
    }

    @Override
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
        data.setEmpowered(false);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieType.replaceGoal(zombie.goalSelector,goal-> goal instanceof ZombieAttackGoal, ()->new ZombieMeleeAndPathBuildGoal(zombie,data),2);
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
