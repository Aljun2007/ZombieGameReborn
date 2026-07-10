package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GamePropertyRefresher {

    private static long lastDayChecked = -1;

    /**
     * 新世界加载时预缓存当前阶段的 StageProperty，
     * 触发 GameProperty 内部 (cachedResult/cachedDayValue) 写入缓存，
     * 避免首个 tick 时缓存缺失导致的不必要计算。
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        //预热缓存
        ZGRGame.getGameProperty().getStageProperty(event.getServer());
    }


    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        lastDayChecked = -1;
    }


    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            //预热缓存
            StageProperty stageProperty = ZGRGame.getGameProperty().getStageProperty(event.getServer());
            ZombieSenseManager.refresh(stageProperty.zombieProperty);
            ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.setMobDamageMultiplier(stageProperty.zombieProperty.musketModGunDamageModify);

            // === 血月重载 ===
            ServerLevel overworld = event.getServer().overworld();
            long currentDay = overworld.getDayTime() / 24000;
            if (currentDay != lastDayChecked) {
                lastDayChecked = currentDay;
                if (RandomUtils.booleanByChance(stageProperty.bloodMoonChance)) {
                    ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.setBloodMoon(overworld);
                }
            }
        }
    }
}
