package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import com.aljun.zombiegamereborn.common.player.capability.PlayerDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEventHandler {

    private static final ResourceLocation ZOMBIE_DATA_KEY =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "zombie_data");
    private static final ResourceLocation PLAYER_DATA_KEY =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "player_data");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Zombie) {
            event.addCapability(ZOMBIE_DATA_KEY, new ZombieDataProvider());
        } else if (event.getObject() instanceof net.minecraft.server.level.ServerPlayer) {
            event.addCapability(PLAYER_DATA_KEY, new PlayerDataProvider());
        }
    }
}
