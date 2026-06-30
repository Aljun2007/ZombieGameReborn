package com.aljun.zombiegamereborn.mixins.musketmod;

import com.aljun.zombiegamereborn.common.entity.sense.SenseType;
import com.aljun.zombiegamereborn.common.entity.sense.ZombieSenseManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ewewukek.musketmod.GunItem")
public class GunItemFireMixin {
    @Inject(
            method = "fire(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("TAIL")
    )
    private void on_fire(LivingEntity entity, ItemStack stack, Vec3 direction, Vec3 smokeOffset, CallbackInfo ci) {
        if (entity.level().isClientSide()) return;
        ZombieSenseManager.broadcastSense(entity, entity.level(), SenseType.GUN_SHOT);
    }
}
