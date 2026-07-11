package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieMeleeAndPathBuildGoal;
import com.aljun.zombiegamereborn.common.entity.goal.attack.ZombieSmartBreakAttackGoal;
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

public class MinerZombieType extends ZombieType {


    public MinerZombieType() {
        super(ZGRZombieTypes.IDs.MINER_ID);
    }

    @Override
    public void onInitializeZombieEquipment(Zombie zombie, IZombieData data) {
        super.onInitializeZombieEquipment(zombie, data);
    }

    @Override
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
        data.setEmpowered(false);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
        ZombieType.replaceGoal(zombie.goalSelector,goal-> goal instanceof ZombieAttackGoal, ()->new ZombieSmartBreakAttackGoal(zombie,data),2);
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
