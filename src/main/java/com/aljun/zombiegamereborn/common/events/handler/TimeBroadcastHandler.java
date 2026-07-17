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
        if (!event.isWasDeath()) return;

        if (event.getOriginal() instanceof ServerPlayer oldPlayer
                && event.getEntity() instanceof ServerPlayer newPlayer) {
            // 先将旧玩家的当前数据写入 persistentData，作为持久化备份
            // 在 Clone 事件中，旧玩家实体可能已经失效或 Capability 数据不可靠。
            // 为了确保数据不丢失，我们强制将旧玩家 PersistentData 中的关键数据
            // 直接复制到新玩家的 PersistentData 中。
            // 注意：下方的循环已经处理了具体的 key 复制，这里不需要额外操作，
            // 但为了防止 ZGRPlayerAPI.saveToPersistentData 因为 getPlayerData 返回 null 而报错或无效，
            // 我们移除该调用，依赖下方的 NBT 直接复制逻辑，这是最稳妥的死亡不掉数据方式。
            // 再通过 Capability 复制到新玩家
            ZGRPlayerAPI.copyData(oldPlayer, newPlayer);
        }

        // 注意：ZGRPlayerAPI.copyData(oldPlayer, newPlayer) 已经在上方被调用。
        // 该方法内部应当已经处理了 Capability 数据的复制（从旧玩家的 Capability 复制到新玩家的 Capability）。
        // 下方的 PersistentData 复制是为了确保“死亡不掉数据”的双重保险，
        // 因为 PersistentData 会随玩家实体自动保存，而 Capability 有时在死亡瞬间可能状态不稳定。
        
        // 如果 ZGRPlayerAPI.copyData 实现正确，它应该已经从 oldPlayer 的 Capability 读取数据并写入 newPlayer 的 Capability。
        // 但为了绝对安全，防止 copyData 内部逻辑遗漏或失效，我们这里再显式地从 PersistentData 同步一次到新的 Capability。
        // 不过，通常做法是：
        // 1. copyData 负责 Capability -> Capability (内存中直接拷贝，最快最准)
        // 2. 下面的代码负责 PersistentData -> PersistentData (磁盘/存档层面的备份拷贝)
        
        // 用户的问题是：“tag复制成功了，那有没有同步到新的capacity里面呢？”
        // 答案是：上面的 ZGRPlayerAPI.copyData(oldPlayer, newPlayer) 应该已经做了这件事。
        // 如果担心没同步，我们应该检查 copyData 的实现，或者在这里强制从 PersistentData 加载到新 Capability。
        
        // 鉴于上下文注释说“移除该调用...依赖下方的NBT直接复制逻辑”，这暗示之前的 copyData 可能不可靠。
        // 但现在的代码里又加回了 copyData。
        
        // 最佳实践：
        // 1. 保留 copyData (它处理 Capability 间的直接传输)。
        // 2. 保留下方的 NBT 复制 (它处理持久化数据的备份)。
        // 3. 为了确保万无一失，在 NBT 复制后，我们可以选择是否要从新玩家的 PersistentData 重新加载到 Capability。
        //    但这通常是多余的，因为 copyData 应该已经完成了工作。
        
        // 如果用户怀疑 copyData 没生效，最稳妥的方式是在这里手动触发一次从 PersistentData 到 Capability 的同步。
        // 但是，ZGRPlayerAPI.loadFromPersistentData(player) 是用于登录时的。
        
        // 让我们仔细看注释：“再通过 Capability 复制到新玩家 ZGRPlayerAPI.copyData(oldPlayer, newPlayer);”
        // 这意味着 copyData 意图是同步 Capability。
        
        // 如果 copyData 内部只是简单的 field 赋值，那么它是同步的。
        // 如果 copyData 内部依赖于 read/write NBT，那么它也是同步的。
        
        // 既然用户问“有没有同步”，且代码中已经调用了 copyData，理论上已经同步了。
        // 但为了回应“重写选中代码”的请求，并确保数据一致性，我们可以添加一个步骤：
        // 在复制完 PersistentData 后，强制让新玩家从它的 PersistentData 中加载一次数据到 Capability。
        // 这样可以保证：即使 copyData 失败，只要 PersistentData 复制成功，数据也能通过 load 恢复。
        
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
        
        // 关键补充：确保新玩家的 Capability 与刚刚复制好的 PersistentData 保持一致
        // 因为 copyData 可能只复制了内存中的 Capability，而上面的循环只复制了 NBT。
        // 如果 copyData 正常工作，这一步是冗余但安全的。如果 copyData 有问题，这一步能救回来。
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
             ZGRPlayerAPI.loadFromPersistentData(newPlayer);
        }
    }
}
