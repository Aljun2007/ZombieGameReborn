package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
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

        var killer = event.getSource().getEntity();
        if (!(killer instanceof Zombie)) return;

        // 适配原版难度感染概率: Easy=0%  Normal=50%  Hard=100%
        Difficulty difficulty = event.getEntity().level().getDifficulty();
        float infectionChance = switch (difficulty) {
            case PEACEFUL, EASY -> 0.0f;
            case NORMAL -> 0.5f;
            case HARD -> 1.0f;
        };
        if (event.getEntity().level().random.nextFloat() >= infectionChance) return;

        if (event.getEntity() instanceof AbstractPiglin piglin) {
            ZombifiedPiglin zombifiedpiglin = piglin.convertTo(EntityType.ZOMBIFIED_PIGLIN, true);
            if (zombifiedpiglin != null) {
                piglin.playSound(SoundEvents.PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED);
                zombifiedpiglin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                ForgeEventFactory.onLivingConvert(piglin, zombifiedpiglin);
                event.setCanceled(true);
            }
        } else if (event.getEntity() instanceof Hoglin hoglin) {
            Zoglin zoglin = hoglin.convertTo(EntityType.ZOGLIN, true);
            if (zoglin != null) {
                zoglin.playSound(SoundEvents.HOGLIN_CONVERTED_TO_ZOMBIFIED);
                zoglin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                ForgeEventFactory.onLivingConvert(hoglin, zoglin);
                event.setCanceled(true);
            }
        }
    }
}
