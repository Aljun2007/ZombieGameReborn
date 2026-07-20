package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.aljun.zombiegamereborn.utils.ZombieUtils.isCuring;

public class ZombieBreakBlockGoal extends Goal {

    private final Zombie zombie;
    private BlockPos pos;
    private float breakProgress;
    private long startTime;
    private final IZombieData data;
    private BlockState state;
    private ServerLevel level;
    private float miningSpeed = 1.0f;
    private int cachedEfficiencyLevel = 0;

    private long lastFailTime = 0;

    public ZombieBreakBlockGoal(Zombie zombie,IZombieData data) {
        this.zombie = zombie;
        //this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.data = data;
        this.level = (ServerLevel) zombie.level();
    }

    @Override
    public boolean canUse() {
        return !this.isDone() && ZGRGame.Rules.canZombieBreakBlock(this.zombie.getServer())
                && !isCuring(this.zombie);
    }

    private boolean canContinueBreaking() {
        return positionVerification(this.pos)
                && blockVerification(this.pos, this.state)
                && ZGRGame.Rules.canZombieBreakBlock(this.zombie.getServer())
                && !isCuring(this.zombie);
    }

    @Override
    public void start() {
        if (pos != null) {
            zombie.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {

        if (this.isDone()) return;

        if (this.canContinueBreaking()) {
            //暂且使用原版攻击目标
            if (this.zombie.getTarget() == null) {
                this.zombie.getLookControl().setLookAt(MathUtils.blockPosToVec3(this.pos));
            }

            this.state = this.level.getBlockState(this.pos);
            this.breakProgress += this.getBreakProgress(this.state, this.pos,this.zombie.getMainHandItem()) *this.miningSpeed;
            if (this.breakProgress >= 1f) {
                this.succeedBreakBlock(this.level);
            } else {
                this.level.destroyBlockProgress(this.zombie.getId(), this.pos, (int) (breakProgress * 10f) - 1);
                if (!this.zombie.swinging) {
                    this.zombie.swing(InteractionHand.MAIN_HAND);
                }
                if ((this.level.getGameTime() - this.startTime) % 4L == 0) {
                    SoundType soundType = this.state.getSoundType();
                    this.level.playSound(null, this.pos, soundType.getHitSound(), SoundSource.BLOCKS,
                            (soundType.getVolume() + 1.0F) / 8.0F, soundType.getPitch() * 0.5F);
                }
            }
        } else {
            this.failBreak();
        }
    }
    /**
     * 检查方块是否可破坏
     */
    private boolean canBreakBlock(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }

        // 不能破坏流体
        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        // 不能破坏游戏大师方块（基岩、命令方块等）
        if (state.getBlock() instanceof GameMasterBlock) {
            return false;
        }

        if (state.getBlock().defaultDestroyTime() < 0 ) {
            return false;
        }

        if (pos == null) {
            return false;
        }

        // 检查 Forge 的方块破坏权限
        return ForgeHooks.canEntityDestroy(this.level, pos, this.zombie);
    }

    /**
     * 计算每刻的挖掘进度
     * 参考原版 Player.getDestroySpeed() 的逻辑
     */
    private float getBreakProgress(BlockState state, BlockPos pos,ItemStack stack) {
        float f = state.getDestroySpeed(this.level, pos);
        if (f == -1.0F) {
            return 0;
        } else {
            boolean a = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
            int i = a ? 30 : 100;
            return (getDigSpeed(stack,state) / f / (float) i);
        }
    }

    /**
     * 计算挖掘速度 - 参考原版 Player.getDigSpeed()
     */
    private float getDigSpeed(ItemStack stack,BlockState state) {
        float f = stack.getDestroySpeed(state);
        if (f > 1.0F) {
            if (this.cachedEfficiencyLevel > 0 && !stack.isEmpty()) {
                f += (float)(this.cachedEfficiencyLevel * this.cachedEfficiencyLevel + 1);
            }
        }

        if (MobEffectUtil.hasDigSpeed(this.zombie)) {
            f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this.zombie) + 1) * 0.2F;
        }

