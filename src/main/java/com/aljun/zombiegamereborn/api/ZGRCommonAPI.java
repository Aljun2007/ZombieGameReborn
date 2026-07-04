package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.game.TimeData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("all")
public class ZGRCommonAPI {
    public static class SurvivalDaysAPI {

        public static final long MIN_DAYS = 1;
        public static final long MAX_DAYS = 1000000;  // 可根据需要调整

        // 获取生存天数
        public static long getSurvivalDays(MinecraftServer server) {
            if (server == null) return MIN_DAYS;

            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return MIN_DAYS;

            TimeData data = TimeData.get(overworld);

            long days = data.getDays();
            return validateDays(days);
        }

        // 设置生存天数
        public static void setSurvivalDays(MinecraftServer server, long days) {
            if (server == null) return;

            days = validateDays(days);

            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            TimeData data = TimeData.get(overworld);
            data.setDays(days);
        }

        // 增加生存天数
        public static void addSurvivalDays(MinecraftServer server, long delta) {
            if (server == null) return;

            // 防止溢出
            if (delta == 0) return;

            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            TimeData data = TimeData.get(overworld);

            long currentDays = data.getDays();
            long newDays = currentDays + delta;

            // 溢出检查
            if (delta > 0 && newDays < currentDays) {
                // 正溢出，设置为最大值
                newDays = MAX_DAYS;
            } else if (delta < 0 && newDays > currentDays) {
                // 负溢出，设置为最小值
                newDays = MIN_DAYS;
            }

            data.setDays(validateDays(newDays));
        }

        // 数据合法性检测
        private static long validateDays(long days) {
            if (days < MIN_DAYS) return MIN_DAYS;
            return Math.min(days, MAX_DAYS);
        }

        // 可选：获取原始数据（跳过合法性检测，用于调试）
        public static long getRawSurvivalDays(MinecraftServer server) {
            if (server == null) return MIN_DAYS;
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return MIN_DAYS;
            TimeData data = TimeData.get(overworld);
            return data.getDays();
        }
    }
}
