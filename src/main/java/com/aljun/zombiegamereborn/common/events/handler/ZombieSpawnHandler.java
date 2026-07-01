package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
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
            return;
        }
        ResourceLocation typeId = selectType(zombie, zombie.getSpawnType());
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
            return;
        }
        ResourceLocation typeId = selectType(zombie, zombie.getSpawnType());
        ZombieTypeManager.initializeZombie(zombie, typeId);
    }

    private static ResourceLocation selectType(Zombie zombie, MobSpawnType spawnType) {
        ZombieSpawnChooser.SpawnType chooserType;
        if (spawnType != null) {
            chooserType = switch (spawnType) {
                case CONVERSION -> ZombieSpawnChooser.SpawnType.DROWNED;
                default -> ZombieSpawnChooser.SpawnType.NORMAL;
            };
        } else {
            chooserType = ZombieSpawnChooser.SpawnType.NORMAL;
        }

        ZombieType type = ZGRGame.getGameProperty()
                .getStageProperty(zombie.getServer())
                .zombieSpawnChooser
                .randomType(chooserType);
        return type != null ? type.getId() : ZGRZombieTypes.DUMMY.getId();
    }
}
