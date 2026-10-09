package com.aljun.zombiegamereborn.common.entity.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public interface INearestAttackableTargetGoalAccessor {
    Class<? extends LivingEntity> get_targetType();
    TargetingConditions get_targetingConditions();
}
