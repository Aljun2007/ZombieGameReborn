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
import net.minecraft.world.entity.monster.Drowned;
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
        // 1. 最高优先级：村民感染且配置为不破坏方块，直接返回增强原版类型
        if (spawnType == MobSpawnType.CONVERSION
                && zombie instanceof ZombieVillager
                && !ZGRGame.getGameProperty().infectedVillagerCanBreakBlocks) {
            return ZGRZombieTypes.ENHANCED_VANILLA.getId();
        }

        // 2. 确定基础池子类型
        ZombieSpawnChooser.SpawnType chooserType;

        if (spawnType == MobSpawnType.CONVERSION) {
            // 转换生成：猪灵或村民走普通池子，其他（如溺尸转换）走溺尸池子
            if (zombie instanceof ZombieVillager || zombie instanceof ZombifiedPiglin) {
                chooserType = ZombieSpawnChooser.SpawnType.NORMAL;
            } else {
                chooserType = ZombieSpawnChooser.SpawnType.DROWNED;
            }
        } else {
            // 自然生成或其他情况：默认走普通池子
            if (zombie instanceof Drowned) {
                chooserType = ZombieSpawnChooser.SpawnType.DROWNED;
            } else {
                chooserType = ZombieSpawnChooser.SpawnType.NORMAL;
            }
        }

        // 3. 次高优先级：血月条件覆盖（如果模组加载且满足血月+夜晚条件）
        if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isLoaded()) {
            if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(zombie.level().getServer())
                    && zombie.level().dayTime() >= 12000L) {
                if (zombie instanceof Drowned) {
                    chooserType = ZombieSpawnChooser.SpawnType.BLOOD_MOON_DROWNED;
                } else {
                    chooserType = ZombieSpawnChooser.SpawnType.BLOOD_MOON;
                }
            }
        }

        // 4. 从对应池子中随机选择具体变种
        ZombieType type = ZGRGame.getGameProperty()
                .getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition())
                .zombieSpawnChooser.randomType(chooserType);

        // 5. 猪灵不用弓
        if (zombie instanceof ZombifiedPiglin) {
            if (type.equals(ZGRZombieTypes.BOW_ATTACKER)) {
                type = ZGRZombieTypes.CROSSBOW_ATTACKER;
            }
        }
        return type != null ? type.getId() : ZGRZombieTypes.DUMMY.getId();
    }
}
