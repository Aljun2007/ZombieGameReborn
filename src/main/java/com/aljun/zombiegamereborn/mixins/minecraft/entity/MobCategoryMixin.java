package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {
    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetMaxInstancesPerChunk(CallbackInfoReturnable<Integer> cir) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (ZGRGame.getGameProperty().getGlobalStage(server).holyCleansing) {
            cir.setReturnValue(0);
            return;
        }
        double modifier = ZGRGame.getGameProperty().getGlobalStage(server).zombieCountModify;
        if (modifier >= 0d && modifier != 1d) {
            if ((Object) this == MobCategory.MONSTER) {
                cir.setReturnValue((int) (cir.getReturnValue() * modifier));
            }
        }
    }
}
