package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ZombieGameReborn.MOD_ID)
public class ZombieStatisticsHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        ZombieStatic.resetZombieCount();
        ZombieGoalOptimizer.update();
    }
}
