package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.accessor.IDrownedAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Drowned.class)
public class DrownedMixin implements IDrownedAccessor {

    @Shadow
    boolean searchingForLand;

    /**
     * 修改 okTarget：让不惧阳光的溺尸全天候攻击
     * 原版：白天只能攻击水中的目标
     * 修改后：不惧阳光的溺尸可以攻击任何目标
     */
    @Inject(method = "okTarget", at = @At("HEAD"), cancellable = true)
    private void onOkTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        Drowned self = (Drowned) (Object) this;

        if (target == null) {
            cir.setReturnValue(false);
            return;
        }

        // 不惧阳光 -> 全天候攻击任何目标
        if (!ZGRZombieAttributesAPI.isSunSensitive(self)) {
            cir.setReturnValue(true);
        }

        // 惧阳光的保持原版逻辑（让原方法执行）
        // 原版逻辑在 HEAD 注入后仍会执行，所以我们不设置返回值
    }

    @Override
    public boolean get_searchingForLand() {
        return this.searchingForLand;
    }
}