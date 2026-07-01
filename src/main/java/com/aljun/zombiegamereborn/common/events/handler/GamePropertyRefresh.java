package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GamePropertyRefresh {

    private static long lastDayChecked = -1;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
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
