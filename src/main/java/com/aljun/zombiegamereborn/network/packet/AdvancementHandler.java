package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.api.ZGRPlayerAPI;
import com.aljun.zombiegamereborn.common.player.TimeBroadcast;
import com.aljun.zombiegamereborn.common.player.capability.IPlayerData;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdvancementHandler {

    private static final ResourceLocation PIGLIN_COLLISION =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_piglin_collision");
    private static final ResourceLocation VOID_TIME_CORRECTION =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_void_time_correction");
    private static final ResourceLocation GUARD_BETRAYAL =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_guard_betrayal");
    private static final ResourceLocation SHIELD_BLOCK =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_shield_block");
    private static final ResourceLocation MUSKET_HIT =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_musket_hit");
    private static final ResourceLocation ZOMBIE_HORDE =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_zombie_horde");
    private static final ResourceLocation ROTTEN_HILL =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_rotten_hill");
    private static final ResourceLocation COVER_IN_DEBRIS =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_cover_in_debris");
    private static final ResourceLocation DIAMOND_PROTECTS =
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "epic_diamond_protects");
    private static final long UNDERGROUND_THRESHOLD = TimeBroadcast.UNDERGROUND_ESTIMATE_THRESHOLD;
    public static final int HORDE_THRESHOLD = 80;
    private static final long KILL_THRESHOLD = 500;

    private static void award(ServerPlayer player, ResourceLocation id, String criterion) {
        if (player == null) return;
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    // ===================== 抱歉！我不是故意的 =====================

    public static void grantPiglinCollision(ServerPlayer player) {
        award(player, PIGLIN_COLLISION, "piglin_collision");
    }

    // ===================== 虚空矫时 =====================

    private static boolean meetsUndergroundCondition(ServerPlayer player) {
        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);

        long undergroundGameTime = data.getUndergroundGameTime();
        if (undergroundGameTime <= 0) return false;

        long elapsed = player.server.overworld().getGameTime() - undergroundGameTime;
        return elapsed >= UNDERGROUND_THRESHOLD;
    }

    @SubscribeEvent
    public static void onCraftClock(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getCrafting().is(Items.CLOCK)) return;
        if (meetsUndergroundCondition(player)) {
            award(player, VOID_TIME_CORRECTION, "clock_in_inventory");
        }
    }

    @SubscribeEvent
    public static void onPickupClock(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getStack().is(Items.CLOCK)) return;
        if (meetsUndergroundCondition(player)) {
            award(player, VOID_TIME_CORRECTION, "clock_in_inventory");
        }
    }

    // ===================== 出尔反尔 =====================

    public static void grantGuardBetrayal(ServerPlayer player) {
        award(player, GUARD_BETRAYAL, "guard_betrayal");
    }

    // ===================== 这套也不吃？ =====================

    public static void grantShieldBlock(ServerPlayer player) {
        award(player, SHIELD_BLOCK, "shield_block");
    }

    public static void grantMusketHit(ServerPlayer player) {
        award(player, MUSKET_HIT, "musket_hit");
    }

    public static void grantZombieHorde(ServerPlayer player) {
        award(player, ZOMBIE_HORDE, "zombie_horde");
    }

    @SubscribeEvent
    public static void onZombieKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Zombie)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        IPlayerData data = ZGRPlayerAPI.getPlayerData(player);

        long kills = data.getTotalZombieKills() + 1;
        data.setTotalZombieKills(kills);

        if (kills == KILL_THRESHOLD) {
            award(player, ROTTEN_HILL, "rotten_hill");
        }
    }

    public static void grantRottenHill(ServerPlayer player) {
        award(player, ROTTEN_HILL, "rotten_hill");
    }

    public static void grantCoverInDebris(ServerPlayer player) {
        award(player, COVER_IN_DEBRIS, "cover_in_debris");
    }

    public static void grantDiamondProtects(ServerPlayer player) {
        award(player, DIAMOND_PROTECTS, "diamond_protects");
    }
}