        if (this.zombie.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float f1 = switch (Objects.requireNonNull(this.zombie.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            f *= f1;
        }

        if (this.zombie.isEyeInFluidType(ForgeMod.WATER_TYPE.get()) && !EnchantmentHelper.hasAquaAffinity(this.zombie)) {
            f /= 5.0F;
        }

        if (!this.zombie.onGround()) {
            f /= 5.0F;
        }

        return f;
    }

    /**
     * 破坏方块
     */
    private void succeedBreakBlock(ServerLevel level) {
        if (pos == null) {
            return;
        }

        if (!ForgeHooks.canEntityDestroy(level, pos, zombie)) {
            this.stop();
            return;
        }

        ItemStack stack = zombie.getItemBySlot(EquipmentSlot.MAINHAND);

        // 只有工具正确时才掉落资源
        if (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), zombie, stack);
        }

        level.destroyBlock(pos, false, zombie);
        this.stop();
    }

    private BiConsumer<Zombie,IZombieData> halfwayFailureCallback = (a, b)->{};

    public boolean tryToBreak(BlockPos pos, BiConsumer<Zombie,IZombieData> halfwayFailureCallback) {
        if (this.tryToBreak(pos) ) {
            this.halfwayFailureCallback = halfwayFailureCallback;
            return true;
        } else return false;
    }



    public boolean tryToBreak(BlockPos pos) {
        if (pos.equals(this.pos) && !this.isDone) {
            return false;
        }

        BlockState state = this.level.getBlockState(pos);

        // 硬度 = 0 的方块（火把等）直接破坏，无需挖掘进度
        if (positionVerification(pos) && blockVerification(pos, state)) {
            if (state.getDestroySpeed(this.level, pos) == 0) {
                this.instantBreak(pos, state);
                return true;
            }
            this.startBreak(pos);
            return true;
        }
        return false;
    }

    private void startBreak(BlockPos pos) {
        this.breakProgress = 0f;
        this.isDone = false;
        this.level = (ServerLevel) this.zombie.level();
        this.startTime = this.level.getGameTime();
        this.state = this.level.getBlockState(pos);
        this.cachedEfficiencyLevel = EnchantmentHelper.getBlockEfficiency(this.zombie);
        if (!this.zombie.swinging) {
            this.zombie.swing(InteractionHand.MAIN_HAND);
        }
        this.pos = pos;
        this.isDone = false;
        this.miningSpeed = (float) ZGRZombieAttributesAPI.getMiningSpeed(this.data);
    }

    private boolean blockVerification(BlockPos pos, BlockState state) {
        return this.zombie.isAlive()
                && canBreakBlock(state,pos);
    }

    /**
     * 直接破坏瞬间破坏方块（硬度 = 0，如火把），不经过挖掘进度系统
     */
    private void instantBreak(BlockPos pos, BlockState state) {
        Block.dropResources(state, this.level, pos, null, this.zombie, this.zombie.getMainHandItem());
        this.level.destroyBlock(pos, false, this.zombie);
        if (!this.zombie.swinging) {
            this.zombie.swing(InteractionHand.MAIN_HAND);
        }
        this.isDone = true;
        this.pos = pos;
    }

    private boolean isDone = true;


    public long getLastFailTime() {
        return lastFailTime;
    }

    public void failBreak() {
        this.halfwayFailureCallback.accept(zombie,this.data);
        this.lastFailTime = this.level.getGameTime();
        this.stopBreak();
    }

    public void stopBreak() {
        if (!this.isDone){
            this.level.destroyBlockProgress(this.zombie.getId(), pos, -1);
            this.isDone = true;
        }
    }

    private boolean positionVerification(BlockPos pos) {
        return !this.level.isOutsideBuildHeight(pos)
                && this.zombie.getEyePosition().distanceToSqr(MathUtils.blockPosToVec3(pos)) <= ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR;
    }

    public boolean isDone() {
        return isDone;
    }
}