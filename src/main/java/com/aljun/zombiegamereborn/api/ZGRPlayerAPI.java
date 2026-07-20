package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.aljun.zombiegamereborn.common.player.capability.PlayerDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ZGRPlayerAPI {

    public static IPlayerData getPlayerData(ServerPlayer player) {
        return player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
    }

    private static final String PREFIX = "zgr_cap_";
    private static final String KEY_SURVIVED_DAY = PREFIX + "survived_day";
    private static final String KEY_UNDERGROUND_DAY = PREFIX + "underground_day";
    private static final String KEY_UNDERGROUND_GAME_TIME = PREFIX + "underground_game_time";
    private static final String KEY_LAST_ESTIMATED_DAY = PREFIX + "last_estimated_day";
    private static final String KEY_TOTAL_ZOMBIE_KILLS = PREFIX + "total_zombie_kills";

    public static long getSurvivedDay(IPlayerData data) {
        return data.getSurvivedDay();
    }

    public static void setSurvivedDay(IPlayerData data, long day) {
        data.setSurvivedDay(day);
    }

    public static long getUndergroundDay(IPlayerData data) {
        return data.getUndergroundDay();
    }

    public static void setUndergroundDay(IPlayerData data, long day) {
        data.setUndergroundDay(day);
    }

    public static long getUndergroundGameTime(IPlayerData data) {
        return data.getUndergroundGameTime();
    }

    public static void setUndergroundGameTime(IPlayerData data, long gameTime) {
        data.setUndergroundGameTime(gameTime);
    }

    public static long getLastEstimatedDay(IPlayerData data) {
        return data.getLastEstimatedDay();
    }

    public static void setLastEstimatedDay(IPlayerData data, long day) {
        data.setLastEstimatedDay(day);
    }

    public static long getTotalZombieKills(IPlayerData data) {
        return data.getTotalZombieKills();
    }

    public static void setTotalZombieKills(IPlayerData data, long kills) {
        data.setTotalZombieKills(kills);
    }

    public static void saveToPersistentData(ServerPlayer player) {
        IPlayerData data = getPlayerData(player);
        if (data == null) return;
        CompoundTag persistent = player.getPersistentData();
        persistent.putLong(KEY_SURVIVED_DAY, data.getSurvivedDay());
        persistent.putLong(KEY_UNDERGROUND_DAY, data.getUndergroundDay());
        persistent.putLong(KEY_UNDERGROUND_GAME_TIME, data.getUndergroundGameTime());
        persistent.putLong(KEY_LAST_ESTIMATED_DAY, data.getLastEstimatedDay());
        persistent.putLong(KEY_TOTAL_ZOMBIE_KILLS, data.getTotalZombieKills());
    }

    public static void loadFromPersistentData(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(KEY_SURVIVED_DAY)) return;

        IPlayerData data = getPlayerData(player);
        if (data == null) return;

        data.setSurvivedDay(Math.max(1L, persistent.getLong(KEY_SURVIVED_DAY)));
        data.setUndergroundDay(Math.max(0L, persistent.getLong(KEY_UNDERGROUND_DAY)));
        data.setUndergroundGameTime(Math.max(0L, persistent.getLong(KEY_UNDERGROUND_GAME_TIME)));
        data.setLastEstimatedDay(Math.max(0L, persistent.getLong(KEY_LAST_ESTIMATED_DAY)));
        data.setTotalZombieKills(Math.max(0L, persistent.getLong(KEY_TOTAL_ZOMBIE_KILLS)));
    }

    public static void copyData(ServerPlayer from, ServerPlayer to) {
        IPlayerData fromData = getPlayerData(from);
        IPlayerData toData = getPlayerData(to);
        if (fromData == null || toData == null) return;

        toData.setSurvivedDay(fromData.getSurvivedDay());
        toData.setUndergroundDay(fromData.getUndergroundDay());
        toData.setUndergroundGameTime(fromData.getUndergroundGameTime());
        toData.setLastEstimatedDay(fromData.getLastEstimatedDay());
        toData.setTotalZombieKills(fromData.getTotalZombieKills());
    }
}
