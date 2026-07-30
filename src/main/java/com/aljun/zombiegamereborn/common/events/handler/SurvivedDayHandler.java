package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurvivedDayHandler {

    private static long lastGameTime = -1;
    private static boolean wasAnyPlayerOnline = false;
    private static boolean wasNight = false;

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.dimension() != Level.OVERWORLD) return;
        if (event.level.isClientSide) return;

        ServerLevel overworld = (ServerLevel) event.level;
        long currentGameTime = overworld.getGameTime();
        boolean hasPlayersOnline = !overworld.getServer().getPlayerList().getPlayers().isEmpty();
        long currentTimeOfDay = Math.floorMod(overworld.getDayTime(), 24000L);
        boolean isNight = currentTimeOfDay >= 12000; // 12000=SUNSET，玩家即可睡觉

        // 首次运行或玩家从离线变在线时，重置基准时间
        if (lastGameTime == -1 || (!wasAnyPlayerOnline && hasPlayersOnline)) {
            updateTickState(currentGameTime, hasPlayersOnline, isNight);
            return;
        }

        // 无玩家在线时，不进行天数检测
        if (!hasPlayersOnline) {
            wasAnyPlayerOnline = false;
            return;
        }

        // 检测昼夜交替（覆盖睡觉造成的时间跳跃）
        if (wasNight && !isNight) {
            // 分别增加每个在线玩家的个人生存天数
            for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
                IPlayerData playerData = ZGRPlayerAPI.getPlayerData(player);
                if (playerData != null) {
                    playerData.setSurvivedDay(playerData.getSurvivedDay() + 1);
                }
            }
        }

        updateTickState(currentGameTime, hasPlayersOnline, isNight);
    }

    private static void updateTickState(long gameTime, boolean hasPlayers, boolean isNight) {
        lastGameTime = gameTime;
        wasAnyPlayerOnline = hasPlayers;
        wasNight = isNight;
    }
}