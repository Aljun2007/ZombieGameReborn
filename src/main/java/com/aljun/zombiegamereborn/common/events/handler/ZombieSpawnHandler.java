package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
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

        if (zombie instanceof ZombifiedPiglin) {
            if (zombie.getMainHandItem().is(Items.CROSSBOW)) {
                typeId = ZGRZombieTypes.CROSSBOW_ATTACKER.getId();
            }
        }

        if (zombie.getSpawnType() == MobSpawnType.CONVERSION) {
            ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(zombie, typeId);
        } else if (zombie.getSpawnType() == null && zombie instanceof ZombifiedPiglin) {
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
                    if (zombie instanceof ZombieVillager || zombie instanceof ZombifiedPiglin) {
                        yield ZombieSpawnChooser.SpawnType.NORMAL;
                    }
                    yield ZombieSpawnChooser.SpawnType.DROWNED;
                }
                default -> ZombieSpawnChooser.SpawnType.NORMAL;
            };
        } else {
            chooserType = ZombieSpawnChooser.SpawnType.NORMAL;
        }
        if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isLoaded()) {
            if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(zombie.level().getServer()) && zombie.level().dayTime() >= 12000L) {
                chooserType = ZombieSpawnChooser.SpawnType.BLOOD_MOON;
            }
        }
        ZombieType type = ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).zombieSpawnChooser.randomType(chooserType);
        return type != null ? type.getId() : ZGRZombieTypes.DUMMY.getId();
    }
}
