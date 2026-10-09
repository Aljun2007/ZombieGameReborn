package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ZombieGameReborn.MOD_ID)
public class ZombieSenseHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        var stageProperty = ZGRGame.getGameProperty().getGlobalStage(event.getServer());
        // 不再需要重复调用 getStageProperty()
        // ZombieSenseManager.refresh 已在 GamePropertyRefresher 中处理
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Level level = event.getEntity().level();
        LivingEntity victim = event.getEntity();
        if (ZombieUtils.attackableEntity(victim)) {
            ZombieSenseManager.broadcastSense(event.getEntity(), level, SenseType.BLEEDING);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null || event.getPlayer().level().isClientSide) return;
        ZombieSenseManager.broadcastSense(event.getPlayer(), event.getPlayer().level(), SenseType.BLOCK);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Player player) {
            ZombieSenseManager.broadcastSense(player, player.level(), SenseType.BLOCK);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (event.getUseBlock() == TriState.FALSE) return;
        ZombieSenseManager.broadcastSense(event.getEntity(), event.getLevel(), SenseType.BLOCK);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) return;

        Explosion explosion = event.getExplosion();
        Entity exploder = explosion.getDirectSourceEntity();

        if (exploder instanceof PrimedTnt tnt) {
            LivingEntity igniter = tnt.getOwner();
            if (igniter != null) {
                ZombieSenseManager.broadcastSense(igniter, event.getLevel(), SenseType.GUN_SHOT);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();

        long gameTime = player.level().getGameTime();
        if (gameTime % 100 == 0) {
            StageProperty property = ZGRGame.getGameProperty().getStageProperty((ServerLevel) player.level(), player.blockPosition());
            if (property.zombieProperty.boundlessHunting) {
                ZombieSenseManager.broadcastSense(player, player.level(), SenseType.BROADCAST);
            } else if (property.zombieProperty.bloodMoonBoundlessHunting && ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(player.level().getServer())) {
                ZombieSenseManager.broadcastSense(player, player.level(), SenseType.BROADCAST);
            } else if (player.getHealth() <= 4.0F) {
                ZombieSenseManager.broadcastSense(player, player.level(), SenseType.BLEEDING);
            }
        }

    }
}
