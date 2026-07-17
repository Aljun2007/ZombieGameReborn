package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.player.ReginalStageDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@SuppressWarnings("all")
public class ZGRCommonAPI {
    public static class SurvivalDaysAPI {

        public static final long MIN_DAYS = 1;
        public static final long MAX_DAYS = Long.MAX_VALUE;

        /**
         * 获取全体玩家平均存活天数（取整）
         */
        public static long getGlobalAverage(MinecraftServer server) {
            return validateDays((long) ReginalStageDetector.getGlobalAverage(server));
        }

        public static long getRegional(ServerLevel level, BlockPos pos) {
            return validateDays((long) ReginalStageDetector.get(level,pos));
        }

        private static long validateDays(long days) {
            if (days < MIN_DAYS) return MIN_DAYS;
            return Math.min(days, MAX_DAYS);
        }
    }
}
