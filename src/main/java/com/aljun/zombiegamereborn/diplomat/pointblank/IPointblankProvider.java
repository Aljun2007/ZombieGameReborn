package com.aljun.zombiegamereborn.diplomat.pointblank;

import net.minecraft.world.item.ItemStack;

public interface IPointblankProvider {
    void registerCallback();
    boolean isGunLoaded(ItemStack stack);
}
