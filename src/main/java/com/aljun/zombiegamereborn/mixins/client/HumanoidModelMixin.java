package com.aljun.zombiegamereborn.mixins.client;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "prepareMobModel(Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("HEAD"))
    private void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, CallbackInfo ci) {
        if (entity instanceof Zombie zombie) {

            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            if(data != null) {
                ZombieType type = ZGRZombieAttributesAPI.getType(data);

                if (type != null) {

                    HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
                    ItemStack mainHand = zombie.getItemInHand(InteractionHand.MAIN_HAND);

                    if ((type == ZGRZombieTypes.SHIELD_USER || data.canZombieContinueUseWeaponsInHand()) && zombie.isUsingItem() && zombie.getUseItem().getItem() instanceof ShieldItem) {

                        if (zombie.getUsedItemHand() == InteractionHand.OFF_HAND) {
                            model.leftArmPose = HumanoidModel.ArmPose.BLOCK;
                        } else {
                            model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
                        }

                    } else if ((type == ZGRZombieTypes.BOW_ATTACKER || data.canZombieContinueUseWeaponsInHand()) && mainHand.is(Items.BOW) && zombie.isAggressive()) {

                        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
                        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

                        if (zombie.getMainArm() == HumanoidArm.RIGHT) {
                            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                        } else {
                            model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
                        }

                    } else if ((type == ZGRZombieTypes.CROSSBOW_ATTACKER || data.canZombieContinueUseWeaponsInHand()) && mainHand.is(Items.CROSSBOW) && zombie.isAggressive()) {

                        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
                        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

                        HumanoidModel.ArmPose armPose = zombie.isUsingItem() ?
                                HumanoidModel.ArmPose.CROSSBOW_CHARGE : HumanoidModel.ArmPose.CROSSBOW_HOLD;

                        model.rightArmPose = armPose;
                        model.leftArmPose = armPose;
                    }
                }
            }

        }
    }
}
