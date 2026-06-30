package com.aljun.zombiegamereborn.diplomat.tacz;

import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TaczProviderImpl implements ITaczProvider {

    @Override
    public void registerEvents(IEventBus forgeBus) {
        forgeBus.register(this);
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
