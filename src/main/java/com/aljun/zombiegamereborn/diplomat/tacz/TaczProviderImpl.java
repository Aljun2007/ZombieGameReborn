package com.aljun.zombiegamereborn.diplomat.tacz;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TaczProviderImpl implements ITaczProvider {

    @Override
    public void registerEvents(IEventBus forgeBus) {
        forgeBus.register(this);
    }

    @Override
    public boolean isGunLoaded(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            return iGun.getCurrentAmmoCount(stack) > 0;
        }
        return false;
    }

    @SubscribeEvent
    public void onGunShoot(GunFireEvent event) {
        if (event.getLogicalSide().isClient()) return;

        LivingEntity shooter = event.getShooter();
        Level level = shooter.level();

        // 检测是否是消音器
        boolean isSilenced = isSilencedShot(event);

        // 广播枪声感知
        SenseType senseType = isSilenced ? SenseType.GUN_SHOT_SILENCED : SenseType.GUN_SHOT;
        ZombieSenseManager.broadcastSense(shooter, level, senseType);
    }

    /**
     * 处理 TACZ 枪械伤害事件 - 僵尸举盾时免疫伤害
     */
    @SubscribeEvent
    public void onTaczGunHurt(EntityHurtByGunEvent.Pre event) {
        // 只处理服务端
        if (event.getLogicalSide().isClient()) return;
        
        Entity target = event.getHurtEntity();
        if (!(target instanceof Zombie zombie)) return;
        
        // 检查是否正在使用盾牌
        boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;
        
        if (isUsingShield) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal == null) return;
            LivingEntity attacker = event.getAttacker();
            ItemStack weapon = null;
            if (attacker != null) {
                weapon = attacker.getMainHandItem();
                shieldGoal.onShieldBlock(weapon, attacker);
                event.setCanceled(true);
            }
        }
    }

    private boolean isSilencedShot(GunFireEvent event) {
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(event.getShooter()).getCacheProperty();
        if (cacheProperty != null) {
            Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
            return silence.right();
        } else {
            return false;
        }
    }
}
