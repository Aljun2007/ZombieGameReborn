package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

public class ZombieShieldAttackGoal extends EnhancedZombieAttackGoal {

    private final ZombieShieldGoal shieldGoal;
    private int useTime = 0;

    public ZombieShieldAttackGoal(Zombie zombie, ZombieShieldGoal shieldGoal) {
        super(zombie);
        this.shieldGoal = shieldGoal;
    }

    @Override
    protected double getSpeedModifier() {
        return super.getSpeedModifier() * this.shieldGoal.speedModify;
    }

    @Override
    public boolean canUse() {
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        this.useTime = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.shieldGoal.stopUsingShield();
        this.useTime = 0;
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            this.stopUsingShield();
            return;
        }

        this.useTime--;
        if (this.needToUseShield(target)) {
            if (!this.shieldGoal.isUsingShield()) {
                this.tryUsingShield();
            }
        } else {
            if (this.useTime <= 0) {
                this.stopUsingShield();
            }
        }
    }

    /**
     * 停止使用盾牌
     */
    public void stopUsingShield() {
        this.useTime = 0;
        this.shieldGoal.stopUsingShield();
        if (this.zombie.getTarget() != null) {
            this.zombie.getNavigation().moveTo(this.zombie.getTarget(), this.getSpeedModifier());
        }
    }

    /**
     * 尝试举起盾牌（检测威胁）
     */
    public void tryUsingShield() {
        LivingEntity target = this.zombie.getTarget();
        if (target != null && this.needToUseShield(target)) {
            if (this.shieldGoal.checkAndStartUsingShield()){
                this.useTime = 10;
                this.zombie.getNavigation().moveTo(this.zombie.getTarget(), this.getSpeedModifier());
            }

        }
    }

    private boolean needToUseShield(LivingEntity target) {
        if (!target.hasLineOfSight(this.zombie)) {
            return false;
        }
        ItemStack mainHand = target.getMainHandItem();
        ItemStack offHand = target.getOffhandItem();
        return this.isEnemyThreatening(target, mainHand)
                || this.isEnemyThreatening(target, offHand);
    }

    private boolean isEnemyThreatening(LivingEntity enemy, ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof BowItem) {
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                this.useTime = 10;
                return true;
            }
        } else if (stack.getItem() instanceof TridentItem) {
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                this.useTime = 10;
                return true;
            }
        } else if (stack.getItem() instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(stack)) {
                this.useTime = 10;
                return true;
            }
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                this.useTime = 10;
                return true;
            }
        }
        if (this.zombie.distanceTo(enemy) <= 5d) {
            this.useTime = 10;
            return true;
        }
        return false;
    }
}
