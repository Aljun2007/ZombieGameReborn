package com.aljun.zombiegamereborn.common.entity.capability;

import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombiePlaceBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ZombieData implements IZombieData {
    private boolean isSunSensitive = true;
    private ResourceLocation type = null;
    private ZombieType zombieType = null;
    private double miningSpeed = 1.0;
    private boolean canSwim = false;
    private boolean typeInitialized = false;
    private int tickCount = 0;
    private boolean fireImmune = false;
    private boolean isEmpowered = true;
    private double movementSpeedModify = 1.0d;
    private boolean canJumpAttack = false;
    private boolean followMustSee = false;
    private @Nullable ZombieSenseTargetGoal zombieSenseTargetGoal = null;
    private @Nullable ZombieBreakBlockGoal zombieBreakBlockGoal = null;
    private @Nullable ZombiePlaceBlockGoal zombiePlaceBlockGoal = null;
    private boolean enhancedSense = false;

    @Override
    public @Nullable ZombieSenseTargetGoal getZombieSenseTargetGoalGoal() {
        return this.zombieSenseTargetGoal;
    }

    @Override
    public void setZombieSenseTargetGoalGoal(ZombieSenseTargetGoal zombieSenseTargetGoalGoal) {
        this.zombieSenseTargetGoal = zombieSenseTargetGoalGoal;
    }

    @Override
    public @Nullable ZombieBreakBlockGoal getZombieBreakBlockGoal() {
        return this.zombieBreakBlockGoal;
    }

    @Override
    public void setZombieBreakBlockGoal(ZombieBreakBlockGoal goal) {
        this.zombieBreakBlockGoal = goal;
    }

    @Override
    public @Nullable ZombiePlaceBlockGoal getZombiePlaceBlockGoal() {
        return this.zombiePlaceBlockGoal;
    }

    @Override
    public void setZombiePlaceBlockGoal(ZombiePlaceBlockGoal goal) {
        this.zombiePlaceBlockGoal = goal;
    }

    @Override
    public boolean enhancedSense() {
        return this.enhancedSense;
    }

    @Override
    public void setEnhancedSense(boolean value) {
        this.enhancedSense = value;
    }

    @Override
    public boolean isSunSensitive() {
        return isSunSensitive;
    }

    @Override
    public void setSunSensitive(boolean value) {
        this.isSunSensitive = value;
    }

    @Override
    public boolean fireImmune() {
        return fireImmune;
    }

    @Override
    public void setFireImmune(boolean value) {
        fireImmune = value;
    }

    @Override
    public ResourceLocation getTypeID() {
        if (this.type == null) {
            this.type = ZGRZombieTypes.DUMMY.getId();
        }
        return type;
    }

    @Override
    public void setTypeID(ResourceLocation type) {
        this.zombieType = ZombieType.getById(type);
        if (this.zombieType == null) {
            this.zombieType = ZGRZombieTypes.DUMMY;
        }
        this.type = type;
    }

    @Override
    public ZombieType getType() {
        return this.zombieType;
    }

    @Override
    public double getMiningSpeed() {
        return miningSpeed;
    }

    @Override
    public void setMiningSpeed(double speed) {
        this.miningSpeed = speed;
    }

    @Override
    public boolean canSwim() {
        return canSwim;
    }

    @Override
    public void enableSwim(boolean canSwim) {
        this.canSwim = canSwim;
    }

    @Override
    public boolean isTypeInitialized() {
        return typeInitialized;
    }

    @Override
    public void setTypeInitialized(boolean initialized) {
        this.typeInitialized = initialized;
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public void incrementTick() {
        this.tickCount++;
    }

    @Override
    public boolean isEmpowered() {
        return this.isEmpowered;
    }

    @Override
    public void setEmpowered(boolean value) {
        this.isEmpowered = value;
    }

    @Override
    public void setMovementSpeedModify(double modify) {
        this.movementSpeedModify = modify;
    }

    @Override
    public double getMovementSpeedModify() {
        return this.movementSpeedModify;
    }

    @Override
    public boolean canJumpAttack() {
        return this.canJumpAttack;
    }

    @Override
    public void enableJumpAttack(boolean value) {
        this.canJumpAttack = value;
    }

    @Override
    public boolean followMustSee() {
        return this.followMustSee;
    }

    @Override
    public void setFollowMustSee(boolean value) {
        this.followMustSee = value;
    }
}
