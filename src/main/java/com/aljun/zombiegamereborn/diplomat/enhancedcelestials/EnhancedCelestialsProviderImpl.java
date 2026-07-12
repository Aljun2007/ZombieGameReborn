package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import dev.corgitaco.enhancedcelestials.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials.api.EnhancedCelestialsRegistry;
import dev.corgitaco.enhancedcelestials.api.lunarevent.LunarEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class EnhancedCelestialsProviderImpl implements IEnhancedCelestialsProvider {

    private static final ResourceLocation BLOOD_MOON_ID =
            ResourceLocation.fromNamespaceAndPath("enhancedcelestials", "blood_moon");

    private static final ResourceKey<LunarEvent> BLOOD_MOON_KEY = ResourceKey.create(
            EnhancedCelestialsRegistry.LUNAR_EVENT_KEY, BLOOD_MOON_ID);

    @Override
    public void setBloodMoon(Level level) {
        EnhancedCelestials.lunarForecastWorldData(level).ifPresent(data ->
                data.setLunarEvent(BLOOD_MOON_KEY));
    }

    @Override
    public boolean isBloodMoon(Level level) {
        return EnhancedCelestials.lunarForecastWorldData(level)
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
