package com.aljun.zombiegamereborn.mixins.minecraft.pathfinding;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PathNavigation Mixin - 跳过冗余的 A* 重算
 *
 * <p>问题：在 vanilla 中，每次调用 moveTo(Entity/BlockPos) 都会执行两次 A* 寻路：
 * <ol>
 *   <li>moveTo() 内部调用 createPath() 完成一次 A*</li>
 *   <li>moveTo(Path) 设置 hasDelayedRecomputation=true，
 *       下一 tick 的 tick() 调用 recomputePath() 跑第二次 A*</li>
 * </ol>
 *
 * <p>优化：在 recomputePath() 执行前检查——如果当前路径还存在（!isDone()），
 * 说明 entity 仍在沿路径移动，没有必要用 A* 重新计算，直接跳过。
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow
    protected Path path;

    @Shadow
    public abstract boolean isDone();

    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "recomputePath", at = @At("HEAD"), cancellable = true)
    private void zgr_skipRedundantRecompute(CallbackInfo ci) {
        if (!(this.mob instanceof Zombie)) return;
        // 如果路径存在且尚未走完，说明 entity 正在跟随该路径移动。
        // 该路径已在 moveTo() 内部的 createPath() 中计算完毕，无需二次 A* 重算。
        if (this.zgr$simplifiedBuilderMovement()) {
            if (this.path != null && !this.isDone()) {
                ci.cancel();
            }
        }
    }

    @Unique
    private boolean zgr$simplifiedBuilderMovement() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return false;
        }
        double distanceToTarget = this.mob.distanceToSqr(target);
        int roughThreshold = ZGRGame.getGameProperty().roughPathfindingThreshold;
        // 只有在距离大于粗略寻路阈值的平方时，才启用简化建造移动
        return distanceToTarget > (double) (roughThreshold * roughThreshold) && ZGRGame.getGameProperty().simplifiedBuilderMovenment;
    }
}
