package com.aljun.zombiegamereborn.mixins.minecraft.pathfinding;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WalkNodeEvaluator Mixin - 向原版寻路系统注入梯子感知能力
 * <p>
 * 修改思路（类似 EnhancedAI）：
 * 1. 将梯子标记为 WALKABLE，使 A* 寻路器能将其纳入路径节点
 * 2. 在梯子位置添加上方邻居连接，使路径能沿梯子垂直生成
 * <p>
 * 配合 aiStep mixin 中的 yya 输入，僵尸在寻路到梯子后自动向上攀爬，
 * 实现"被动攀爬"——僵尸为追击目标而自然走梯子，无需独立的扫描 Goal
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    @Inject(
            method = "getBlockPathTypeRaw(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void zgr_onGetBlockPathTypeRaw(BlockGetter blockGetter, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState state = blockGetter.getBlockState(pos);
        if (state.is(Blocks.LADDER)) {
            cir.setReturnValue(BlockPathTypes.WALKABLE);
        }
    }

    /**
     * 在梯子位置添加上方邻居连接，使 A* 能沿梯子向上生成路径。
     * 注意：只添加上方连接（y+1），不添加下方连接（y-1），
     * 否则 PathFinder 在扩展上方节点时会将下方节点视为"新节点"重新加入，
     * 导致 parent 链循环 (L.parent = L+1, L+1.parent = L)，reconstructPath 死循环。
     */
    @Inject(method = "getNeighbors", at = @At("RETURN"), cancellable = true)
    private void zgr_onGetNeighbors(Node[] neighbors, Node node, CallbackInfoReturnable<Integer> cir) {

        if (!(this.mob instanceof Zombie zombie)) return;
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null) return;
        if (!data.canLadderClimb()) return;

        int count = cir.getReturnValue();
        if (count >= neighbors.length) return;

        BlockPos pos = new BlockPos(node.x, node.y, node.z);
        BlockState state = this.level.getBlockState(pos);
        if (!state.is(Blocks.LADDER)) return;

        int aboveY = node.y + 1;
        if (aboveY <= this.level.getMaxBuildHeight()) {
            BlockPos abovePos = new BlockPos(node.x, aboveY, node.z);
            BlockState aboveState = this.level.getBlockState(abovePos);
            if (aboveState.is(Blocks.LADDER)) {
                Node aboveNode = this.getNode(node.x, aboveY, node.z);
                aboveNode.type = BlockPathTypes.WALKABLE;
                neighbors[count++] = aboveNode;
            }
        }

        if (count != cir.getReturnValue()) {
            cir.setReturnValue(count);
        }
    }
}
