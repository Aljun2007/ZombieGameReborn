package com.aljun.zombiegamereborn.common.player;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.game.DayTime;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.AdvancementHandler;
import com.aljun.zombiegamereborn.network.packet.TimeBroadcastPacket;
import com.aljun.zombiegamereborn.sounds.ZGRSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TimeBroadcast {

    private static final Map<UUID, PlayerBroadcastData> PLAYER_DATA_MAP = new ConcurrentHashMap<>();

    private static final int LEAVE_SURFACE_THRESHOLD_TICKS = 14400;
    public static final int UNDERGROUND_ESTIMATE_THRESHOLD = 36000; // 1.5天后模糊时间概念
    private static final int LOGIN_BROADCAST_DELAY = 40;

    public static void scheduleLoginBroadcast(ServerPlayer player) {
        if (player.level().isClientSide) return;
        UUID uuid = player.getGameProfile().getId();
        PlayerBroadcastData data = PLAYER_DATA_MAP.computeIfAbsent(uuid, k -> new PlayerBroadcastData());
        data.loginBroadcastDelay = LOGIN_BROADCAST_DELAY;
        ServerLevel overworld = player.server.overworld();
        long dayTime = overworld.getDayTime();
        data.lastDayTimePeriod = DayTime.fromDayTime(dayTime);
    }

    // ========== tick() 中 ==========

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide) return;

        UUID uuid = player.getGameProfile().getId();
        PlayerBroadcastData data = PLAYER_DATA_MAP.computeIfAbsent(uuid, k -> new PlayerBroadcastData());

        IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);
        ServerLevel overworld = player.server.overworld();
        long dayTime = overworld.getDayTime();
        long days = playerData.getSurvivedDay();
        long gameTime = overworld.getGameTime();

        boolean isInOverworld = player.level().dimension() == Level.OVERWORLD;
        boolean isOnSurface = isInOverworld && PlayerStatic.isOnSurfaceOfOverworld(player);
        boolean previouslyOnSurface = data.wasOnSurface;

        // === 登录延迟播报 ===
        if (data.loginBroadcastDelay > 0) {
            data.loginBroadcastDelay--;
            if (data.loginBroadcastDelay == 0) {
                if (!isInOverworld) {
                    ZGRNetwork.sendToClient(new TimeBroadcastPacket(), player);
                    return;
                }
                boolean hasClock = hasClock(player);
                if (hasClock) {
                    DayTime current = DayTime.fromDayTime(dayTime);
                    ZGRNetwork.sendToClient(new TimeBroadcastPacket(days, current, dayTime, true), player);
                } else if (isOnSurface) {
                    DayTime current = DayTime.fromDayTime(dayTime);
                    ZGRNetwork.sendToClient(new TimeBroadcastPacket(days, current, dayTime, false), player);
                } else {
                    long undergroundSince = readUndergroundGameTime(player);
                    if (undergroundSince > 0 && gameTime - undergroundSince >= UNDERGROUND_ESTIMATE_THRESHOLD) {
                        long estimated = calculateEstimatedDay(player, overworld, data);
                        ZGRNetwork.sendToClient(new TimeBroadcastPacket(estimated, true), player);
                    } else {
                        ZGRNetwork.sendToClient(new TimeBroadcastPacket(days, false), player);
                    }
                }
            }
        }

        // === 主世界地下数据追踪（仅用于估算存储） ===
        if (isInOverworld && !isOnSurface && previouslyOnSurface) {
            saveUndergroundEntry(player, days, gameTime);
        }
        if (isInOverworld && isOnSurface && !previouslyOnSurface) {
            clearUndergroundData(player);
        }

        // === 时刻过渡播报 ===
        DayTime currentDayTime = DayTime.fromDayTime(dayTime);

        boolean shouldBroadcastDawn = currentDayTime == DayTime.DAWN && data.lastDayTimePeriod != DayTime.DAWN;
        boolean shouldBroadcastSunset = currentDayTime == DayTime.EARLY_NIGHT && data.lastDayTimePeriod == DayTime.SUNSET;

        if (isInOverworld && (shouldBroadcastDawn || shouldBroadcastSunset)) {
            boolean hasClock = hasClock(player);
            if (isOnSurface) {
                if (shouldBroadcastDawn) {
                    playSoundForPlayer(player, ZGRSoundEvents.MORNING_ROAST, SoundSource.AMBIENT, 1.0f);
                } else {
                    playSoundForPlayer(player, ZGRSoundEvents.EVENING_HOWL, SoundSource.AMBIENT, 1.0f);
                }
                ZGRNetwork.sendToClient(new TimeBroadcastPacket(
                        days,
                        shouldBroadcastDawn ? DayTime.DAWN : DayTime.SUNSET,
                        dayTime,
                        hasClock
                ), player);
            } else if (hasClock) {
                triggerUndergroundAlarm(player);
            }
        }
        data.lastDayTimePeriod = currentDayTime;

        // === 长时间离开地表后返回播报 ===
        // 离开地表时（进入地下或异世界）记录时间
        if (!isOnSurface && previouslyOnSurface) {
            data.lastLeftSurfaceTime = gameTime;
        }

        // 返回地表时（从地下或异世界回来）检查是否足够久
        if (isOnSurface && !previouslyOnSurface) {
            if (data.lastLeftSurfaceTime != -1 && gameTime - data.lastLeftSurfaceTime >= LEAVE_SURFACE_THRESHOLD_TICKS) {
                triggerReturnBroadcast(player, days, currentDayTime, hasClock(player));
            }
        }

        data.wasOnSurface = isOnSurface;
    }

    // ========== calculateEstimatedDay() 中 ==========

    private static long calculateEstimatedDay(ServerPlayer player, ServerLevel overworld, PlayerBroadcastData data) {
        IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);
        long lastSurfaceDay = playerData.getUndergroundDay();
        long lastSurfaceGameTime = playerData.getUndergroundGameTime();
        long lastEstimatedDay = playerData.getLastEstimatedDay();
        long actualDay = playerData.getSurvivedDay();
        long currentGameTime = overworld.getGameTime();

        if (lastSurfaceDay <= 0) {
            lastSurfaceDay = actualDay;
            lastSurfaceGameTime = currentGameTime;
        }

        long elapsedDays = (currentGameTime - lastSurfaceGameTime) / 24000;
        long rawEstimate = lastSurfaceDay + elapsedDays;
        long ceiling = Math.min(actualDay, rawEstimate);
        long estimated = Math.max(lastEstimatedDay, ceiling);
        estimated = Math.max(estimated, 1);

        playerData.setLastEstimatedDay(estimated);
        return estimated;
    }

    private static void saveUndergroundEntry(ServerPlayer player, long currentDay, long gameTime) {
        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);
        if (data.getUndergroundDay() <= 0) {
            data.setUndergroundDay(currentDay);
            data.setUndergroundGameTime(gameTime);
            if (data.getLastEstimatedDay() <= 0) {
                data.setLastEstimatedDay(currentDay);
            }
        }
    }

    private static long readUndergroundGameTime(ServerPlayer player) {
        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);
        return data.getUndergroundGameTime();
    }

    private static void clearUndergroundData(ServerPlayer player) {
        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);
        data.setUndergroundDay(0L);
        data.setUndergroundGameTime(0L);
        data.setLastEstimatedDay(0L);
    }

    private static void playSoundForPlayer(ServerPlayer player, SoundEvent sound, SoundSource source, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, source, volume, 1.0f);
    }

    private static boolean hasClock(ServerPlayer player) {
        return player.getInventory().hasAnyOf(Set.of(Items.CLOCK));
    }

    private static void triggerUndergroundAlarm(ServerPlayer player) {
        Component chatMsg = buildChatComponent(player);
        ZGRNetwork.sendToClient(new TimeBroadcastPacket(chatMsg), player);
    }

    private static void triggerReturnBroadcast(ServerPlayer player, long days, DayTime dayTime, boolean hasClock) {
        ZGRNetwork.sendToClient(new TimeBroadcastPacket(days, dayTime, player.level().dayTime(), hasClock), player);
    }

    private static Component buildChatComponent(ServerPlayer player) {
        List<ItemStack> clocks = new ArrayList<>();
        if (player.getMainHandItem().is(Items.CLOCK)) {
            clocks.add(player.getMainHandItem());
        }
        if (player.getOffhandItem().is(Items.CLOCK)) {
            clocks.add(player.getOffhandItem());
        }
        ItemStack clockStack = clocks.isEmpty() ? new ItemStack(Items.CLOCK) : clocks.get(0);

        MutableComponent clockName = clockStack.getDisplayName().copy();
        clockName.withStyle(style -> style.withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(clockStack)
        )));

        return Component.translatable("message.zombiegamereborn.time_broadcast.alarm", clockName);
    }


    // ========== handleManualClockUse() 中 ==========

    public static void handleManualClockUse(ServerPlayer player) {
        if (player.level().isClientSide) return;

        if (player.level().dimension() != Level.OVERWORLD) {
            ZGRNetwork.sendToClient(new TimeBroadcastPacket(), player);
            return;
        }

        IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);

        ServerLevel overworld = player.server.overworld();
        long dayTime = overworld.getDayTime();
        long days = playerData.getSurvivedDay();
        DayTime currentDayTime = DayTime.fromDayTime(dayTime);

        triggerManualBroadcast(player, days, currentDayTime);
    }

    private static void triggerManualBroadcast(ServerPlayer player, long days, DayTime dayTime) {
        ZGRNetwork.sendToClient(new TimeBroadcastPacket(days, dayTime, player.level().dayTime(), true), player);
    }

    public static void resetAllData() {
        PLAYER_DATA_MAP.clear();
    }

    public static void resetPlayerData(UUID uuid) {
        PLAYER_DATA_MAP.remove(uuid);
    }

    private static class PlayerBroadcastData {
        DayTime lastDayTimePeriod = null;
        boolean wasOnSurface = false;
        long lastLeftSurfaceTime = -1;
        int loginBroadcastDelay = 0;
    }
}
