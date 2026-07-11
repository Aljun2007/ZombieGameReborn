package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.events.handler.SurvivedDayHandler;
import net.minecraft.server.MinecraftServer;

@SuppressWarnings("all")
public class ZGRCommonAPI {
    public static class SurvivalDaysAPI {

        public static final long MIN_DAYS = 1;
        public static final long MAX_DAYS = 1000000;

        public static long getSurvivalDays(MinecraftServer server) {
            return validateDays(SurvivedDayHandler.getSurvivedDays());
        }

        public static void setSurvivalDays(MinecraftServer server, long days) {
            SurvivedDayHandler.setSurvivedDays(validateDays(days));
        }

        public static void addSurvivalDays(MinecraftServer server, long delta) {
            if (delta == 0) return;
            long current = SurvivedDayHandler.getSurvivedDays();
            long newDays = current + delta;
            if (delta > 0 && newDays < current) {
                newDays = MAX_DAYS;
            } else if (delta < 0 && newDays > current) {
                newDays = MIN_DAYS;
            }
            SurvivedDayHandler.setSurvivedDays(validateDays(newDays));
        }

        private static long validateDays(long days) {
            if (days < MIN_DAYS) return MIN_DAYS;
            return Math.min(days, MAX_DAYS);
        }

        public static long getRawSurvivalDays(MinecraftServer server) {
            return SurvivedDayHandler.getSurvivedDays();
        }
    }
}
