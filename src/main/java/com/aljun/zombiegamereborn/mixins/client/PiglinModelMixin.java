package com.aljun.zombiegamereborn.mixins.client;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@OnlyIn(Dist.CLIENT)
@Mixin(PiglinModel.class)
public abstract class PiglinModelMixin {

    @Redirect(
            method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms(Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V"
            )
    )
    private void redirectAnimateZombieArms(
            ModelPart leftArm,
            ModelPart rightArm,
            boolean isAggressive,
            float attackTime,
            float ageInTicks,
            Mob entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks2,
            float netHeadYaw,
            float headPitch
    ) {
        Zombie zombie = (Zombie) entity;
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);

        if (data != null) {
            ZombieType type = ZGRZombieAttributesAPI.getType(data);
            if (type != null) {
                if ((type == ZGRZombieTypes.MUSKET_MOD_GUNNER || (data.canZombieContinueUseWeaponsInHand())) && ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isHoldingGun(zombie) && zombie.isAggressive()) {
                    PiglinModel<?> model = (PiglinModel<?>) (Object) this;
                    model.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
                    model.head.xRot = headPitch * ((float) Math.PI / 180F);
                    if (zombie.isUsingItem()) {
                        AnimationUtils.animateCrossbowCharge(model.rightArm, model.leftArm, zombie, true);
                    }
                    return;
                } else if ((type == ZGRZombieTypes.BOW_ATTACKER || (data.canZombieContinueUseWeaponsInHand())) && zombie.getMainHandItem().getItem() instanceof BowItem && zombie.isAggressive()) {
                    return;
                } else if ((type == ZGRZombieTypes.CROSSBOW_ATTACKER || (data.canZombieContinueUseWeaponsInHand())) && zombie.getMainHandItem().getItem() instanceof CrossbowItem && zombie.isAggressive()) {
                    return;
                } else if ((type == ZGRZombieTypes.SHIELD_USER || (data.canZombieContinueUseWeaponsInHand())) && zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem) {
                    return;
                }
            }
        }

        AnimationUtils.animateZombieArms(leftArm, rightArm, isAggressive, attackTime, ageInTicks);
    }
}
