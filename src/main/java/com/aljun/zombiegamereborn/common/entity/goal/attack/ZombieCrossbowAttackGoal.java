package com.aljun.zombiegamereborn.common.entity.goal.attack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ZombieCrossbowAttackGoal extends Goal implements CrossbowAttackMob {

    protected static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;

    protected final Zombie zombie;
    protected final double speedModifier;
    protected final float attackRadius;
    protected final float attackRadiusSqr;

    protected int seeTime;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime = -1;

    protected CrossbowState crossbowState = CrossbowState.UNCHARGED;
    protected int attackDelay;

    protected long lastCanUseCheck;

    public ZombieCrossbowAttackGoal(Zombie zombie, double speedModifier, float attackRadius) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public ZombieCrossbowAttackGoal(Zombie zombie) {
        this(zombie, 1.0D, 10.0F);
    }

    @Override
    public boolean canUse() {
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }
        this.lastCanUseCheck = gameTime;

        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return this.isHoldingCrossbow();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return this.isHoldingCrossbow() && (this.zombie.getSensing().hasLineOfSight(target)
                || !this.zombie.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.zombie.setAggressive(true);
        this.seeTime = 0;
        this.strafingTime = -1;
        this.crossbowState = CrossbowState.UNCHARGED;
        this.attackDelay = 0;
    }

    @Override
    public void stop() {
        this.zombie.setAggressive(false);
        this.seeTime = 0;
        this.strafingTime = -1;
        this.crossbowState = CrossbowState.UNCHARGED;
        this.attackDelay = 0;
        this.setChargingCrossbow(false);
        if (this.zombie.isUsingItem()) {
            this.zombie.stopUsingItem();
            ItemStack useItem = this.zombie.getUseItem();
            if (useItem.getItem() instanceof CrossbowItem) {
                CrossbowItem.setCharged(useItem, false);
            }
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    protected boolean isHoldingCrossbow() {
        return this.zombie.isHolding(item -> item.getItem() instanceof CrossbowItem);
    }

    @Override
    public void tick() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return;
        }

        this.zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distanceSqr = this.zombie.distanceToSqr(target.getX(), target.getY(), target.getZ());

        boolean canSee = this.zombie.getSensing().hasLineOfSight(target);
        boolean wasSeeing = this.seeTime > 0;
        if (canSee != wasSeeing) {
            this.seeTime = 0;
        }
        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        this.handleMovement(target, distanceSqr);
        this.handleCrossbowAction(target, distanceSqr, canSee);
    }

    protected void handleMovement(LivingEntity target, double distanceSqr) {
        float enterStrafeSqr = this.attackRadiusSqr * 0.9F;
        float exitStrafeSqr = this.attackRadiusSqr * 1.21F;
        double dist = Math.sqrt(distanceSqr);

        if (distanceSqr <= (double) enterStrafeSqr && this.seeTime >= 20) {
            if (this.strafingTime < 0) {
                this.zombie.getNavigation().stop();
                this.strafingTime = 0;
            } else {
                ++this.strafingTime;
            }
        } else if (distanceSqr > (double) exitStrafeSqr || this.seeTime < -60) {
            double speed = dist <= this.attackRadius + 5.0D ? this.speedModifier * 0.6D : this.speedModifier;
            this.zombie.getNavigation().moveTo(target, speed);
            this.strafingTime = -1;
        } else if (this.strafingTime < 0) {
            this.zombie.getNavigation().moveTo(target, this.speedModifier * 0.6D);
        }

        if (this.strafingTime >= 20) {
            if ((double) this.zombie.getRandom().nextFloat() < 0.3D) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            if ((double) this.zombie.getRandom().nextFloat() < 0.3D) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }

        if (this.strafingTime > -1) {
            if (distanceSqr > (double) (this.attackRadiusSqr * 0.8F)) {
                this.strafingBackwards = false;
            } else if (distanceSqr < (double) (this.attackRadiusSqr * 0.5F)) {
                this.strafingBackwards = true;
            }

            float speed = (float) this.speedModifier * (this.crossbowState == CrossbowState.CHARGING ? 0.5F : 1.0F);
            if (this.zombie.getNavigation().isDone()) {
                this.zombie.getMoveControl().strafe(
                        this.strafingBackwards ? -speed : speed,
                        this.strafingClockwise ? speed : -speed
                );
            }
        }
    }

    protected void handleCrossbowAction(LivingEntity target, double distanceSqr, boolean canSee) {
        switch (this.crossbowState) {
            case UNCHARGED -> {
                this.zombie.startUsingItem(ProjectileUtil.getWeaponHoldingHand(
                        this.zombie, item -> item instanceof CrossbowItem));
                this.crossbowState = CrossbowState.CHARGING;
                this.setChargingCrossbow(true);
            }
            case CHARGING -> {
                if (!this.zombie.isUsingItem()) {
                    this.crossbowState = CrossbowState.UNCHARGED;
                    return;
                }
                int usingTime = this.zombie.getTicksUsingItem();
                ItemStack useItem = this.zombie.getUseItem();
                if (usingTime >= CrossbowItem.getChargeDuration(useItem)) {
                    this.zombie.releaseUsingItem();
                    this.crossbowState = CrossbowState.CHARGED;
                    this.attackDelay = 20 + this.zombie.getRandom().nextInt(20);
                    this.setChargingCrossbow(false);
                }
            }
            case CHARGED -> {
                --this.attackDelay;
                if (this.attackDelay <= 0) {
                    this.crossbowState = CrossbowState.READY_TO_ATTACK;
                }
            }
            case READY_TO_ATTACK -> {
                if (canSee && !(distanceSqr > (double) this.attackRadiusSqr) && this.seeTime >= 20) {
                    this.performRangedAttack(target, 1.0F);
                    ItemStack heldItem = this.zombie.getItemInHand(ProjectileUtil.getWeaponHoldingHand(
                            this.zombie, item -> item instanceof CrossbowItem));
                    CrossbowItem.setCharged(heldItem, false);
                    this.crossbowState = CrossbowState.UNCHARGED;
                }
            }
        }
    }

    @Override
    public void setChargingCrossbow(boolean charged) {
    }

    @Override
    public void shootCrossbowProjectile(@NotNull LivingEntity target, @NotNull ItemStack stack, @NotNull Projectile projectile, float velocity) {
        double dx = target.getX() - this.zombie.getX();
        double dy = target.getY(0.3333333333333333D) - projectile.getY();
        double dz = target.getZ() - this.zombie.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + horizontalDist * 0.2F, dz, velocity, (float) (14 - this.zombie.level().getDifficulty().getId() * 4));
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.zombie.setNoActionTime(0);
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity target, float power) {
        this.performCrossbowAttack(this.zombie, 1.6F);
    }

    @Override
    public LivingEntity getTarget() {
        return this.zombie.getTarget();
    }

    protected enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}
