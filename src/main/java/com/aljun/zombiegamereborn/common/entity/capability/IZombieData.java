package com.aljun.zombiegamereborn.common.entity.capability;

import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombiePlaceBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public interface IZombieData {

    @Nullable
    ZombieSenseTargetGoal getZombieSenseTargetGoalGoal();

    void setZombieSenseTargetGoalGoal(ZombieSenseTargetGoal zombieSenseTargetGoalGoal);

    @Nullable
    ZombieBreakBlockGoal getZombieBreakBlockGoal();

    void setZombieBreakBlockGoal(ZombieBreakBlockGoal goal);

    @Nullable
    ZombiePlaceBlockGoal getZombiePlaceBlockGoal();

    void setZombiePlaceBlockGoal(ZombiePlaceBlockGoal goal);

    boolean enhancedSense();
    void setEnhancedSense(boolean value);

    boolean isSunSensitive();
    void setSunSensitive(boolean value);

    boolean fireImmune();
    void setFireImmune(boolean value);

    ResourceLocation getTypeID();
    void setTypeID(ResourceLocation type);
    ZombieType getType();

    double getMiningSpeed();
    void setMiningSpeed(double speed);

    boolean canSwim();
    void enableSwim(boolean canSwim);

    boolean isTypeInitialized();
    void setTypeInitialized(boolean initialized);

    int getTickCount();
    void incrementTick();

    boolean isEmpowered();
    void setEmpowered(boolean value);

    void setMovementSpeedModify(double modify);
    double getMovementSpeedModify();

    boolean canJumpAttack();
    void enableJumpAttack(boolean value);

    boolean followMustSee();
    void setFollowMustSee(boolean value);
}
