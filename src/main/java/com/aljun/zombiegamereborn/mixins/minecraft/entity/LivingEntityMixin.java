package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieShieldGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected boolean jumping;

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void playHurtSoundMixin(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof Zombie zombie)) return;

        boolean isUsingShield = zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem;

        if (isUsingShield) {
            ci.cancel();
            return;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data != null) {
            ZombieShieldGoal shieldGoal = data.getZombieShieldGoal();
            if (shieldGoal != null && shieldGoal.wasShieldJustBroken()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void aiStepMixin(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide) return;

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null || !data.canLadderClimb()) return;

        if (zombie.onClimbable()) {
            // 阻止 MoveControl 的 jump() 干扰平滑攀爬
            this.jumping = false;

            zombie.yya = 0.25F;

            // 上方还有可攀爬方块 → 正在连续攀爬中，抑制水平移动防脱离
            BlockPos pos = zombie.blockPosition();
            if (zombie.level().getBlockState(pos.above()).is(BlockTags.CLIMBABLE)) {
                zombie.zza = 0.0F;
                zombie.xxa = 0.0F;
            }
        }
    }
}