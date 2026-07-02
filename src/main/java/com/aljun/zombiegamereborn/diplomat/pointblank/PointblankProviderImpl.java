package com.aljun.zombiegamereborn.diplomat.pointblank;

import com.aljun.zombiegamereborn.common.entity.sense.PointblankCallback;
import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.network.MainHeldSimplifiedStateSyncRequest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

public class PointblankProviderImpl implements IPointblankProvider {

    private Field simplifiedStateField;

    @Override
    public void registerCallback() {
        try {
            this.simplifiedStateField = MainHeldSimplifiedStateSyncRequest.class.getDeclaredField("simplifiedState");
            this.simplifiedStateField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            return;
        }
        PointblankCallback.setHandler(this::handleGunSync);
    }

    @Override
    public boolean isGunLoaded(ItemStack stack) {
        if (stack.getItem() instanceof GunItem) {
            return stack.getOrCreateTag().getInt("ammo") > 0;
        }
        return false;
    }

    private void handleGunSync(ServerPlayer sender, Object packetObj) {
        MainHeldSimplifiedStateSyncRequest packet = (MainHeldSimplifiedStateSyncRequest) packetObj;

        GunClientState.FireState state;
        try {
            state = (GunClientState.FireState) simplifiedStateField.get(packet);
        } catch (IllegalAccessException e) {
            return;
        }

        if (state != GunClientState.FireState.FIRE_SINGLE
                && state != GunClientState.FireState.FIRE_AUTO
                && state != GunClientState.FireState.FIRE_BURST) {
            return;
        }

        ItemStack gun = sender.getMainHandItem();
        boolean isSilenced = isSilencedShot(gun);
        SenseType senseType = isSilenced ? SenseType.GUN_SHOT_SILENCED : SenseType.GUN_SHOT;
        ZombieSenseManager.broadcastSense(sender, sender.level(), senseType);
    }

    private boolean isSilencedShot(ItemStack gun) {
        if (gun.isEmpty() || !(gun.getItem() instanceof GunItem gunItem)) return false;

        float volume;
        SoundFeature.SoundDescriptor fsv = SoundFeature.getFireSoundAndVolume(gun);
        if (fsv != null) {
            volume = fsv.volume();
        } else {
            volume = gunItem.getFireSoundVolume();
        }
        return volume < 5.0F;
    }
}
