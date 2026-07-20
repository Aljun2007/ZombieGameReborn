package com.aljun.zombiegamereborn.mixins.musketmod;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ewewukek.musketmod.BulletEntity")
public abstract class BulletEntityMixin {
    
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHitMixin(EntityHitResult hitResult, CallbackInfo ci) {
        Entity target = hitResult.getEntity();
        if (!(target instanceof Zombie zombie)) return;
        boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;
        if (isUsingShield) {
            ci.cancel();
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieShieldGoal shieldGoal = null;
            if (data != null) {
                shieldGoal = data.getZombieShieldGoal();
            }
            if (shieldGoal == null) return;
            ItemStack weapon = zombie.getMainHandItem();
            shieldGoal.onShieldBlock(weapon, zombie);
        }
    }
}
