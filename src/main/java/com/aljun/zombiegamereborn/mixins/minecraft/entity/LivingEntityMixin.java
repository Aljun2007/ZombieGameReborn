package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntity Mixin - 处理僵尸举盾时的受伤音效静音
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * 注入 playHurtSound 方法，在僵尸举盾时取消播放受伤音效
     */
    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void playHurtSoundMixin(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 只处理僵尸实体
        if (!(entity instanceof Zombie zombie)) return;

        // 检查是否正在使用盾牌
        boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;

        if (isUsingShield) {
            ci.cancel();
            return;
        }

        // 检查刚破盾状态
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data != null) {
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal != null && shieldGoal.wasShieldJustBroken()) {
                ci.cancel();
            }
        }
    }
}