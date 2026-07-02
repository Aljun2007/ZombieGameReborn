package com.aljun.zombiegamereborn.mixins.musketmod;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import ewewukek.musketmod.BulletEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Musketmod BulletEntity Mixin
 * 
 * 拦截子弹命中逻辑，当目标是举盾的僵尸时取消伤害
 */
@Mixin(BulletEntity.class)
public abstract class BulletEntityMixin {
    
    /**
     * 注入 onHitEntity 方法，在僵尸举盾时取消子弹伤害
     * 
     * @param hitResult 命中结果（包含被命中的实体）
     * @param ci 回调信息（用于取消）
     */
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHitMixin(EntityHitResult hitResult, CallbackInfo ci) {
        Entity target = hitResult.getEntity();

        if (!(target instanceof Zombie zombie)) return;

        boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;

        if (isUsingShield) {
            ci.cancel();
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal == null) return;
            ItemStack weapon = zombie.getMainHandItem();
            shieldGoal.onShieldBlock(weapon, zombie);
        }
    }
}
