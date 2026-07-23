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
    private static final int REGION_SIZE = 64;
    private static final int SEARCH_RADIUS = 3;
    private static final long CACHE_DURATION_TICKS = 100;

    private static final Map<String, RegionCacheData> REGION_CACHE = new ConcurrentHashMap<>();
    private static volatile double SERVER_AVERAGE_SURVIVED_DAY = 1.0;
    private static volatile long SERVER_AVERAGE_UPDATE_TIME = -1;

    public static double get(ServerLevel level, BlockPos pos) {
        String regionKey = getRegionKey(pos);
        long currentGameTime = level.getGameTime();

        RegionCacheData cached = REGION_CACHE.get(regionKey);
        if (cached != null && (currentGameTime - cached.gameTime) < CACHE_DURATION_TICKS) {
            return cached.survivedDay;
        }
        return calculateRegionSurvivedDay(level, pos, currentGameTime);
    }

    public static double getGlobalAverage(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return getServerAverageSurvivedDay(overworld, overworld.getGameTime());
    }

    private static double calculateRegionSurvivedDay(ServerLevel level, BlockPos pos, long currentGameTime) {
        BlockPos regionCenter = getRegionCenter(pos);
        var players = level.players();

        if (players.isEmpty()) {
            double fallback = getServerAverageSurvivedDay(level, currentGameTime);
            REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(fallback, List.of(), currentGameTime));
            return fallback;
        }

        List<PlayerInfluence> influences = new ArrayList<>();
        Vec3 centerVec = Vec3.atCenterOf(regionCenter);

        for (ServerPlayer player : players) {
            double distance = centerVec.distanceTo(player.position());
            double playerSurvivedDay = getPlayerSurvivedDay(player, level);
            double weight = 1.0 / (distance + 1.0);
            influences.add(new PlayerInfluence(player, distance, weight, playerSurvivedDay));
        }

        influences.sort(Comparator.comparingDouble(PlayerInfluence::distance));
        if (influences.size() > SEARCH_RADIUS) {
            influences = influences.subList(0, SEARCH_RADIUS);
        }

        if (influences.isEmpty()) {
            double fallback = getServerAverageSurvivedDay(level, currentGameTime);
            REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(fallback, List.of(), currentGameTime));
            return fallback;
        }

        double totalWeight = 0.0;
        double weightedSurvivedDay = 0.0;

        for (PlayerInfluence inf : influences) {
            weightedSurvivedDay += inf.playerSurvivedDay() * inf.weight();
            totalWeight += inf.weight();
        }

        double result = totalWeight > 0 ? weightedSurvivedDay / totalWeight : 0.0;
        REGION_CACHE.put(getRegionKey(pos), new RegionCacheData(result, influences, currentGameTime));
        return result;
    }

    private static double getServerAverageSurvivedDay(ServerLevel level, long currentGameTime) {
        if (currentGameTime - SERVER_AVERAGE_UPDATE_TIME < CACHE_DURATION_TICKS) {
            return SERVER_AVERAGE_SURVIVED_DAY;
        }
        return computeServerAverageSurvivedDay(level, currentGameTime);
    }

    private static double computeServerAverageSurvivedDay(ServerLevel level, long currentGameTime) {
        var players = level.players();
        if (players.isEmpty()) {
            SERVER_AVERAGE_SURVIVED_DAY = 1.0;
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

    private static double getPlayerSurvivedDay(ServerPlayer player, ServerLevel level) {
        var data = player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
        if (data == null) return 1.0;
        long day = data.getSurvivedDay();
        double timeFraction = Math.floorMod(level.getDayTime(), 24000L) / 24000.0;
        return (double) day + timeFraction;
    }

    public static void setPlayerSurvivedDay(ServerPlayer player, long survivedDay) {
        var data = player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
        if (data == null) return;
        data.setSurvivedDay(survivedDay);
    }

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

    public static void invalidate(BlockPos pos) {
        REGION_CACHE.remove(getRegionKey(pos));
    }

    public static double getCached(BlockPos pos) {
        RegionCacheData data = REGION_CACHE.get(getRegionKey(pos));
        return data != null ? data.survivedDay : -1.0;
    }

    public static List<PlayerInfluence> getPlayerInfluence(BlockPos pos) {
        RegionCacheData data = REGION_CACHE.get(getRegionKey(pos));
        return data != null ? data.influences : List.of();
    }

    private record RegionCacheData(double survivedDay, List<PlayerInfluence> influences, long gameTime) {
    }

    public record PlayerInfluence(ServerPlayer player, double distance, double weight, double playerSurvivedDay) {
    }
}
