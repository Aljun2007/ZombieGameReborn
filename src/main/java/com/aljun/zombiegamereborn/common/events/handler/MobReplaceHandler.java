package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.MobReplacement;
import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraft.world.level.Level.END;
import static net.minecraft.world.level.Level.NETHER;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobReplaceHandler {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getLevel().dimension().equals(END)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (event.getEntity() instanceof Zombie ) return;
        if (event.getEntity() instanceof Zoglin) return;
        if (event.getEntity() instanceof Player) return;
        if (mob.getSpawnType() != MobSpawnType.NATURAL) return;

        ResourceLocation typeId = EntityType.getKey(mob.getType());
        MobReplacement.ReplaceableType action = ZGRGame.getGameProperty().mobReplacement.get(typeId);
        if (action == null) return;

        StageProperty stage = ZGRGame.getGameProperty().getGlobalStage(event.getEntity().getServer());

        if (action == MobReplacement.ReplaceableType.REMOVE) {
            if (RandomUtils.booleanByChance(stage.removeChance)) {
                event.setCanceled(true);
            }
            return;
        }

        // REPLACE → 概率判定
        if (!RandomUtils.booleanByChance(stage.replaceChance)) return;

        ResourceLocation lootTable = mob.getLootTable();

        // 疣猪 → 僵尸疣猪
        if (mob.getType() == EntityType.HOGLIN) {
            replace(event, mob, lootTable, EntityType.ZOGLIN);
            return;
        }

        if (AbstractVillager.class.isAssignableFrom(mob.getType().getBaseClass())) {
            replace(event, mob, lootTable, EntityType.ZOMBIE_VILLAGER);
            return;
        }

        Holder<Biome> biome = mob.level().getBiome(mob.blockPosition());

        if (biome.is(BiomeTags.IS_NETHER)) {
            replace(event, mob, lootTable, EntityType.ZOMBIFIED_PIGLIN);
        } else if (biome.is(Biomes.DESERT) || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS) || biome.is(Biomes.WOODED_BADLANDS)) {
            replace(event, mob, lootTable, EntityType.HUSK);
        } else {
            replace(event, mob, lootTable, EntityType.ZOMBIE);
        }
    }

    private static void replace(EntityJoinLevelEvent event, Mob original, ResourceLocation lootTable, EntityType<? extends Mob> type) {
        event.setCanceled(true);
        Mob entity = type.create(original.level());
        if (entity != null) {
            entity.moveTo(original.getX(), original.getY(), original.getZ(),
                    original.getYRot(), original.getXRot());
            if (entity instanceof Zombie zombie) {
                if (ZGRGame.getGameProperty().keepMobLootTable) {
                    IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
                    if (data != null) {
                        data.setCustomLootTable(lootTable);
                    }
                }
            }
            if (original.level() instanceof ServerLevel serverLevel) {
                entity.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(entity.blockPosition()),
                        MobSpawnType.NATURAL, null, null);
            }
            original.level().addFreshEntity(entity);
        }
    }
}
