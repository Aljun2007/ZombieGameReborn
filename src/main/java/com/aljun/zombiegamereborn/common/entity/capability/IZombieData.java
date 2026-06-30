package com.aljun.zombiegamereborn.common.entity.capability;

import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombiePlaceBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 僵尸数据接口
 * 存储僵尸的类型和 tick 状态
 */
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

    /**
     * 检查是否已初始化类型
     * @return 如果已初始化返回 true
     */
    boolean isTypeInitialized();

    /**
     * 标记类型已初始化
     * @param initialized 是否已初始化
     */
    void setTypeInitialized(boolean initialized);

    /**
     * 获取 tick 计数器
     * @return 当前 tick 数
     */
    int getTickCount();

    /**
     * 增加 tick 计数
     */
    void incrementTick();

    boolean isEmpowered();

    void setEmpowered( boolean value);

    void setMovementSpeedModify(double modify);
    double getMovementSpeedModify();

    boolean canJumpAttack();
    void enableJumpAttack(boolean value);

    boolean followMustSee();
    void setFollowMustSee(boolean value);
}
