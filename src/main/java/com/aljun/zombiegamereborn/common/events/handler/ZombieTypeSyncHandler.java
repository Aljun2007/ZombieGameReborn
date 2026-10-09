package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class ZombieTypeSyncHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof Zombie zombie)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (player.level().isClientSide) return;
        ZombieTypeManager.syncToClient(zombie);
    }
}
