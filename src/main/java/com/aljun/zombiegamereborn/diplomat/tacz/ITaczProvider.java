package com.aljun.zombiegamereborn.diplomat.tacz;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;

public interface ITaczProvider {
    /**
     * 注册 Tacz 的 Forge 事件监听器到传入的事件总线
     */
    void registerEvents(IEventBus forgeBus);
    
    boolean isGunLoaded(ItemStack stack);
}
