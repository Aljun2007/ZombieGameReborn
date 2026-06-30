package com.aljun.zombiegamereborn.diplomat.enhancedcelestials;

import dev.corgitaco.enhancedcelestials.EnhancedCelestials;
import dev.corgitaco.enhancedcelestials.api.EnhancedCelestialsRegistry;
import dev.corgitaco.enhancedcelestials.api.lunarevent.LunarEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class EnhancedCelestialsProviderImpl implements IEnhancedCelestialsProvider {

    private static final ResourceLocation BLOOD_MOON_ID =
            ResourceLocation.fromNamespaceAndPath("enhancedcelestials", "blood_moon");

    @Override
    public void setBloodMoon(Level level) {
        EnhancedCelestials.lunarForecastWorldData(level).ifPresent(data -> {
            ResourceKey<LunarEvent> bloodMoonKey = ResourceKey.create(
                    EnhancedCelestialsRegistry.LUNAR_EVENT_KEY,
                    BLOOD_MOON_ID
            );
            data.setLunarEvent(bloodMoonKey);
        });
    }
}
