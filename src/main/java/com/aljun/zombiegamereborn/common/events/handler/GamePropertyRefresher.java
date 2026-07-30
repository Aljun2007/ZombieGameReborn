package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.DayTime;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber
public class GamePropertyRefresher {

    private static long lastDayChecked = -1;
    public static boolean bloodMoonTriggeredThisDay = false;
    public static boolean bloodMoonActive = false;

    private static Logger LOGGER = LogUtils.getLogger();

    /**
     * 新世界加载时预缓存当前阶段的 StageProperty，
     * 触发 GameProperty 内部 (cachedResult/cachedDayValue) 写入缓存，
     * 避免首个 tick 时缓存缺失导致的不必要计算。
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ZGRGame.getGameProperty().getGlobalStage(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            long nanos = System.nanoTime();

            // 每 20 tick 清理 dayCache
            if (event.getServer().getTickCount() % 20 == 0) {
                ZGRGame.getGameProperty().clearDayCache();
            }

            StageProperty stageProperty = ZGRGame.getGameProperty().getGlobalStage(event.getServer());
            ZombieSenseManager.refresh(stageProperty.zombieProperty);
            ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.setMobDamageMultiplier(stageProperty.zombieProperty.musketModGunDamageModify);
            long t1 = System.nanoTime();
            // === 血月重载 ===
            ServerLevel overworld = event.getServer().overworld();
            long dayTime = overworld.getDayTime();
            long currentDay = dayTime / 24000;

            // 检测天数变化，重置当天血月触发标记
            if (currentDay != lastDayChecked) {
                lastDayChecked = currentDay;
                bloodMoonTriggeredThisDay = false;
                bloodMoonActive = false;
            }

            long timeOfDay = Math.floorMod(dayTime, 24000L);

            // 在入夜时(>=13000)触发血月检查
            if (!bloodMoonTriggeredThisDay) {
                if (timeOfDay >= DayTime.EARLY_NIGHT.start) {
                    bloodMoonTriggeredThisDay = true;
                    bloodMoonActive = RandomUtils.booleanByChance(stageProperty.bloodMoonChance);
                    if (bloodMoonActive) {
                        setBloodMoonSafe(overworld);
                    }
                }
            }

            // 夜间每 40 tick 检查血月是否被 forecast 重算清掉，若丢失则重新设置
            if (bloodMoonActive && timeOfDay >= DayTime.EARLY_NIGHT.start
                    && event.getServer().getTickCount() % 40 == 0) {
                if (!ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(event.getServer())) {
                    setBloodMoonSafe(overworld);
                }
            }
            long elapsed = System.nanoTime() - nanos;
            if (elapsed > 5_000_000) { // > 5ms
                LOGGER.warn("ZGR tick took {}ms (getStageProperty: {}µs, sense: {}µs)",
                        elapsed / 1_000_000,
                        (t1 - nanos) / 1_000,
                        (System.nanoTime() - t1) / 1_000);
            }
        }
    }

    /**
     * 安全调用 setBloodMoon，避免 Enhanced Celestials 内部 setDayTime 造成时间跳跃
     */
    private static void setBloodMoonSafe(ServerLevel overworld) {
        long savedDayTime = overworld.getDayTime();
        ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.setBloodMoon(overworld.getServer());
        overworld.setDayTime(savedDayTime);
    }
}
