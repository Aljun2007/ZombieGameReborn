package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import dev.corgitaco.enhancedcelestials.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials.api.EnhancedCelestialsRegistry;
import dev.corgitaco.enhancedcelestials.api.lunarevent.LunarEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class EnhancedCelestialsProviderImpl implements IEnhancedCelestialsProvider {

    private static final ResourceLocation BLOOD_MOON_ID =
            ResourceLocation.fromNamespaceAndPath("enhancedcelestials", "blood_moon");

    private static final ResourceKey<LunarEvent> BLOOD_MOON_KEY = ResourceKey.create(
            EnhancedCelestialsRegistry.LUNAR_EVENT_KEY, BLOOD_MOON_ID);

    @Override
    public void setBloodMoon(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            EnhancedCelestials.lunarForecastWorldData(overworld).ifPresent(data ->
                    data.setLunarEvent(BLOOD_MOON_KEY));
        }
    }

    @Override
    public boolean isBloodMoon(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return false;
        return EnhancedCelestials.lunarForecastWorldData(overworld)
                .map(data -> {
                    long currentDay = data.getCurrentDay();
                    Holder<LunarEvent> event = data.getLunarEventForDay(currentDay);
                    return event != null && event.unwrapKey()
                            .map(key -> key.equals(BLOOD_MOON_KEY))
                            .orElse(false);
                })
                .orElse(false);
    }
}
