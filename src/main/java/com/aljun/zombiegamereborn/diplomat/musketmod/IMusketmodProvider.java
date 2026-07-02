package com.aljun.zombiegamereborn.diplomat.musketmod;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

public interface IMusketmodProvider {
    Goal createZombieGunGoal(Zombie zombie);
    void setMobDamageMultiplier(float multiplier);
    ItemStack getGunStack();
    ItemStack getAmmoStack();
}
