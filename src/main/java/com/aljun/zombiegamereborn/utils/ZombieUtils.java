package com.aljun.zombiegamereborn.utils;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.logging.Level;

import static net.minecraft.world.level.Level.END;
import static net.minecraft.world.level.Level.NETHER;

public class ZombieUtils {

    public static boolean isZombieVeryCloseToTarget(@NotNull Zombie zombie, LivingEntity entity) {
        return zombie.distanceToSqr(entity) < ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR;
    }

    public static boolean isTargetLegal(@Nullable Entity entity) {
        if (entity == null) return false;
        boolean b = true;
        if (entity instanceof Player player) {
            b = !player.isCreative() && !player.isSpectator();
        }
        return b && entity.isAlive();
    }
    @SuppressWarnings("all")
    public static boolean zombieAttackableEntity(LivingEntity livingEntity) {
        if (!isTargetLegal(livingEntity)) return false;
        if (livingEntity instanceof Player) return true;
        if (livingEntity instanceof IronGolem) return true;
        if (livingEntity instanceof AbstractVillager) return true;
        if (livingEntity instanceof Turtle turtle&& turtle.isBaby()) return true;
        return false;
    }

    public static int threatLevel(LivingEntity entity) {
        if (entity instanceof Player) return 0;
        if (entity instanceof AbstractVillager || entity instanceof IronGolem) return 1;
        if (entity instanceof Turtle) return 2;
        return 3;
    }

    public static ItemStack randomPathBlock(ServerLevel level, BlockPos pos) {
        var random = RandomUtils.RANDOM;
        var dimension = level.dimension();

        // ===== 下界 =====
        if (dimension.equals(NETHER)) {
            return switch (random.nextInt(5)) {
                case 0 -> new ItemStack(Items.NETHERRACK);
                case 1 -> new ItemStack(Items.BLACKSTONE);
                case 2 -> new ItemStack(Items.BASALT);
                case 3 -> new ItemStack(Items.CRIMSON_PLANKS);
                case 4 -> new ItemStack(Items.WARPED_PLANKS);
                default -> new ItemStack(Items.NETHERRACK);
            };
        }

        // ===== 末地 =====
        if (dimension.equals(END)) {
            return new ItemStack(Items.END_STONE);
        }

        // ===== 主世界 =====
        var biomeKey = level.getBiome(pos).unwrapKey();
        if (biomeKey.isEmpty()) {
            return new ItemStack(Items.COBBLESTONE);
        }

        var biome = biomeKey.get();

        // ===== 沙漠系 =====
        if (biome == Biomes.DESERT || biome == Biomes.BADLANDS || biome == Biomes.ERODED_BADLANDS || biome == Biomes.WOODED_BADLANDS) {
            return switch (random.nextInt(4)) {
                case 0, 1 -> new ItemStack(Items.SANDSTONE);
                case 2 -> new ItemStack(Items.COBBLESTONE);
                default -> new ItemStack(Items.OAK_PLANKS);
            };
        }

        // ===== 海洋系 =====
        if (biome == Biomes.OCEAN || biome == Biomes.COLD_OCEAN || biome == Biomes.FROZEN_OCEAN ||
                biome == Biomes.DEEP_OCEAN || biome == Biomes.DEEP_COLD_OCEAN || biome == Biomes.DEEP_FROZEN_OCEAN ||
                biome == Biomes.LUKEWARM_OCEAN || biome == Biomes.DEEP_LUKEWARM_OCEAN || biome == Biomes.WARM_OCEAN) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.PRISMARINE);
                default -> new ItemStack(Items.OAK_PLANKS);
            };
        }

        // ===== 雪地系 =====
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA || biome == Biomes.SNOWY_BEACH ||
                biome == Biomes.FROZEN_RIVER || biome == Biomes.FROZEN_PEAKS || biome == Biomes.JAGGED_PEAKS ||
                biome == Biomes.SNOWY_SLOPES || biome == Biomes.ICE_SPIKES || biome == Biomes.GROVE) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.SPRUCE_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 海洋系 =====
        // 海洋系已在上方处理

        // ===== 针叶林系 =====
        if (biome == Biomes.TAIGA || biome == Biomes.OLD_GROWTH_PINE_TAIGA || biome == Biomes.OLD_GROWTH_SPRUCE_TAIGA) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.SPRUCE_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 白桦林系 =====
        if (biome == Biomes.BIRCH_FOREST || biome == Biomes.OLD_GROWTH_BIRCH_FOREST) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.BIRCH_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 黑森林系 =====
        if (biome == Biomes.DARK_FOREST) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.DARK_OAK_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 丛林系 =====
        if (biome == Biomes.JUNGLE || biome == Biomes.BAMBOO_JUNGLE || biome == Biomes.SPARSE_JUNGLE) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.JUNGLE_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 沼泽系 =====
        if (biome == Biomes.SWAMP || biome == Biomes.MANGROVE_SWAMP) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.OAK_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 草甸/花海系 =====
        if (biome == Biomes.MEADOW || biome == Biomes.FLOWER_FOREST) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.OAK_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 樱花系 =====
        if (biome == Biomes.CHERRY_GROVE) {
            return switch (random.nextInt(3)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.CHERRY_PLANKS);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 其他（默认） =====
        return switch (random.nextInt(3)) {
            case 0 -> new ItemStack(Items.COBBLESTONE);
            case 1 -> new ItemStack(Items.OAK_PLANKS);
            default -> new ItemStack(Items.STONE);
        };
    }

}
