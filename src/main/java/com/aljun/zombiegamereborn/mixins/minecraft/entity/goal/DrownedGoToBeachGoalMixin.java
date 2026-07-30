package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedGoToBeachGoal")
public class DrownedGoToBeachGoalMixin {

    @Shadow
    @Final
    private Drowned drowned;

    /**
     * 修改 canUse 中的 !isDay() 判断
     * 原版：!this.drowned.level().isDay()
     * 修改后：不惧阳光的溺尸全天候返回 true
     */
    @Redirect(
            method = "canUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;isDay()Z"
            )
    )
    private boolean redirectIsDayInCanUse(Level level) {
        if (!ZGRZombieAttributesAPI.isSunSensitive(this.drowned)) {
            return false; // 让 !isDay() 始终为 true（全天候可上岸）
        }
        return level.isDay(); // 惧阳光的保持原版
    }

    /**
     * 修改 canContinueToUse 中的 !isDay() 判断
     * 确保白天也能继续执行上岸行为
     */
    @Redirect(
            method = "canContinueToUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;isDay()Z"
            )
    )
    private boolean redirectIsDayInCanContinueToUse(Level level) {
        if (!ZGRZombieAttributesAPI.isSunSensitive(this.drowned)) {
            return false; // 让 !isDay() 始终为 true
        }
        return level.isDay();
    }
}