package com.aljun.zombiegamereborn.register;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * 僵尸类型注册表
 * 用于注册和管理所有僵尸类型
 */
public class ZGRRegistries {
    public static Supplier<IForgeRegistry<ZombieType>> ZOMBIE_TYPE;

    public static void register(NewRegistryEvent event) {
        ZOMBIE_TYPE = event.create(
                new RegistryBuilder<ZombieType>()
                        .setName(ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "zombie_types"))
                        .setIDRange(0, Integer.MAX_VALUE - 1)
                        .hasTags()
                        .disableSaving()
        );
    }

    public static class Keys {
        public static final ResourceKey<Registry<ZombieType>> ZOMBIE_TYPES_KEY = key("zombie_types");

        private static <T> ResourceKey<Registry<T>> key(String name) {
            return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, name));
        }
    }


}