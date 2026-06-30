package com.aljun.zombiegamereborn.common.entity.zombieType;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.accessor.ITargetGoalAccessor;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.*;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
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
            type.onInitializeZombieAppearance(zombie,data);
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
                ZombieStatic.incrementZombieCount(zombie,data);
                type.onInitializeZombieGoals(zombie);
                if (type.canBreakBlocks()) {
                    ZombieBreakBlockGoal breakBlockGoal = new ZombieBreakBlockGoal(zombie);
                    data.setZombieBreakBlockGoal(breakBlockGoal);
                    zombie.goalSelector.addGoal(1, breakBlockGoal);
                    zombie.goalSelector.addGoal(2, new ClearHeadBlockGoal(zombie));
                    zombie.goalSelector.addGoal(3, new ZombieWaterBridgeBuildGoal(zombie));
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
                        targetGoal.set_mustSee(data.followMustSee()&&targetGoal.get_mustSee());
                    }
                });
                if (data.enhancedSense()) {
                    ZombieSenseTargetGoal senseGoal = new ZombieSenseTargetGoal(zombie);
                    data.setZombieSenseTargetGoalGoal(senseGoal);
                    zombie.targetSelector.addGoal(4, senseGoal);
                }
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


}

