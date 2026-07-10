package com.aljun.zombiegamereborn.sounds;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegisterEvent;

public class ZGRSoundEvents {

    public static class IDs {

        public static final ResourceLocation MORNING_ROAST = id("morning_roast");
        public static final ResourceLocation EVENING_HOWL = id("evening_howl");
        public static final ResourceLocation CLOCK_RING = id("clock_ring");

        public static ResourceLocation id(String id) {
            return ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, id);
        }
    }

    public static final SoundEvent MORNING_ROAST = SoundEvent.createVariableRangeEvent(IDs.MORNING_ROAST);
    public static final SoundEvent EVENING_HOWL = SoundEvent.createVariableRangeEvent(IDs.EVENING_HOWL);
    public static final SoundEvent CLOCK_RING = SoundEvent.createVariableRangeEvent(IDs.CLOCK_RING);

    public static void register(RegisterEvent.RegisterHelper<SoundEvent> helper) {
        helper.register(IDs.MORNING_ROAST, MORNING_ROAST);
        helper.register(IDs.EVENING_HOWL, EVENING_HOWL);
        helper.register(IDs.CLOCK_RING, CLOCK_RING);
    }
}
