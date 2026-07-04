package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZombieSpawnHandler {

    private static final String GUARD_CLASS_NAME = "tallestegg.guardvillagers.entities.Guard";

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

        if (zombie.getSpawnType() == MobSpawnType.CONVERSION) {
            ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(zombie, typeId);
        } else {
            ZombieTypeManager.initializeZombie(zombie, typeId);
        }

    }

    private static ResourceLocation selectType(Zombie zombie, MobSpawnType spawnType) {
        ZombieSpawnChooser.SpawnType chooserType;
        if (spawnType != null) {
            chooserType = switch (spawnType) {
                case CONVERSION -> {
                    // 村民感染走普通池，溺尸转化走溺尸池
                    if (zombie instanceof ZombieVillager) {
                        yield ZombieSpawnChooser.SpawnType.NORMAL;
                    }
                    yield ZombieSpawnChooser.SpawnType.DROWNED;
                }
                default -> ZombieSpawnChooser.SpawnType.NORMAL;
            };
        } else {
            chooserType = ZombieSpawnChooser.SpawnType.NORMAL;
        }

        ZombieType type = ZGRGame.getGameProperty().getStageProperty(zombie.getServer()).zombieSpawnChooser.randomType(chooserType);
        return type != null ? type.getId() : ZGRZombieTypes.DUMMY.getId();
    }
}
