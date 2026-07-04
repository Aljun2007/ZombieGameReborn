package com.aljun.zombiegamereborn.common.entity.goal.attack;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class ZombieBowAttackGoal extends Goal {

    protected static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;

    protected final Zombie zombie;
    protected final double speedModifier;
    protected final float attackRadius;
    protected final float attackRadiusSqr;

    protected int attackIntervalMin;
    protected int attackTime = -1;
    protected int seeTime;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime = -1;

    protected long lastCanUseCheck;
    protected int ticksUntilNextPathRecalculation;

    public ZombieBowAttackGoal(Zombie zombie, double speedModifier, int attackIntervalMin, float attackRadius) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public ZombieBowAttackGoal(Zombie zombie) {
        this(zombie, 1.0D, 20, 15.0F);
    }

    public void setMinAttackInterval(int interval) {
        this.attackIntervalMin = interval;
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
        return this.isHoldingBow();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
            return false;
        }
        return (this.isHoldingBow()) && (this.zombie.getSensing().hasLineOfSight(target)
                || !this.zombie.getNavigation().isDone());
    }

    @Override
    public void start() {
        this.zombie.setAggressive(true);
        this.attackTime = -1;
        this.seeTime = 0;
        this.strafingTime = -1;
        this.ticksUntilNextPathRecalculation = 0;
    }

    @Override
    public void stop() {
        this.zombie.setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.strafingTime = -1;
        this.zombie.stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    protected boolean isHoldingBow() {
        return this.zombie.isHolding(item -> item.getItem() instanceof BowItem);
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

        this.handleBowAction(target, distanceSqr, canSee);
    }

    protected void handleMovement(LivingEntity target, double distanceSqr) {
        float enterStrafeSqr = this.attackRadiusSqr * 0.7F;
        float exitStrafeSqr = this.attackRadiusSqr * 1.1F;
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
            if (distanceSqr > (double) (this.attackRadiusSqr * 0.75F)) {
                this.strafingBackwards = false;
            } else if (distanceSqr < (double) (this.attackRadiusSqr * 0.25F)) {
                this.strafingBackwards = true;
            }
            this.zombie.getMoveControl().strafe(
                    this.strafingBackwards ? -1.0F : 1.0F,
                    this.strafingClockwise ? 0.8F : -0.8F
            );
        }
    }

    protected void handleBowAction(LivingEntity target, double distanceSqr, boolean canSee) {
        if (this.zombie.isUsingItem()) {
            if (!canSee && this.seeTime < -60) {
                this.zombie.stopUsingItem();
            } else if (canSee) {
                int usingTime = this.zombie.getTicksUsingItem();
                if (usingTime >= 20) {
                    this.zombie.stopUsingItem();
                    this.performRangedAttack(target, BowItem.getPowerForTime(usingTime));
                    this.attackTime = this.attackIntervalMin;
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60 && distanceSqr <= (double) this.attackRadiusSqr) {
            this.zombie.startUsingItem(ProjectileUtil.getWeaponHoldingHand(
                    this.zombie, item -> item instanceof BowItem));
        }
    }

    protected void performRangedAttack(LivingEntity target, float power) {
        ItemStack weaponStack = this.zombie.getItemInHand(
                ProjectileUtil.getWeaponHoldingHand(this.zombie, item -> item instanceof BowItem));
        ItemStack projectileStack = this.zombie.getProjectile(weaponStack);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this.zombie, projectileStack, power);

        if (weaponStack.getItem() instanceof BowItem bowItem) {
            arrow = bowItem.customArrow(arrow);
        }

        double dx = target.getX() - this.zombie.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.zombie.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + horizontalDist * 0.2F, dz, 1.6F,
                (float) (14 - this.zombie.level().getDifficulty().getId() * 4));

        this.zombie.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (this.zombie.getRandom().nextFloat() * 0.4F + 0.8F));
        this.zombie.level().addFreshEntity(arrow);
    }
}
