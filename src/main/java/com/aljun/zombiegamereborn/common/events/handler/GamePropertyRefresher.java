package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber
public class GamePropertyRefresher {

    private static long lastDayChecked = -1;

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
            long currentDay = overworld.getDayTime() / 24000;
            if (currentDay != lastDayChecked) {
                lastDayChecked = currentDay;
                if (RandomUtils.booleanByChance(stageProperty.bloodMoonChance)) {
                    ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.setBloodMoon(overworld);
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
}
