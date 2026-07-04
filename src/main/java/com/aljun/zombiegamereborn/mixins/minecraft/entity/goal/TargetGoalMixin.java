package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.common.entity.accessor.ITargetGoalAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin implements ITargetGoalAccessor {

    @Shadow
    @Final
    protected Mob mob;

    @Accessor("mustSee")
    @Override
    public abstract void set_mustSee(boolean mustSee);

    @Accessor("mustSee")
    @Override
    public abstract boolean get_mustSee();

    @Inject(method = "canAttack", at = @At("RETURN"), cancellable = true)
    public void canAttack(@Nullable LivingEntity livingEntity, TargetingConditions conditions, CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof Zombie && livingEntity instanceof Zombie) {
            cir.setReturnValue(false);
        }
    }

}
