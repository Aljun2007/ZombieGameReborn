package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
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
        LivingEntity target = this.zombie.getTarget();

        if (target==null) return false;

        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)){
            this.zombie.setTarget(null);
            return false;
        }

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
            this.tryUsingShield();
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
        if (this.shieldGoal.checkAndStartUsingShield()) {
            if (this.zombie.getTarget() != null) {
                this.zombie.getNavigation().moveTo(this.zombie.getTarget(), this.getSpeedModifier());
            }
        }
        this.useTime = 10;
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
        if (enemy instanceof AbstractVillager) {
            return false;
        }
        if (enemy instanceof Mob enemyMob) {
            if (this.zombie == enemyMob.getTarget()) {
                return false;
            }
        }
        if (stack.getItem() instanceof BowItem) {
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                return true;
            }
        } else if (stack.getItem() instanceof TridentItem) {
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                return true;
            }
        } else if (stack.getItem() instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(stack)) {
                return true;
            }
            if (enemy.isUsingItem() && enemy.getUseItem().equals(stack)) {
                return true;
            }
        }
        if (ZGRDiplomacyCenter.TACZ_DIPLOMAT.isLoaded()) {
            if (ZGRDiplomacyCenter.TACZ_DIPLOMAT.isGunLoaded(stack)) {
                return true;
            }
        }
        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isGunLoaded(stack)) {
                return true;
            }
        }
        if (ZGRDiplomacyCenter.POINTBLANK_DIPLOMAT.isLoaded()) {
            if (ZGRDiplomacyCenter.POINTBLANK_DIPLOMAT.isGunLoaded(stack)) {
                return true;
            }
        }
        return this.zombie.distanceTo(enemy) <= 5d;
    }

    @Override
    protected boolean checkAndPerformAttack(LivingEntity target, double distanceSqr) {
        if (super.checkAndPerformAttack(target, distanceSqr)) {
            this.stopUsingShield();
        }
        return true;
    }
}
