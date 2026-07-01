package com.aljun.zombiegamereborn.mixins.pointblank;

import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import com.vicmatskiv.pointblank.client.GunClientState;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.network.MainHeldSimplifiedStateSyncRequest")
public class MainHeldSimplifiedStateSyncRequestMixin {

    @Shadow
    private GunClientState.FireState simplifiedState;

    @Inject(
            method = "handleEnqueued",
            at = @At("TAIL")
    )
    private void on_handleEnqueued(Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        if (simplifiedState != GunClientState.FireState.FIRE_SINGLE
                && simplifiedState != GunClientState.FireState.FIRE_AUTO
                && simplifiedState != GunClientState.FireState.FIRE_BURST) {
            return;
        }

        ServerPlayer sender = ctx.get().getSender();
        if (sender == null) return;

        Level level = sender.level();
        if (level.isClientSide()) return;

        ItemStack gun = sender.getMainHandItem();
        boolean isSilenced = zGR1_20_1$isSilencedShot(sender,gun);
        SenseType senseType = isSilenced ? SenseType.GUN_SHOT_SILENCED : SenseType.GUN_SHOT;
        ZombieSenseManager.broadcastSense(sender, level, senseType);
    }

    @Unique
    private boolean zGR1_20_1$isSilencedShot(Player player, ItemStack gun) {
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
