package com.aljun.zombiegamereborn.register;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * 僵尸类型注册表
 * 用于注册和管理所有僵尸类型
 */
public class ZGRRegistries {
    public static Supplier<Registry<ZombieType>> ZOMBIE_TYPE;

    public static void register(NewRegistryEvent event) {
        Registry<ZombieType> registry = event.create(
                new RegistryBuilder<ZombieType>(Keys.ZOMBIE_TYPES_KEY)
        );
        ZOMBIE_TYPE = () -> registry;
    }

    public static class Keys {
        public static final ResourceKey<Registry<ZombieType>> ZOMBIE_TYPES_KEY = key("zombie_types");

        private static <T> ResourceKey<Registry<T>> key(String name) {
            return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, name));
        }
    }


}
