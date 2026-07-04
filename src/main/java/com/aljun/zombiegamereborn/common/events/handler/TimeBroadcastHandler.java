package com.aljun.zombiegamereborn.common.events.handler;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TimeBroadcastHandler {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        //TODO: 添加世界时间广播
    }

    @SubscribeEvent
    public static void onWorldLoaded(ServerAboutToStartEvent event) {
        //TODO 重置所有计时器等
    }

    //TODO 还要添加一个玩家加入服务器的播报
}
