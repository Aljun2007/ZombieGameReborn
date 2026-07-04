package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.game.TimeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SurvivedDayHandler {

    @SuppressWarnings("ConstantConditions")
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // 只在 END 阶段执行，避免重复
        if (event.phase != TickEvent.Phase.END) return;

        // 只处理主世界
        if (event.level.dimension() != Level.OVERWORLD) return;

        // 关键：必须在服务端运行
        if (event.level.isClientSide) return;

        // 此时 event.level 是 ServerLevel
        ServerLevel overworld = (ServerLevel) event.level;

        // 获取或创建全局数据
        TimeData data = TimeData.get(overworld);

        long currentGameTime = overworld.getGameTime();
        boolean hasPlayersOnline = !overworld.getServer().getPlayerList().getPlayers().isEmpty();
        long currentTimeOfDay = currentGameTime % 24000;
        boolean isNight = currentTimeOfDay >= 13000;

        // 首次运行或玩家从离线变在线时，重置基准时间
        if (data.getLastGameTime() == -1 ||
                (!data.wasAnyPlayerOnline() && hasPlayersOnline)) {
            data.updateTickState(currentGameTime, hasPlayersOnline, isNight);
            return;
        }

        // 无玩家在线时，不进行天数检测
        if (!hasPlayersOnline) {
            data.setWasAnyPlayerOnline(false);
            return;
        }

        // 有玩家在线，检测昼夜交替（仅自然流逝）
        long delta = currentGameTime - data.getLastGameTime();
        boolean isNaturalTick = (delta == 1);

        if (isNaturalTick) {
            boolean wasNight = data.wasNight();
            // 从夜晚过渡到白天 → 天数+1
            if (wasNight && !isNight) {
                data.incrementDay();
            }
        }

        // 更新状态
        data.updateTickState(currentGameTime, hasPlayersOnline, isNight);
    }
}