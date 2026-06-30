package com.aljun.zombiegamereborn.common.entity.goal.target;

import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.utils.ZombieDecisionUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import javax.annotation.Nullable;

public class ZombieSenseTargetGoal extends TargetGoal {

    private static final int LEAVE_RADIUS_DECAY_MULTIPLIER = 3;

    @Nullable
    private InterestPoint activePoint;

    public ZombieSenseTargetGoal(Mob mob) {
        super(mob, false);
    }

    @Override
    public boolean canUse() {
        if (this.activePoint == null) {
            return false;
        }
        LivingEntity creator = this.activePoint.creator;
        if (!ZombieDecisionUtils.isTargetLegal(creator)) {
            this.activePoint = null;
            return false;
        }
        if (this.mob.getTarget() == creator) {
            return false;
        }
        this.mob.setTarget(creator);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.activePoint == null) {
            return false;
        }
        LivingEntity creator = this.activePoint.creator;
        return ZombieDecisionUtils.isTargetLegal(creator) && !this.activePoint.isDecayed();
    }

    @Override
    public void start() {
        // target 已在 canUse 中设置
    }

    @Override
    public void stop() {
        this.activePoint = null;
    }

    @Override
    public void tick() {
        if (this.mob.getTarget() == null) {
            this.stop();
            return;
        }
        // decay 由 ZombieTypeManager 统一触发
        this.syncTarget();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * 由外部每 tick 调用，处理兴趣点衰减。衰减至零或目标死亡时清除目标。
     */
    public void tickDecay() {
        if (this.activePoint == null) {
            return;
        }

        LivingEntity creator = this.activePoint.creator;
        if (creator == null || !creator.isAlive()) {
            this.activePoint = null;
            this.mob.setTarget(null);
            return;
        }

        boolean withinRadius = this.activePoint.isWithinRadius(this.mob);
        this.activePoint.decay(withinRadius);

        if (this.activePoint.isDecayed()) {
            this.activePoint = null;
            this.mob.setTarget(null);
        }
    }

    private void syncTarget() {
        if (this.activePoint == null) {
            return;
        }
        LivingEntity creator = this.activePoint.creator;
        if (creator != null && this.mob.getTarget() != creator) {
            this.mob.setTarget(creator);
        }
    }

    public void sense(LivingEntity entity, SenseType senseType) {
        if (!ZombieDecisionUtils.zombieAttackableEntity(entity)) {
            return;
        }

        LivingEntity current = this.activePoint != null ? this.activePoint.creator : null;

        if (current != null && current != entity
                && ZombieDecisionUtils.threatLevel(entity) >= ZombieDecisionUtils.threatLevel(current)) {
            return;
        }

        if (this.activePoint != null && this.activePoint.creator == entity) {
            this.activePoint.refresh(senseType);
            return;
        }

        this.activePoint = new InterestPoint(entity, senseType);
    }

    private static class InterestPoint {
        final LivingEntity creator;
        int remainingLifespan;
        final double radius;

        InterestPoint(LivingEntity creator, SenseType senseType) {
            this.creator = creator;
            this.remainingLifespan = senseType.baseLifespan();
            this.radius = senseType.radius();
        }

        void refresh(SenseType senseType) {
            this.remainingLifespan = Math.max(this.remainingLifespan, senseType.baseLifespan());
        }

        boolean isWithinRadius(Mob mob) {
            if (this.radius >= Double.MAX_VALUE) {
                return true;
            }
            return mob.distanceToSqr(this.creator) <= this.radius * this.radius;
        }

        void decay(boolean withinRadius) {
            if (withinRadius) {
                this.remainingLifespan--;
            } else {
                this.remainingLifespan -= LEAVE_RADIUS_DECAY_MULTIPLIER;
            }
        }

        boolean isDecayed() {
            return this.remainingLifespan <= 0;
        }
    }
}
