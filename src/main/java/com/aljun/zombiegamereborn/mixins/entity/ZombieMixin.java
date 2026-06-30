package com.aljun.zombiegamereborn.mixins.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin{
    @Inject(method = "isSunSensitive", at = @At("RETURN"), cancellable = true)
    private void isSunSensitiveMixin(CallbackInfoReturnable<Boolean> cir) {
        Zombie zombie = (Zombie) (Object) this;
        cir.setReturnValue(cir.getReturnValue() && ZGRZombieAttributesAPI.isSunSensitive(ZGRZombieAttributesAPI.getZombieData(zombie)));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickMixin(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
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

                ZombieProperty zombieProperty = ZGRGame.getGameProperty().getStageProperty(server).zombieProperty;

                if (zombieProperty != null) {

                    double coefficient = zombieProperty.canPickUpLootCoefficient;
                    RandomSource random = zombie.getRandom();
                    float difficulty = zombie.level().getCurrentDifficultyAt(zombie.blockPosition()).getSpecialMultiplier();

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
        if (ZGRGame.getGameProperty().getStageProperty(zombie.getServer()).zombieProperty.doSwimmingZombieConvert) {
            cir.setReturnValue(cir.getReturnValue() && !ZGRZombieAttributesAPI.canSwim(ZGRZombieAttributesAPI.getZombieData(zombie)));
        }
    }
}
