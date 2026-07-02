package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class ZombieShieldGoal extends Goal {

    private final Zombie zombie;
    private int coolTime = 0;
    private boolean isUsingShield = false;
    private boolean isBlocked = false;
    private long lastBlockTime = 0L;
    private long shieldBrokenTime = -1L;
    public double speedModify = 1.0d;

    public ZombieShieldGoal(Zombie zombie) {
        this.zombie = zombie;
    }

    @Override
    public boolean canUse() {
        return this.isHoldingShield();
    }

    @Override
    public boolean canContinueToUse() {
        return this.isHoldingShield();
    }

    @Override
    public void start() {
        this.coolTime = 0;
        this.isUsingShield = false;
        this.isBlocked = false;
        this.shieldBrokenTime = -1L;
    }

    @Override
    public void stop() {
        this.stopUsingShield();
        this.coolTime = 0;
        this.isBlocked = false;
        this.shieldBrokenTime = -1L;
    }

    @Override
    public void tick() {
        this.coolTime--;
        if (this.isBlocked && (this.zombie.level().getGameTime() - this.lastBlockTime) > 1) {
            this.isBlocked = false;
        }
        
        // 清除破盾标记（2 ticks 后过期）
        if (this.shieldBrokenTime > 0 && (this.zombie.level().getGameTime() - this.shieldBrokenTime) > 2) {
            this.shieldBrokenTime = -1L;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean isHoldingShield() {
        return this.zombie.getMainHandItem().getItem() instanceof ShieldItem
                || this.zombie.getOffhandItem().getItem() instanceof ShieldItem;
    }

    /**
     * 放下盾牌
     */

    public void stopUsingShield() {
        if (!this.isHoldingShield()) return;
        if (!this.isUsingShield()) return;

        this.zombie.stopUsingItem();
        this.coolTime = 5;
        this.isUsingShield = false;
        this.speedModify = 1.0d;
    }


    /**
     * 尝试举起盾牌
     */

    public boolean checkAndStartUsingShield() {
        if (!this.isHoldingShield()) return false;
        if (this.isUsingShield()) return false;
        if (this.zombie.isUsingItem()) return false;
        if (this.coolTime > 0) return false;

        if (this.zombie.getMainHandItem().getItem() instanceof ShieldItem) {
            this.zombie.startUsingItem(InteractionHand.MAIN_HAND);
        } else if (this.zombie.getOffhandItem().getItem() instanceof ShieldItem) {
            this.zombie.startUsingItem(InteractionHand.OFF_HAND);
        }
        this.isUsingShield = true;
        this.speedModify = 0.5d;
        return true;
    }

    /**
     * 盾牌格挡回调
     */
    public void onShieldBlock(ItemStack weapon, LivingEntity attacker) {
        if (!this.isUsingShield()) return;

        this.zombie.handleEntityEvent((byte) 29);
        this.isBlocked = true;
        this.lastBlockTime = this.zombie.level().getGameTime();

        if (weapon != null && !weapon.isEmpty() && weapon.getItem().canDisableShield(weapon, this.zombie.getUseItem(), this.zombie, attacker)) {
            // 斧头破盾：记录破盾时间戳
            this.shieldBrokenTime = this.zombie.level().getGameTime();
            this.stopUsingShield();
            this.zombie.handleEntityEvent((byte) 30);
            this.coolTime = 100;
        } else {
            zombie.level().playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE,
                    1.0F, 1.0F / (zombie.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    /**
     * 判断是否正在使用盾牌
     */
    public boolean isUsingShield() {
        if (!(this.zombie.getUseItem().getItem() instanceof ShieldItem)) {
            this.isUsingShield = false;
        }
        return this.isUsingShield;
    }

    /**
     * 检查是否刚刚破盾（用于免疫击退）
     */
    public boolean wasShieldJustBroken() {
        return this.shieldBrokenTime > 0;
    }
}
