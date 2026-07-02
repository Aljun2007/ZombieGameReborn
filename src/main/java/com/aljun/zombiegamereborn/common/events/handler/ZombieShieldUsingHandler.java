package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 僵尸盾牌使用事件处理器
 * <p>
 * 负责处理僵尸举盾时的特殊行为：
 * - 抗击退免疫
 * - 格挡音效播放
 * - 斧头破盾机制
 */
@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZombieShieldUsingHandler {

    /**
     * 处理击退事件 - 举盾或刚破盾时免疫击退
     */
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            // 检查是否正在使用盾牌
            boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;

            if (isUsingShield) {
                event.setCanceled(true);
                return;
            }

            // 检查是否刚刚破盾（通过 ShieldGoal 的状态判断）
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal != null && shieldGoal.wasShieldJustBroken()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 处理受伤事件 - 播放格挡音效和处理斧头破盾
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (event.getEntity() instanceof Zombie zombie) {
            // 检查是否正在使用盾牌
            if (!zombie.isUsingItem()) return;
            if (!(zombie.getUseItem().getItem() instanceof ShieldItem)) return;
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal == null) return;
            Entity attacker = event.getSource().getDirectEntity();
            if (attacker instanceof LivingEntity livingAttacker) {
                ItemStack weapon = livingAttacker.getMainHandItem();
                shieldGoal.onShieldBlock(weapon, livingAttacker);
            }
        }
    }
}

