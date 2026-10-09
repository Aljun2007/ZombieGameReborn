package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.optimizer.ZombieBlockOperationQueue;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 在每个 Level 刻结束时刷新延迟方块操作队列，
 * 确保方块修改在区块锁释放后进行。
 * 同时在服务器关闭时清空所有队列。
 */
@EventBusSubscriber
public class ChunkOperationFlushHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) {
            ZombieBlockOperationQueue.flushLevel((ServerLevel) event.getLevel());
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
