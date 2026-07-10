package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {
    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetMaxInstancesPerChunk(CallbackInfoReturnable<Integer> cir) {
        if(ZGRGame.getGameProperty().getCurrentStageProperty().holyCleansing) {
            cir.setReturnValue(0);
            return;
        }
        double i = ZGRGame.getGameProperty().getCurrentStageProperty().zombieCountModify;
        if (i >= 0d && i != 1d) {
            if ((Object) this == MobCategory.MONSTER) {
                cir.setReturnValue((int) (cir.getReturnValue() * ZGRGame.getGameProperty().getCurrentStageProperty().zombieCountModify));
            }
        }
    }
}
