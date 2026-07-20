package com.aljun.zombiegamereborn.mixins.minecraft.entity.goal;

import com.aljun.zombiegamereborn.common.entity.accessor.INearestAttackableTargetGoalAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin implements INearestAttackableTargetGoalAccessor {

    @Accessor("targetType")
    @Override
    public abstract Class<? extends LivingEntity> get_targetType();

    @Accessor("targetConditions")
    @Override
    public abstract TargetingConditions get_targetingConditions();

}
