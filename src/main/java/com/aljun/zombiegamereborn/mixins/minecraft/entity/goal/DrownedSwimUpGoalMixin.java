package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedSwimUpGoal")
public class DrownedSwimUpGoalMixin {

    @Shadow
    @Final
    private Drowned drowned;

    @Redirect(
            method = "canUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;isDay()Z"
            )
    )
    private boolean makeSwimUpAvailableInDay(Level level) {
        return ZGRZombieAttributesAPI.isSunSensitive(this.drowned) && level.isDay();
    }
}