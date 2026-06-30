package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.common.data.GlobalSurvivalData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class SurvivalDayManager {
    public static double getDay(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        GlobalSurvivalData data = GlobalSurvivalData.get(overworld);
        
        long baseDays = data.getDays();
        long dayTime = overworld.dayTime();
        long gameTime = overworld.getGameTime();
        long lastIncrementTime = data.getLastIncrementDayGameTime();
        
        double dayProgress = dayTime / 24000d;
        
        // 修复回跳问题：如果刚递增过天数且 dayTime 还没重置
        // 说明处于"名义上是新一天，但实际dayTime还是旧值"的状态
        if (lastIncrementTime > 0 && gameTime - lastIncrementTime < 100) {
            // 在递增后的短时间内，如果 dayTime 很大（接近24000），说明还没重置
            // 此时应该用 baseDays - 1 来计算，避免跳跃
            if (dayTime > 23000) {
                return (baseDays - 1) + dayProgress;
            }
        }
        
        return baseDays + dayProgress;
    }
}
