package com.aljun.zombiegamereborn.mixins.entity.goal;

import com.aljun.zombiegamereborn.common.entity.goal.target.accessor.ITargetGoalAccessor;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin implements ITargetGoalAccessor {

    @Accessor("mustSee")
    @Override
    public abstract void set_mustSee(boolean mustSee);

    @Accessor("mustSee")
    @Override
    public abstract boolean get_mustSee();
}
