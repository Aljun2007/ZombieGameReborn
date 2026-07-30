package com.aljun.zombiegamereborn.debug.events;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.debug.ZGRDebug;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Objects;

@Mod.EventBusSubscriber
public class ZGRDebugEvents {
    public static Logger LOGGER = LogUtils.getLogger();

    //为游戏中添加调试快捷工具，一共包含 调时间（day, night）、秒杀鱼、强物品、调试棒、生命血包
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ItemStack stack = player.getMainHandItem();
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                String debugType = tag.getString(ZombieGameReborn.MOD_ID + ".debug.itemtype");
                if ("snatcher".equals(debugType)) {
                    if (event.getTarget() instanceof LivingEntity livingEntity) {
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.MAINHAND).copy());
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.OFFHAND).copy());
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.HEAD).copy());
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.CHEST).copy());
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.LEGS).copy());
                        player.addItem(livingEntity.getItemBySlot(EquipmentSlot.FEET).copy());
                    }
                } else if ("killer".equals(debugType)) {
                    event.getTarget().kill();
                }
            }
        }
    }

    //为游戏中添加调试快捷工具，一共包含 调时间（day, night）、秒杀鱼、强物品、调试棒、生命血包
    @SubscribeEvent
    public static void onUse(EntityItemPickupEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ItemStack stack = event.getItem().getItem();
                CompoundTag tag = stack.getTag();
                if (tag == null) return;
                String debugType = tag.getString(ZombieGameReborn.MOD_ID + ".debug.itemtype");
                switch (debugType) {
                    case "heal" -> {
                        player.removeAllEffects();
                        player.clearFire();
                        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, 8));
                        player.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 8));
                    }
                    case "day" -> {
                        try {
                            for (ServerLevel serverlevel : Objects.requireNonNull(event.getEntity().getServer()).getAllLevels()) {
                                serverlevel.setDayTime(1000L);
                            }
                        } catch (NullPointerException ignore) {
                        }
                    }
                    case "night" -> {
                        try {
                            for (ServerLevel serverlevel : Objects.requireNonNull(event.getEntity().getServer()).getAllLevels()) {
                                serverlevel.setDayTime(13000L);
                            }
                        } catch (NullPointerException ignore) {
                        }
                    }
                    case "test" -> ZGRDebug.testItem();
                }
            }
        }
    }
}
