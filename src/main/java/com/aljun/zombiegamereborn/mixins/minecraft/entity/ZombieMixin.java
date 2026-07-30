package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.common.entity.accessor.IZombieAccessor;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin implements IZombieAccessor {

    @Override
    public boolean get_isSunSensitive() {
        return isSunSensitive();
    }

    @Shadow
    protected abstract boolean isSunSensitive();

    @Shadow
    private int conversionTime;

    @Shadow
    private int inWaterTime;

    @Inject(method = "isSunSensitive", at = @At("RETURN"), cancellable = true)
    private void isSunSensitiveMixin(CallbackInfoReturnable<Boolean> cir) {
        Zombie zombie = (Zombie) (Object) this;
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data != null) {
            cir.setReturnValue(cir.getReturnValue() && data.isSunSensitive());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickMixin(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie.level().isClientSide) return;

        // 离开水时强制取消水下转化，防止爬岸后仍在倒计时
        if (this.conversionTime > 0 && !zombie.isInWater()) {
            this.conversionTime = -1;
            this.inWaterTime = -1;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data != null && data.fleeSun()) {
            if (zombie.isOnFire() && !zombie.isInWater()) {
                zombie.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            }
        }
        ZombieTypeManager.tickZombie(zombie);
    }

    @ModifyArg(
            method = "finalizeSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Zombie;setCanPickUpLoot(Z)V",
                    ordinal = 0
            ),
            index = 0
    )
    @SuppressWarnings("all")
    private boolean modifyCanPickUpLoot(boolean original) {
        try {
            Zombie zombie = (Zombie) (Object) this;
            MinecraftServer server = zombie.getServer();
            if (server != null) {

                ZombieProperty zombieProperty = ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).zombieProperty;

                if (zombieProperty != null) {

                    double coefficient = zombieProperty.canPickUpLootCoefficient;
                    RandomSource random = zombie.getRandom();
                    // 完全绕过 chunk 加载：只使用世界基础难度值
                    float difficulty = switch (zombie.level().getDifficulty()) {
                        case PEACEFUL -> 0.0f;
                        case EASY -> 0.5f;
                        case NORMAL -> 1.0f;
                        case HARD -> 1.5f;
                    };

                    return random.nextFloat() < (float) coefficient * difficulty;
                }
            }

        } catch (Exception ignored) {

        }
        return original;
    }

    @Inject(method = "convertsInWater", at = @At("RETURN"), cancellable = true)
    private void convertsInWaterMixin(CallbackInfoReturnable<Boolean> cir) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie.level().isClientSide) return;
        MinecraftServer server = zombie.getServer();
        if (server != null && ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(),zombie.blockPosition()).zombieProperty.doSwimmingZombieConvert) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            if (data != null) {
                cir.setReturnValue(cir.getReturnValue() && !ZGRZombieAttributesAPI.canSwim(data));
            }
        }
    }

    @ModifyArg(
            method = "playStepSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/Zombie;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            ),
            index = 1  // 音量参数
    )
    private float modifyStepVolume(float originalVolume) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData((Zombie) (Object) this);
        if (data != null) {
            return (float) (originalVolume * data.getStepVolumeModify());
        }
        return originalVolume;
    }


    @Inject(method = "aiStep", at = @At("RETURN"))
    private void aiStepMixin(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie.level().isClientSide) return;
        MinecraftServer server = zombie.getServer();
        if (server != null && (zombie.level().getGameTime() + zombie.getBlockY()) % 20 == 0) {
            if (ZGRGame.getGameProperty().getGlobalStage(zombie.getServer()).holyCleansing) {
                zombie.setSecondsOnFire(8);
            }
        }
    }

}

