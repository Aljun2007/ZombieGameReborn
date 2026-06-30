package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZombieSpawnHandler {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }

        if (zombie.level().isClientSide) {
            return;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data.isTypeInitialized()) {
            return; // 已初始化则跳过
        }
        ResourceLocation typeId = ZGRZombieAttributesAPI.getTypeID(data);
        if (zombie.getSpawnType() != null) {
            typeId = selectTypeBySpawnReason(zombie.getSpawnType());
        }
        ZombieTypeManager.initializeZombie(zombie, typeId);
    }

    @SubscribeEvent
    public static void onEntityConvert(LivingConversionEvent.Post event) {
        if (!(event.getOutcome() instanceof Zombie zombie)) {
            return;
        }

        if (zombie.level().isClientSide) {
            return;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data.isTypeInitialized()) {
            return; // 已初始化则跳过
        }
        ResourceLocation typeId = ZGRZombieAttributesAPI.getTypeID(data);
        if (zombie.getSpawnType() != null) {
            typeId = selectTypeBySpawnReason(zombie.getSpawnType());
        }
        ZombieTypeManager.initializeZombie(zombie, typeId);
    }


    //这个是以后处理僵尸生成类型的，目前只处理了刷怪蛋和命令生成

    private static ResourceLocation selectTypeBySpawnReason(MobSpawnType spawnType) {
        return switch (spawnType) {
            case SPAWN_EGG, COMMAND, CONVERSION -> ZGRZombieTypes.IDs.VANILLA_ID;
            default -> ZGRZombieTypes.IDs.VANILLA_ID;
        };
    }
}
