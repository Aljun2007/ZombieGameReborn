package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

import static com.aljun.zombiegamereborn.utils.ZombieUtils.isCuring;

public class ZombiePlaceBlockGoal extends Goal {
    private final Zombie zombie;
    private final IZombieData data;

    public static final long PLACE_COOLDOWN = 10L;

    public void setDefaultPlaceBlock(Supplier<BlockState> defaultPlaceBlock) {
        this.defaultPlaceBlock = defaultPlaceBlock;
    }

    private Supplier<BlockState> defaultPlaceBlock = Blocks.DIRT::defaultBlockState;

    public boolean place(BlockPos blockPos, BlockState blockState) {
        if (isCuring(this.zombie)) return false;
        if (this.lastPlaceTime + PLACE_COOLDOWN >= this.zombie.level().getGameTime()) {
            return true;
        }
        if (this.checkState(blockState) && this.checkPos(blockPos)) {
            this.succeedPlace(blockPos, blockState);
            return true;
        } else {
            return false;
        }
    }

    public boolean placeIgnoreCoolDown(BlockPos blockPos, BlockState blockState) {
        if (isCuring(this.zombie)) return false;
        if (this.checkState(blockState) && this.checkPos(blockPos)) {
            this.succeedPlace(blockPos, blockState);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkState(BlockState blockState) {
        return (!blockState.isAir() && blockState.getFluidState().isEmpty());
    }

    private boolean checkPos(BlockPos pos) {
        if (zombie.level().isOutsideBuildHeight(pos)) return false;
        return !(zombie.blockPosition().distSqr(pos) > ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR);
    }

    private void succeedPlace(BlockPos blockPos, BlockState blockState) {
        ServerLevel level = (ServerLevel) this.zombie.level();
        level.setBlock(blockPos, blockState, 3);
        SoundType soundType = blockState.getSoundType();
        this.zombie.level().playSound(null, blockPos, soundType.getBreakSound(), SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 8.0F, soundType.getPitch() * 0.5F);
        if (!this.zombie.swinging) {
            this.zombie.swing(InteractionHand.MAIN_HAND);
        }
        this.lastPlaceTime = this.zombie.level().getGameTime();
    }
    private long lastPlaceTime = 0;

    private boolean canPlaceBlock() {
        return ZGRGame.Rules.canZombiePlaceBlock(zombie.getServer());
    }

    public ZombiePlaceBlockGoal(Zombie zombie, IZombieData data) {
        this.zombie = zombie;
        this.data = data;
    }

    @Override
    public boolean canUse() {
        return false;
    }

    public BlockState getPlaceBlock() {
        return this.defaultPlaceBlock.get();
    }
}
