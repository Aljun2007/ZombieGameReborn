package com.aljun.zombiegamereborn.mixins.minecraft.entity;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void fireImmuneMixin(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide) return;
        if (entity instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);

            MinecraftServer server = zombie.getServer();
            if (server != null && ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).holyCleansing) {
                cir.setReturnValue(false);
                return;
            }

            cir.setReturnValue(data.fireImmune() || cir.getReturnValue());
        }
    }
}
