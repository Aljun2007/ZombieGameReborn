package com.aljun.zombiegamereborn.common.client.events;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.client.ResourcePackDetector;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(@NotNull Void input, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
                ResourcePackDetector.refresh();
            }
        });
    }
}
