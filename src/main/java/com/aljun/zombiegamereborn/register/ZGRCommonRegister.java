package com.aljun.zombiegamereborn.register;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZGRCommonRegister {
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        ZGRRegistries.register(event);
    }
    @SubscribeEvent
    public static void registerZombieTypes(RegisterEvent event) {
        event.register(ZGRRegistries.Keys.ZOMBIE_TYPES_KEY, ZGRZombieTypes::register);
    }
}
