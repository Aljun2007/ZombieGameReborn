package com.aljun.zombiegamereborn.register;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.sounds.ZGRSoundEvents;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * MOD 总线事件（NewRegistryEvent/RegisterEvent），
 * 由主类构造器中 modEventBus.register(ZGRCommonRegister.class) 注册。
 */
public class ZGRCommonRegister {
    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        ZGRRegistries.register(event);
    }
    @SubscribeEvent
    public static void registerZombieTypes(RegisterEvent event) {
        event.register(ZGRRegistries.Keys.ZOMBIE_TYPES_KEY, ZGRZombieTypes::register);
        event.register(Registries.SOUND_EVENT, ZGRSoundEvents::register);
    }
}
