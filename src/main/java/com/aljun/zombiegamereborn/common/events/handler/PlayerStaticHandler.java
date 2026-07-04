package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.player.PlayerStatic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerStaticHandler {

    /**
     * 玩家每刻事件 - 用于驱动检测（可选）
     * 实际检测由 PlayerStatic.isOnSurfaceOfOverworld() 按需触发
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // 每20刻（1秒）主动触发一次检测预热，确保数据及时更新
        // 实际使用时可以直接调用 PlayerStatic.isOnSurfaceOfOverworld(player)
        if (player.tickCount % 20 == 0) {
            PlayerStatic.isOnSurfaceOfOverworld(player);
        }
    }

    /**
     * 服务器启动/世界加载时重置所有数据
     */
    @SubscribeEvent
    public static void onWorldLoaded(ServerAboutToStartEvent event) {
        PlayerStatic.resetAllData();
    }
}