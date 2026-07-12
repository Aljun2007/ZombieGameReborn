package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ZombifiedPiglinHandler {

    @SubscribeEvent
    public static void onPiglinDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ZGRGame.getGameProperty().canPiglinInfection) return;
        if (event.getEntity() instanceof AbstractPiglin piglin)  {
            var killer = event.getSource().getEntity();
            if (!(killer instanceof Zombie)) return;
            ZombifiedPiglin zombifiedpiglin = piglin.convertTo(EntityType.ZOMBIFIED_PIGLIN, true);
            if (zombifiedpiglin != null) {
                piglin.playSound(SoundEvents.PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED);
                zombifiedpiglin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                ForgeEventFactory.onLivingConvert(piglin, zombifiedpiglin);
                event.setCanceled(true);

            }
        } else if (event.getEntity() instanceof Hoglin hoglin) {
            var killer = event.getSource().getEntity();
            if (!(killer instanceof Zombie)) return;
            Zoglin zoglin = hoglin.convertTo(EntityType.ZOGLIN, true);
            if (zoglin != null) {
                zoglin.playSound(SoundEvents.HOGLIN_CONVERTED_TO_ZOMBIFIED);
                zoglin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                ForgeEventFactory.onLivingConvert(zoglin, zoglin);
                event.setCanceled(true);
            }
        }
    }
}
