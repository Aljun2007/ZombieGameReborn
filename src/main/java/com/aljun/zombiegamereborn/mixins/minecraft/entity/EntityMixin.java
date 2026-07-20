package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void fireImmuneMixin(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide) return;
        if (entity instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            if (data == null) return;

            MinecraftServer server = zombie.getServer();
            if (server != null && ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).holyCleansing) {
                cir.setReturnValue(false);
                return;
            }

            cir.setReturnValue(data.fireImmune() || cir.getReturnValue());
        }
    }

    /**
     * 劫持 checkInsideBlocks 中的 blockstate.entityInside 调用，
     * 用于拦截方块对实体的碰撞效果（如岩浆块、仙人掌、甜浆果丛等）。
     */
    @SuppressWarnings("all")
    @Redirect(
            method = "checkInsideBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private void redirectEntityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
        // 在此处添加自定义拦截逻辑
        // 例如：检查实体类型、方块类型，选择性取消或修改 entityInside 效果
        if (entity instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            if (data != null && data.isBlockStabImmune()) {
                if (blockState.is(Blocks.CACTUS)) {
                    return;
                } else if (blockState.is(Blocks.SWEET_BERRY_BUSH)) {
                    zombie.makeStuckInBlock(blockState, new Vec3((double)0.8F, 0.75D, (double)0.8F));
                    return;
                } else if (blockState.getBlock().getClass().getName().equals("xxrexraptorxx.minetraps.blocks.BlockBarbedWire")
                        || blockState.getBlock().getClass().getName().equals("xxrexraptorxx.minetraps.blocks.BlockBarbedWireFence")) {
                    zombie.makeStuckInBlock(blockState, new Vec3(0.25F, 0.05F, 0.25F));
                    return;
                } else if (blockState.getBlock().getClass().getName().equals("xxrexraptorxx.minetraps.blocks.BlockNailTrap")
                || blockState.getBlock().getClass().getName().equals("xxrexraptorxx.minetraps.blocks.BlockSpikes")) {
                    return;
                }
            } else {
                blockState.entityInside(level, pos, entity);
            }
        }
        blockState.entityInside(level, pos, entity);
    }

}
