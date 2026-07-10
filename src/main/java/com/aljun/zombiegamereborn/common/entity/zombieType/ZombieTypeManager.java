package com.aljun.zombiegamereborn.common.entity.zombieType;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.accessor.ITargetGoalAccessor;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.*;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.ZombieCapacitySyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieTypeManager {


    public static void initializeZombie(Zombie zombie, ResourceLocation typeID) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data.isTypeInitialized()) {
            return;
        }
        data.setTypeInitialized(true);
        ZGRZombieAttributesAPI.setTypeID(data, typeID);
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            ZGRGame.getGameProperty().getStageProperty(zombie.getServer()).zombieProperty.loadZombieAttributes(zombie);
            type.onInitializeZombieAttributes(zombie, data);
            type.onInitializeZombieWeaponsAndArmors(zombie, data);
        }

        if (!zombie.level().isClientSide) {
            syncToClient(zombie);
        }
    }

    public static void syncToClient(Zombie zombie) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        CompoundTag tag = ZombieDataProvider.serializeNBT(data);
        ZGRNetwork.sendToTrackingEntity(new ZombieCapacitySyncPacket(zombie.getId(), tag), zombie);
    }

    public static void initializeZombieWithNoWeaponAndArmor(Zombie zombie, ResourceLocation typeID) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data.isTypeInitialized()) {
            return;
        }
        data.setTypeInitialized(true);
        ZGRZombieAttributesAPI.setTypeID(data, typeID);
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            ZGRGame.getGameProperty().getStageProperty(zombie.getServer()).zombieProperty.loadZombieAttributes(zombie);
            type.onInitializeZombieAttributes(zombie, data);
        }

        if (!zombie.level().isClientSide) {
            syncToClient(zombie);
        }
    }

    /**
     * 每刻更新僵尸状态（服务端）
     *
     * @param zombie 僵尸实体
     */
    public static void tickZombie(Zombie zombie) {
        if (zombie.level().isClientSide) {
            return;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (!data.isTypeInitialized()) {
            return;
        }
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            int tickCount = data.getTickCount();
            if (tickCount == 0) {
                ZombieStatic.incrementZombieCount(zombie, data);
                initialGoal(zombie, data, type);
            }

            // 每 tick 触发感知衰减
            ZombieSenseTargetGoal senseGoal = data.getZombieSenseTargetGoalGoal();
            if (senseGoal != null) {
                senseGoal.tickDecay();
            }

            type.onTick(zombie, data, tickCount);
            data.incrementTick();
        }
    }

    private static void initialGoal(Zombie zombie, IZombieData data, ZombieType type) {
        type.onInitializeZombieGoals(zombie, data);
        if (type.canBreakBlocks()) {
            ZombieBreakBlockGoal breakBlockGoal = new ZombieBreakBlockGoal(zombie, data);
            data.setZombieBreakBlockGoal(breakBlockGoal);
            zombie.goalSelector.addGoal(1, breakBlockGoal);
            zombie.goalSelector.addGoal(2, new ClearHeadBlockGoal(zombie, data));
            zombie.goalSelector.addGoal(3, new ZombieWaterBridgeBuildGoal(zombie, data));
        }
        if (type.canPlaceBlock()) {
            ZombiePlaceBlockGoal placeBlockGoal = new ZombiePlaceBlockGoal(zombie, data);
            data.setZombiePlaceBlockGoal(placeBlockGoal);
            zombie.goalSelector.addGoal(1, placeBlockGoal);
        }
        if (data.canSwim()) {
            zombie.goalSelector.addGoal(1, new ZombieFloatGoal(zombie));
        }
        if (data.canJumpAttack()) {
            zombie.goalSelector.addGoal(3, new JumpAttackGoal(zombie));
        }
        zombie.targetSelector.getRunningGoals().forEach(wrappedGoal -> {
            if (wrappedGoal.getGoal() instanceof ITargetGoalAccessor targetGoal) {
                targetGoal.set_mustSee(data.followMustSee() && targetGoal.get_mustSee());
            }
        });
        if (data.enhancedSense()) {
            ZombieSenseTargetGoal senseGoal = new ZombieSenseTargetGoal(zombie);
            data.setZombieSenseTargetGoalGoal(senseGoal);
            zombie.targetSelector.addGoal(4, senseGoal);
        }
        if (data.fleeSun()) {
            zombie.goalSelector.addGoal(2, new ZombieRestrictSunGoal(zombie,data));
            zombie.goalSelector.addGoal(3, new ZombieFleeSunGoal(zombie));
        }
    }


}

