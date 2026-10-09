package com.aljun.zombiegamereborn.utils;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static net.minecraft.world.level.Level.END;
import static net.minecraft.world.level.Level.NETHER;

public class ZombieUtils {

    public static boolean isZombieVeryCloseToTarget(@NotNull Zombie zombie, LivingEntity entity) {
        return zombie.distanceToSqr(entity) < ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR;
    }



    @SuppressWarnings("all")
    public static boolean attackableEntity(LivingEntity livingEntity) {
        if (!isTargetLegal(livingEntity)) return false;
        if (livingEntity instanceof Player) return true;
        if (livingEntity instanceof IronGolem) return true;
        if (livingEntity instanceof AbstractVillager) return true;
        if (livingEntity instanceof AbstractPiglin) return true;
        if (livingEntity instanceof Hoglin) return true;
        if (livingEntity instanceof Turtle turtle && turtle.isBaby()) return true;
        return false;
    }

    @SuppressWarnings("all")
    public static boolean zombieAttackableEntity(LivingEntity livingEntity) {
        if (!isTargetLegal(livingEntity)) return false;
        if (livingEntity instanceof Player) return true;
        if (livingEntity instanceof IronGolem) return true;
        if (livingEntity instanceof AbstractVillager) return true;
        if (livingEntity instanceof Turtle turtle && turtle.isBaby()) return true;
        return false;
    }

    @SuppressWarnings("all")
    public static boolean zombifiedPiglinAttackableEntity(LivingEntity livingEntity) {
        if (!isTargetLegal(livingEntity)) return false;
        if (livingEntity instanceof Player) return true;
        if (livingEntity instanceof AbstractPiglin) return true;
        if (livingEntity instanceof Hoglin) return true;
        if (livingEntity instanceof Turtle turtle && turtle.isBaby()) return true;
        return false;
    }

    public static boolean isTargetLegal(@Nullable Entity entity) {
        if (entity == null) return false;
        boolean b = true;
        if (entity instanceof Player player) {
            b = !player.isCreative() && !player.isSpectator();
        }
        return b && entity.isAlive();
    }

    public static int threatLevel(LivingEntity entity) {
        if (entity instanceof Player) return 0;
        if (entity instanceof AbstractVillager) return 2;
        if (entity instanceof Turtle) return 3;
        return 1;
    }

    public static ItemStack randomPathBlock(ServerLevel level, BlockPos pos) {
        var random = RandomUtils.RANDOM;
        var dimension = level.dimension();

        // ===== 下界 =====
        if (dimension.equals(NETHER)) {
            return switch (random.nextInt(5)) {
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
        var biome = level.getBiome(pos);

        // ===== 沙漠系 =====
        if (biome.is(Biomes.DESERT) || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS) || biome.is(Biomes.WOODED_BADLANDS)) {
            return switch (random.nextInt(4)) {
                case 0, 1 -> new ItemStack(Items.SANDSTONE);
                case 2 -> new ItemStack(Items.COBBLESTONE);
                default -> new ItemStack(Items.OAK_PLANKS);
            };
        }

        // ===== 海洋系 =====
        if (biome.is(Biomes.OCEAN) || biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.FROZEN_OCEAN) ||
                biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN) ||
                biome.is(Biomes.LUKEWARM_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN) || biome.is(Biomes.WARM_OCEAN)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.PRISMARINE);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.OAK_PLANKS);
            };
        }

        // ===== 雪地系 =====
        if (biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.SNOWY_TAIGA) || biome.is(Biomes.SNOWY_BEACH) ||
                biome.is(Biomes.FROZEN_RIVER) || biome.is(Biomes.FROZEN_PEAKS) || biome.is(Biomes.JAGGED_PEAKS) ||
                biome.is(Biomes.SNOWY_SLOPES) || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.GROVE)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.SPRUCE_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 针叶林系 =====
        if (biome.is(Biomes.TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.SPRUCE_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 白桦林系 =====
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.BIRCH_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 黑森林系 =====
        if (biome.is(Biomes.DARK_FOREST)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.DARK_OAK_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 丛林系 =====
        if (biome.is(Biomes.JUNGLE) || biome.is(Biomes.BAMBOO_JUNGLE) || biome.is(Biomes.SPARSE_JUNGLE)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.JUNGLE_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 沼泽系 =====
        if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.OAK_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 草甸/花海系 =====
        if (biome.is(Biomes.MEADOW) || biome.is(Biomes.FLOWER_FOREST)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.OAK_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 樱花系 =====
        if (biome.is(Biomes.CHERRY_GROVE)) {
            return switch (random.nextInt(4)) {
                case 0 -> new ItemStack(Items.COBBLESTONE);
                case 1 -> new ItemStack(Items.CHERRY_PLANKS);
                case 2 -> new ItemStack(Items.DIRT);
                default -> new ItemStack(Items.STONE);
            };
        }

        // ===== 其他（默认） =====
        return switch (random.nextInt(4)) {
            case 0 -> new ItemStack(Items.COBBLESTONE);
            case 1 -> new ItemStack(Items.OAK_PLANKS);
            case 2 -> new ItemStack(Items.DIRT);
            default -> new ItemStack(Items.STONE);
        };
    }

    public static boolean isCuring(Zombie zombie) {
        if (zombie instanceof ZombieVillager villager) {
            return villager.isConverting() || villager.hasEffect(MobEffects.WEAKNESS);
        } else return false;
    }

}
