package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.utils.ZombieDecisionUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZombieSenseHandler {



    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ZombieProperty zombieProperty = ZGRGame.getGameProperty().getStageProperty(event.getServer()).zombieProperty;
            ZombieSenseManager.refresh(zombieProperty);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Level level = event.getEntity().level();
        LivingEntity victim = event.getEntity();
        if (ZombieDecisionUtils.zombieAttackableEntity(victim)) {
            ZombieSenseManager.broadcastSense(event.getEntity(), level, SenseType.BLEEDING);
        }

    }

}
