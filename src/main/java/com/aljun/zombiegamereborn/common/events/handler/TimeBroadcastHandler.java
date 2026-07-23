package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.player.PlayerStatic;
import com.aljun.zombiegamereborn.common.player.TimeBroadcast;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

    private static int i1 = 0;

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
        if (event.getEntity() instanceof ServerPlayer player) {
            ZGRPlayerAPI.saveToPersistentData(player);
            // 注：PlayerList.save(ServerPlayer) 是 protected 方法，无法直接调用。
            // 服务端在玩家断开连接时会自动保存玩家实体数据，saveToPersistentData 已将
            // Capability 数据写入 getPersistentData()，随实体保存时自动持久化。
        }
        TimeBroadcast.resetPlayerData(event.getEntity().getGameProfile().getId());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);
        if (data != null) {
            CompoundTag persistent = player.getPersistentData();
            boolean hasNewFormatData = persistent.contains("zgr_cap_survived_day");
            boolean hasOldFormatData = persistent.contains("zgr_underground_day") ||
                                       persistent.contains("zgr_underground_game_time") ||
                                       persistent.contains("zgr_last_estimated_day");

            if (hasNewFormatData) {
                // 已有新格式持久数据的玩家：从持久数据恢复个人天数
                ZGRPlayerAPI.loadFromPersistentData(player);
            } else if (hasOldFormatData) {
                // 是旧格式老玩家 → 尝试从旧数据恢复
                long recovered = persistent.getLong("zgr_underground_day");
                if (recovered <= 1L) recovered = persistent.getLong("zgr_last_estimated_day");
                if (recovered > 1L) {
                    data.setSurvivedDay(recovered);
                }
            }
            // 全新玩家（无任何持久数据）：保持默认的第 1 天
        }

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
        // 注意：同时处理死亡（isWasDeath=true）和通关末地后重生（isWasDeath=false）两种场景
        // 后者发生时，客户端看完 Credits 后发送 PERFORM_RESPAWN，服务端创建新玩家实体。
        // 如果不在此处复制 Capability 数据，新实体的生存天数会重置为默认值 1。
        if (event.getOriginal() instanceof ServerPlayer oldPlayer
                && event.getEntity() instanceof ServerPlayer newPlayer) {
            // 死亡后 Forge 会 invalidateCaps() 禁用旧实体的所有 Capability，
            // 导致 getCapability() 返回空。必须先 reviveCaps() 才能读取数据。
            // 对于非死亡场景，reviveCaps() 是安全的空操作。
            oldPlayer.reviveCaps();
            try {
                ZGRPlayerAPI.saveToPersistentData(oldPlayer);
                ZGRPlayerAPI.copyData(oldPlayer, newPlayer);
            } finally {
                oldPlayer.invalidateCaps();
            }
        }

        var oldTag = event.getOriginal().getPersistentData();
        var newTag = event.getEntity().getPersistentData();
        String[] keys = {
                "zgr_cap_survived_day",
                "zgr_cap_underground_day",
                "zgr_cap_underground_game_time",
                "zgr_cap_last_estimated_day",
                "zgr_cap_total_zombie_kills",
                "zgr_underground_day",
                "zgr_underground_game_time",
                "zgr_last_estimated_day"
        };
        for (String key : keys) {
            if (oldTag.contains(key)) {
                newTag.putLong(key, oldTag.getLong(key));
            }
        }

        // 从新玩家的 PersistentData 强制加载到 Capability（兜底恢复）
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
             ZGRPlayerAPI.loadFromPersistentData(newPlayer);
        }
    }
}
