package com.aljun.zombiegamereborn.diplomat.musketmod;

import com.aljun.zombiegamereborn.diplomat.Diplomat;
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

    public Goal createGunnerGoal(Zombie zombie) {
        if (provider != null) {
            return provider.createZombieGunGoal(zombie);
        }
        return null;
    }

    public void setMobDamageMultiplier(double multiplier) {
        if (provider != null) {
            provider.setMobDamageMultiplier((float) multiplier);
        }
    }

    public ItemStack getGunStack() {
        if (provider != null) {
            return provider.getGunStack();
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getAmmoStack() {
        if (provider != null) {
            return provider.getAmmoStack();
        }
        return ItemStack.EMPTY;
    }
}
