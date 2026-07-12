package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.aljun.zombiegamereborn.common.player.capability.PlayerDataProvider;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ZGRPlayerAPI {

    public static IPlayerData getPlayerData(ServerPlayer player) {
        return player.getCapability(PlayerDataProvider.PLAYER_DATA).orElse(null);
    }

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
}
