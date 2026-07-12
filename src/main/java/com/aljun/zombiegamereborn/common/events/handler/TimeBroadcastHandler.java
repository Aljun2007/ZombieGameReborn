package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.player.PlayerStatic;
import com.aljun.zombiegamereborn.common.player.TimeBroadcast;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TimeBroadcastHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        TimeBroadcast.tick(player);
    }

    @SubscribeEvent
    public static void onRightClickClock(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItemStack().is(Items.CLOCK)) return;

        TimeBroadcast.handleManualClockUse(player);
    }

    @SubscribeEvent
    public static void onWorldLoaded(ServerAboutToStartEvent event) {
        TimeBroadcast.resetAllData();
        PlayerStatic.resetAllData();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TimeBroadcast.resetPlayerData(event.getEntity().getGameProfile().getId());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TimeBroadcast.scheduleLoginBroadcast(player);
        MinecraftServer server = player.getServer();
        boolean isOp = false;
        if (server != null) {
            isOp = server.getPlayerList().isOp(player.getGameProfile());
        }
        ZGRNetwork.sendToClient(new LoginWelcomePacket(isOp), player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        // 死亡重生后，复制地下估算数据到新玩家实体
        var oldTag = event.getOriginal().getPersistentData();
        var newTag = event.getEntity().getPersistentData();
        String[] keys = {"zgr_underground_day", "zgr_underground_game_time", "zgr_last_estimated_day"};
        for (String key : keys) {
            if (oldTag.contains(key)) {
                newTag.putLong(key, oldTag.getLong(key));
            }
        }
    }
}
