package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZombieSenseHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            var stageProperty = ZGRGame.getGameProperty().getGlobalStage(event.getServer());
            // 不再需要重复调用 getStageProperty()
            // ZombieSenseManager.refresh 已在 GamePropertyRefresher 中处理
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Level level = event.getEntity().level();
        LivingEntity victim = event.getEntity();
        if (ZombieUtils.zombieAttackableEntity(victim)) {
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
        if (event.getUseBlock() == Event.Result.DENY) return;
        ZombieSenseManager.broadcastSense(event.getEntity(), event.getLevel(), SenseType.BLOCK);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) return;

        Explosion explosion = event.getExplosion();
        Entity exploder = explosion.getExploder();

        if (exploder instanceof PrimedTnt tnt) {
            LivingEntity igniter = tnt.getOwner();
            if (igniter != null) {
                ZombieSenseManager.broadcastSense(igniter, event.getLevel(), SenseType.GUN_SHOT);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        
        // 检查玩家血量是否低于4点 (2颗心)
        if (player.getHealth() < 4.0F || ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(player.level().getServer())) {
            // 使用游戏总刻数作为计时器，每1000tick触发一次
            long gameTime = player.level().getGameTime();
            if (gameTime % 1000 == 0) {
                ZombieSenseManager.broadcastSense(player, player.level(), SenseType.BLEEDING);
            }
        }
    }
}

