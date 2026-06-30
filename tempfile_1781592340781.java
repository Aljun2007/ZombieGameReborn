package com.aljun.zombiegamereborn.common.entity.goal;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import java.util.EnumSet;

public class ZombieBreakBlockGoal extends Goal {
    
    private static final double REACH_DISTANCE = 5.0;
    private static final long EXPIRE_TIME = 1200L;
    
    private final Mob mob;
    private BlockPos targetPos;
    private float breakProgress;
    private long startTime;
    private BlockState targetState;
    
    public ZombieBreakBlockGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        if (!shouldBreakBlocks()) {
            return false;
        }
        
        BlockPos pos = findBlockToBreak();
        if (pos == null) {
            return false;
        }
        
        this.targetPos = pos;
        this.targetState = mob.level().getBlockState(pos);
        this.breakProgress = 0f;
        this.startTime = mob.level().getGameTime();
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (targetPos == null || !shouldBreakBlocks()) {
            return false;
        }
        
        if (mob.level().getGameTime() - startTime > EXPIRE_TIME) {
            return false;
        }
        
        BlockState currentState = mob.level().getBlockState(targetPos);
        if (currentState != targetState || !canBreakBlock(currentState)) {
            return false;
        }
        
        double distance = mob.getEyePosition().distanceToSqr(targetPos.getCenter());
        return distance <= REACH_DISTANCE * REACH_DISTANCE;
    }
    
    @Override
    public void start() {
        if (targetPos != null) {
            mob.swing(InteractionHand.MAIN_HAND);
        }
    }
    
    @Override
    public void stop() {
        if (targetPos != null && mob.level() instanceof ServerLevel) {
            ((ServerLevel) mob.level()).destroyBlockProgress(mob.getId(), targetPos, -1);
        }
        this.targetPos = null;
        this.breakProgress = 0f;
    }
    
    @Override
    public void tick() {
        if (targetPos == null) {
            return;
        }
        
        mob.getLookControl().setLookAt(targetPos.getCenter());
        
        if (mob.level() instanceof ServerLevel serverLevel) {
            double miningSpeed = ZGRZombieAttributesAPI.getMiningSpeed((net.minecraft.world.entity.monster.Zombie) mob);
            float breakSpeed = (float) (miningSpeed / getDestroyTime(targetState) * 20f);
            
            breakProgress += breakSpeed;
            
            int damageStage = (int) (breakProgress / 10f);
            if (damageStage >= 10) {
                breakBlock(serverLevel);
                return;
            }
            
            serverLevel.destroyBlockProgress(mob.getId(), targetPos, damageStage);
            
            if (mob.level().getGameTime() % 4L == 0) {
                SoundType soundType = targetState.getSoundType();
                mob.level().playSound(null, targetPos, soundType.getHitSound(), 
                    SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 8.0F, 
                    soundType.getPitch() * 0.5F);
            }
            
            if (!mob.swinging) {
                mob.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
    
    private boolean shouldBreakBlocks() {
        return mob.getTarget() == null;
    }
    
    private BlockPos findBlockToBreak() {
        Iterable<BlockPos> positions = BlockPos.betweenClosed(
            mob.blockPosition().offset(-2, -2, -2),
            mob.blockPosition().offset(2, 2, 2)
        );
        
        for (BlockPos pos : positions) {
            BlockState state = mob.level().getBlockState(pos);
            if (canBreakBlock(state) && isWithinReach(pos)) {
                return pos;
            }
        }
        return null;
    }
    
    private boolean canBreakBlock(BlockState state) {
        if (state.isAir() || state.getFluidState().isEmpty() == false) {
            return false;
        }
        
        Block block = state.getBlock();
        if (block instanceof BedrockBlock || 
            block instanceof PortalBlock ||
            block instanceof GameMasterBlock) {
            return false;
        }
        
        return ForgeHooks.canEntityDestroy(mob.level(), targetPos, mob);
    }
    
    private boolean isWithinReach(BlockPos pos) {
        return mob.getEyePosition().distanceToSqr(pos.getCenter()) <= REACH_DISTANCE * REACH_DISTANCE;
    }
    
    private float getDestroyTime(BlockState state) {
        float hardness = state.getDestroySpeed(mob.level(), targetPos);
        if (hardness < 0) {
            return 1.0f;
        }
        return hardness == 0 ? 1.0f : hardness;
    }
    
    private void breakBlock(ServerLevel level) {
        if (ForgeHooks.canEntityDestroy(level, targetPos, mob)) {
            ItemStack stack = mob.getItemBySlot(EquipmentSlot.MAINHAND);
            Block.dropResources(targetState, level, targetPos, level.getBlockEntity(targetPos), mob, stack);
            level.destroyBlock(targetPos, false, mob);
        }
        this.stop();
    }
}
