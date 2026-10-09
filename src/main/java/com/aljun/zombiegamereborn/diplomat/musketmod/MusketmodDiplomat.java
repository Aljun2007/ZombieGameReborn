package com.aljun.zombiegamereborn.diplomat.musketmod;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

public class MusketmodDiplomat extends Diplomat {

    private IMusketmodProvider provider = null;

    @Override
    public String getModID() {
        return "musketmod";
    }

    @Override
    public void init() {
        super.init();
        if (this.isLoaded()) {
            try {
                Class<?> implClass = Class.forName(
                        "com.aljun.zombiegamereborn.diplomat.musketmod.MusketmodProviderImpl"
                );
                this.provider = (IMusketmodProvider) implClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // 反射加载失败
            }
        }
    }

    /**
     * 创建一个僵尸火枪手 Goal，musketmod 未安装时返回 null
     */
    public Goal createGunnerGoal(Zombie zombie) {
        if (provider != null) {
            return provider.createGunnerGoal(zombie);
        }
        return null;
    }

    /**
     * 设置怪物火枪伤害倍率，覆盖 musketmod 的 mobDamageMultiplier（默认 0.5）
     * 仅 musketmod 已安装时生效
     */
    public void setMobDamageMultiplier(double multiplier) {
        if (provider != null) {
            provider.setMobDamageMultiplier((float) multiplier);
        }
    }

    public boolean isGunLoaded(ItemStack stack) {
        if (provider != null) {
             return  provider.isGunLoaded(stack);
        }
        return false;
    }

    public boolean isHoldingGun(LivingEntity livingEntity) {
        if (provider != null) {
            return  provider.isHoldingGun(livingEntity);
        }
        return false;
    }

    public ItemStack getGunStack() {
        if (provider != null) {
            return  provider.getGunStack();
        }
        return ItemStack.EMPTY;
    }


    public ItemStack getBulletStack(int bulletCount) {
        if (provider != null) {
            return  provider.getBulletStack(bulletCount);
        }
        return ItemStack.EMPTY;
    }
}
