package com.aljun.zombiegamereborn.common.player;

import com.aljun.zombiegamereborn.common.game.DayTime;
import com.aljun.zombiegamereborn.common.game.TimeData;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
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

/**
 * 时间自动播报系统<br>
 * 黎明（进入 DAWN 第一刻）播报早晨音效，
 * 日落（离开 SUNSET 进入 EARLY_NIGHT 最后一刻）播报黄昏嚎叫，
 * 地下玩家背包有钟时触发闹钟。
 */
public class TimeBroadcast {

    private static final Map<UUID, PlayerBroadcastData> PLAYER_DATA_MAP = new ConcurrentHashMap<>();

    private static final int LEAVE_SURFACE_THRESHOLD_TICKS = 14400;
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

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide) return;

        UUID uuid = player.getGameProfile().getId();
        PlayerBroadcastData data = PLAYER_DATA_MAP.computeIfAbsent(uuid, k -> new PlayerBroadcastData());

        ServerLevel overworld = player.server.overworld();
        TimeData timeData = TimeData.get(overworld);
        long dayTime = overworld.getDayTime();
        long days = timeData.getDays();

        if (data.loginBroadcastDelay > 0) {
            data.loginBroadcastDelay--;
            if (data.loginBroadcastDelay == 0) {
                DayTime current = DayTime.fromDayTime(dayTime);
                triggerLoginBroadcast(player, days, current);
            }
        }

        boolean isInOverworld = player.level().dimension() == Level.OVERWORLD;
        boolean isOnSurface = isInOverworld && PlayerStatic.isOnSurfaceOfOverworld(player);
        boolean previouslyOnSurface = data.wasOnSurface;

        DayTime currentDayTime = DayTime.fromDayTime(dayTime);
        long gameTime = overworld.getGameTime();

        boolean shouldBroadcastDawn = currentDayTime == DayTime.DAWN && data.lastDayTimePeriod != DayTime.DAWN;
        boolean shouldBroadcastSunset = currentDayTime == DayTime.EARLY_NIGHT && data.lastDayTimePeriod == DayTime.SUNSET;

        if (isInOverworld && (shouldBroadcastDawn || shouldBroadcastSunset)) {
            if (isOnSurface) {
                if (shouldBroadcastDawn) {
                    playSoundForPlayer(player, ZGRSoundEvents.MORNING_ROAST, SoundSource.AMBIENT, 1.0f);
                } else {
                    playSoundForPlayer(player, ZGRSoundEvents.EVENING_HOWL, SoundSource.AMBIENT, 1.0f);
                }
                TimeBroadcastPacket packet = new TimeBroadcastPacket(days, shouldBroadcastDawn ? DayTime.DAWN : DayTime.SUNSET, dayTime);
                ZGRNetwork.sendToClient(packet, player);
            } else if (hasClock(player)) {
                triggerUndergroundAlarm(player);
            }
            data.lastDayTimePeriod = currentDayTime;
        }

        if (isInOverworld && isOnSurface && !previouslyOnSurface) {
            if (data.lastLeftSurfaceTime != -1 && gameTime - data.lastLeftSurfaceTime >= LEAVE_SURFACE_THRESHOLD_TICKS) {
                triggerReturnBroadcast(player, days, currentDayTime);
            }
        }

        if (isInOverworld && !isOnSurface && previouslyOnSurface) {
            data.lastLeftSurfaceTime = gameTime;
        }

        data.wasOnSurface = isOnSurface;
    }

    private static void triggerLoginBroadcast(ServerPlayer player, long days, DayTime dayTime) {
        TimeBroadcastPacket packet = new TimeBroadcastPacket(days, dayTime,player.level().dayTime());
        ZGRNetwork.sendToClient(packet, player);
    }

    private static void playSoundForPlayer(ServerPlayer player, SoundEvent sound, SoundSource source, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, source, volume, 1.0f);
    }

    private static boolean hasClock(ServerPlayer player) {
        return player.getInventory().hasAnyOf(Set.of(Items.CLOCK));
    }

    private static void triggerUndergroundAlarm(ServerPlayer player) {
        playSoundForPlayer(player, ZGRSoundEvents.CLOCK_RING, SoundSource.PLAYERS, 0.5f);

        Component chatMsg = buildChatComponent(player);
        TimeBroadcastPacket packet = new TimeBroadcastPacket(chatMsg);
        ZGRNetwork.sendToClient(packet, player);
    }

    private static void triggerReturnBroadcast(ServerPlayer player, long days, DayTime dayTime) {
        TimeBroadcastPacket packet = new TimeBroadcastPacket(days, dayTime,player.level().dayTime());
        ZGRNetwork.sendToClient(packet, player);
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

    public static void handleManualClockUse(ServerPlayer player) {
        if (player.level().isClientSide) return;
        if (player.level().dimension() != Level.OVERWORLD) return;

        ServerLevel overworld = player.server.overworld();
        TimeData timeData = TimeData.get(overworld);
        long dayTime = overworld.getDayTime();
        long days = timeData.getDays();
        DayTime currentDayTime = DayTime.fromDayTime(dayTime);

        triggerManualBroadcast(player, days, currentDayTime);
    }

    private static void triggerManualBroadcast(ServerPlayer player, long days, DayTime dayTime) {
        TimeBroadcastPacket packet = new TimeBroadcastPacket(days, dayTime,player.level().dayTime());
        ZGRNetwork.sendToClient(packet, player);
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
