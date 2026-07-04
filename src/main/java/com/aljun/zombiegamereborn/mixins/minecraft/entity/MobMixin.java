package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Mob.class)
public class MobMixin {

    @Unique
    private static final String GUARD_CLASS_NAME = "tallestegg.guardvillagers.entities.Guard";

    @Inject(
            method = "convertTo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private <T extends Mob> void onConvertTo(EntityType<T> entityType, boolean keepInventory,
                                             CallbackInfoReturnable<T> cir, T target) {
        if (!(target instanceof Zombie zombie) || zombie.level().isClientSide) return;

        Mob self = (Mob) (Object) this;
        if (self.getClass().getName().equals(GUARD_CLASS_NAME)) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            if (!data.isTypeInitialized()) {
                ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(
                        zombie, ZGRZombieTypes.ZOMBIE_GUARD_VILLAGER.getId());
            }
        }
    }

    @ModifyArg(
            method = "playAmbientSound",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            ),
            index = 1
    )
    private float modifyAmbientVolume(float originalVolume) {
        Mob mob = (Mob) (Object) this;
        if (mob instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            return (float) (originalVolume * data.getAmbientVolumeModify());
        }
        return originalVolume;
    }

}
