package com.aljun.zombiegamereborn.common.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class SurvivalDayManager {
    public static double getDay(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        TimeData data = TimeData.get(overworld);

        long baseDays = data.getDays();
        long dayTime = Math.floorMod(overworld.getDayTime(), 24000L);
        double dayProgress = dayTime / 24000d;

        return baseDays + dayProgress;
    }
}
