package com.aljun.zombiegamereborn.mixins.pointblank;

import com.aljun.zombiegamereborn.common.entity.sense.PointblankCallback;
import net.minecraft.server.level.ServerPlayer;
import com.vicmatskiv.pointblank.NetworkService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.network.MainHeldSimplifiedStateSyncRequest")
public class MainHeldSimplifiedStateSyncRequestMixin {
     @Inject(
            method = "handleEnqueued",
            at = @At("HEAD")
    )
    private void on_handleEnqueued(NetworkService.MessageContext ctx, CallbackInfo ci) {
        ServerPlayer sender = ctx.getSender() instanceof ServerPlayer sp ? sp : null;
        if (sender == null || sender.level().isClientSide()) return;
        PointblankCallback.onGunSync(sender, this);
    }
}
