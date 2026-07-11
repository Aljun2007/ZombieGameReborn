package com.aljun.zombiegamereborn.common.player;

import com.aljun.zombiegamereborn.common.player.capability.PlayerDataProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReginalStageDetector {
    // ========== 常量 ==========

    private static final int REGION_SIZE = 64;
    private static final int SEARCH_RADIUS = 3;
    private static final double MAX_PLAYER_DISTANCE = 256.0;

    /**
     * 区域存活天数缓存的过期间隔（tick 数）
     */
    private static final long CACHE_DURATION_TICKS = 100;

    // ========== 缓存数据结构 ==========

    /**
     * 区域平均存活天数缓存：大格坐标 → 带时间戳的缓存条目
     */
    private static final Map<String, RegionCacheData> REGION_CACHE = new ConcurrentHashMap<>();

    private static volatile double SERVER_AVERAGE_SURVIVED_DAY = 0.0;
    private static volatile long SERVER_AVERAGE_UPDATE_TIME = -1;

    // ========== 核心方法 ==========

    /**
     * 获取某个位置的区域平均存活天数。
     * 检查缓存时间戳，超过 CACHE_DURATION_TICKS 自动重算。
     */
    public static double get(ServerLevel level, BlockPos pos) {
        String regionKey = getRegionKey(pos);
        long currentGameTime = level.getGameTime();

        RegionCacheData cached = REGION_CACHE.get(regionKey);
        if (cached != null && (currentGameTime - cached.gameTime) < CACHE_DURATION_TICKS) {
            return cached.survivedDay;
        }

        return calculateRegionSurvivedDay(level, pos, currentGameTime);
    }

    /**
     * 获取全体在线玩家的平均存活天数（含当天时间小数部分）
     * 区别于 get() 的区域加权，这个是纯算术平均，无区域概念
     */
    public static double getGlobalAverage(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return 0.0;

        return getServerAverageSurvivedDay(overworld, overworld.getGameTime());
    }

    /**
     * 计算大格区域的平均存活天数并缓存结果
     */
    private static double calculateRegionSurvivedDay(ServerLevel level, BlockPos pos, long currentGameTime) {
        BlockPos regionCenter = getRegionCenter(pos);
        var players = level.players();

        if (players.isEmpty()) {
            double fallback = getServerAverageSurvivedDay(level, currentGameTime);
            REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(fallback, List.of(), currentGameTime));
            return fallback;
        }

        // 收集所有玩家影响
        List<PlayerInfluence> influences = new ArrayList<>();
        Vec3 centerVec = Vec3.atCenterOf(regionCenter);

        for (ServerPlayer player : players) {
            double distance = centerVec.distanceTo(player.position());
            if (distance > MAX_PLAYER_DISTANCE) continue;

            double playerSurvivedDay = getPlayerSurvivedDay(player, level);
            double weight = 1.0 / (distance + 1.0);
            influences.add(new PlayerInfluence(player, distance, weight, playerSurvivedDay));
        }

        // 按距离排序，只取最近的 SEARCH_RADIUS 个
        influences.sort(Comparator.comparingDouble(PlayerInfluence::distance));
        if (influences.size() > SEARCH_RADIUS) {
            influences = influences.subList(0, SEARCH_RADIUS);
        }

        // 没有玩家在影响范围内 → 回退到服务器平均存活天数
        if (influences.isEmpty()) {
            double fallback = getServerAverageSurvivedDay(level, currentGameTime);
            REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(fallback, List.of(), currentGameTime));
            return fallback;
        }

        // 加权平均计算区域存活天数
        double totalWeight = 0.0;
        double weightedSurvivedDay = 0.0;

        for (PlayerInfluence inf : influences) {
            weightedSurvivedDay += inf.playerSurvivedDay() * inf.weight();
            totalWeight += inf.weight();
        }

        double result = totalWeight > 0 ? weightedSurvivedDay / totalWeight : 0.0;

        // 缓存结果
        REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(result, influences, currentGameTime));
        return result;
    }

    /**
     * 获取服务器平均存活天数（带时间戳缓存）
     */
    private static double getServerAverageSurvivedDay(ServerLevel level, long currentGameTime) {
        if (currentGameTime - SERVER_AVERAGE_UPDATE_TIME < CACHE_DURATION_TICKS) {
            return SERVER_AVERAGE_SURVIVED_DAY;
        }
        return computeServerAverageSurvivedDay(level, currentGameTime);
    }

    /**
     * 计算全体在线玩家的平均存活天数并缓存
     */
    private static double computeServerAverageSurvivedDay(ServerLevel level, long currentGameTime) {
        var players = level.players();
        if (players.isEmpty()) {
            SERVER_AVERAGE_SURVIVED_DAY = 0.0;
        } else {
            double sum = 0.0;
            for (ServerPlayer player : players) {
                sum += getPlayerSurvivedDay(player, level);
            }
            SERVER_AVERAGE_SURVIVED_DAY = sum / players.size();
        }
        SERVER_AVERAGE_UPDATE_TIME = currentGameTime;
        return SERVER_AVERAGE_SURVIVED_DAY;
    }

    /**
     * 从玩家的 Capability 中读取存活天数，并叠加主世界当前 timeOfDay 的小数部分
     */
    private static double getPlayerSurvivedDay(ServerPlayer player, ServerLevel level) {
        var data = player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
        long day = data.getSurvivedDay();
        double timeFraction = Math.floorMod(level.getDayTime(), 24000L) / 24000.0;
        return (double) day + timeFraction;
    }

    public static void setPlayerSurvivedDay(ServerPlayer player, long survivedDay) {
        var data = player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
        data.setSurvivedDay(survivedDay);
    }

    // ========== 大格坐标工具 ==========

    private static String getRegionKey(BlockPos pos) {
        int regionX = Math.floorDiv(pos.getX(), REGION_SIZE);
        int regionZ = Math.floorDiv(pos.getZ(), REGION_SIZE);
        return regionX + "," + regionZ;
    }

    private static BlockPos getRegionCenter(BlockPos pos) {
        int regionX = Math.floorDiv(pos.getX(), REGION_SIZE);
        int regionZ = Math.floorDiv(pos.getZ(), REGION_SIZE);
        int centerX = regionX * REGION_SIZE + REGION_SIZE / 2;
        int centerZ = regionZ * REGION_SIZE + REGION_SIZE / 2;
        return new BlockPos(centerX, 0, centerZ);
    }

    public static BlockPos getRegionStart(BlockPos pos) {
        int regionX = Math.floorDiv(pos.getX(), REGION_SIZE);
        int regionZ = Math.floorDiv(pos.getZ(), REGION_SIZE);
        return new BlockPos(regionX * REGION_SIZE, 0, regionZ * REGION_SIZE);
    }

    // ========== 缓存管理 ==========

    /**
     * 清空指定大格的缓存（调试/命令用）
     */
    public static void invalidate(BlockPos pos) {
        REGION_CACHE.remove(getRegionKey(pos));
    }

    // ========== 调试方法 ==========

    /**
     * 获取缓存中的平均存活天数，不存在返回 -1.0
     */
    public static double getCached(BlockPos pos) {
        RegionCacheData data = REGION_CACHE.get(getRegionKey(pos));
        return data != null ? data.survivedDay : -1.0;
    }

    /**
     * 获取缓存中的玩家影响列表
     */
    public static List<PlayerInfluence> getPlayerInfluence(BlockPos pos) {
        RegionCacheData data = REGION_CACHE.get(getRegionKey(pos));
        return data != null ? data.influences : List.of();
    }

    public static int getCacheSize() {
        return REGION_CACHE.size();
    }

    // ========== 内部类 ==========

    /**
     * 缓存条目：平均存活天数 + 玩家影响列表 + 计算时的 gameTime
     */
    private record RegionCacheData(double survivedDay, List<PlayerInfluence> influences, long gameTime) {
    }

    public record PlayerInfluence(ServerPlayer player, double distance, double weight, double playerSurvivedDay) {
    }
}
