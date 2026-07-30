package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedGoToWaterGoal")
public class DrownedGoToWaterGoalMixin {

    @Shadow
    @Final
    private PathfinderMob mob;

    /**
     * 修改 canUse：不惧阳光的溺尸在有目标时不返回水中
     * 改为使用 HEAD 注入更安全
     */
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    public void onCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Zombie zombie && !ZGRZombieAttributesAPI.isSunSensitive(zombie)) {
            // 如果有攻击目标，不触发 GoToWater 行为（留在陆地上追击）
            if (this.mob.getTarget() != null) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    /**
     * 修改 canContinueToUse：不惧阳光的溺尸在有目标时停止返回水中
     */
    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    public void onCanContinueToUse(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Zombie zombie && !ZGRZombieAttributesAPI.isSunSensitive(zombie)) {
            if (this.mob.getTarget() != null) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}