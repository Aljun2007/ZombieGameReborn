package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.player.PlayerStatic;
import com.aljun.zombiegamereborn.common.player.TimeBroadcast;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import net.minecraft.nbt.CompoundTag;
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

        ZGRPlayerAPI.loadFromPersistentData(player);

        // 登录后与全局天数计数器同步
        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);
        if (data != null) {
            long globalDays = SurvivedDayHandler.getSurvivedDays();
            if (data.getSurvivedDay() < globalDays) {
                data.setSurvivedDay(globalDays);
            } else if (data.getSurvivedDay() > globalDays) {
                SurvivedDayHandler.setSurvivedDays(data.getSurvivedDay());
            } else if (data.getSurvivedDay() == 1L && globalDays == 1L) {
                // 两种来源都未恢复 → 检查是否有持久数据线索判断新老玩家
                CompoundTag persistent = player.getPersistentData();
                // 检查旧格式持久键（可能来自旧版本 mod）或任何 zgr_ 数据
                if (persistent.contains("zgr_underground_day") ||
                    persistent.contains("zgr_underground_game_time") ||
                    persistent.contains("zgr_last_estimated_day")) {
                    // 是老玩家（有旧格式数据）→ 尝试用旧数据恢复
                    long recovered = persistent.getLong("zgr_underground_day");
                    if (recovered <= 1L) recovered = persistent.getLong("zgr_last_estimated_day");
                    if (recovered > 1L) {
                        data.setSurvivedDay(recovered);
                        SurvivedDayHandler.setSurvivedDays(recovered);
                    }
                }
            }
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
        if (!event.isWasDeath()) return;

        if (event.getOriginal() instanceof ServerPlayer oldPlayer
                && event.getEntity() instanceof ServerPlayer newPlayer) {
            ZGRPlayerAPI.copyData(oldPlayer, newPlayer);
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
    }
}
