package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.optimizer.ZombieBlockOperationQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在每个 Level 刻结束时刷新延迟方块操作队列，
 * 确保方块修改在区块锁释放后进行。
 * 同时在服务器关闭时清空所有队列。
 */
@Mod.EventBusSubscriber
public class ChunkOperationFlushHandler {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.level.isClientSide()) {
            ZombieBlockOperationQueue.flushLevel((ServerLevel) event.level);
        }
    }

    /**
     * 服务器关闭时清空所有延迟操作，防止内存泄漏和世界保存异常。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ZombieBlockOperationQueue.flushAll();
    }
}
