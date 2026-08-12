package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import dev.corgitaco.enhancedcelestials2core.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials2core.api.EnhancedCelestialsRegistry;
import dev.corgitaco.enhancedcelestials2core.api.lunarevent.LunarEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class EnhancedCelestials2ProviderImpl implements IEnhancedCelestialsProvider {

    // 新版 EC2 的 blood_moon 事件由 Enhanced Celestials 2: Default Lunar Events 提供
    private static final ResourceLocation BLOOD_MOON_ID =
            ResourceLocation.fromNamespaceAndPath("enhancedcelestials2defaultlunarevents", "blood_moon");

    private static final ResourceKey<LunarEvent> BLOOD_MOON_KEY = ResourceKey.create(
            EnhancedCelestialsRegistry.LUNAR_EVENT_KEY, BLOOD_MOON_ID);

    @Override
    public void setBloodMoon(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        server.registryAccess().registry(EnhancedCelestialsRegistry.LUNAR_EVENT_KEY).flatMap(registry -> registry.getHolder(BLOOD_MOON_KEY)).ifPresent(holder ->
                EnhancedCelestials.lunarForecastWorldData(overworld)
                        .ifPresent(data -> data.setLunarEventTonight(holder)));
    }

    @Override
    public boolean isBloodMoon(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return EnhancedCelestials.lunarForecastWorldData(overworld)
                .map(data -> {
                    Holder<LunarEvent> event = data.currentScheduledLunarEvent();
                    return event != null && event.unwrapKey()
                            .map(key -> key.equals(BLOOD_MOON_KEY))
                            .orElse(false);
                })
                .orElse(false);
    }
}