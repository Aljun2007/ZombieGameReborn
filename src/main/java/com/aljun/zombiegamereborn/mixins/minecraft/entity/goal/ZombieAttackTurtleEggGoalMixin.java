package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RemoveBlockGoal.class)
public class ZombieAttackTurtleEggGoalMixin {
    @Shadow
    @Final
    private Mob removerMob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    public void canUse(CallbackInfoReturnable<Boolean> cir) {
        if (ZGRGame.getGameProperty().disableTurtleEggSeeking) {
            cir.setReturnValue(false);
        }
    }
}
