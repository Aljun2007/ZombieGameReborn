package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieTypeManager;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DrownedConversionHandler {

    @SubscribeEvent
    public static void onZombieConversion(LivingConversionEvent.Post event) {
        if (event.getEntity() instanceof Zombie pre &&event.getOutcome() instanceof Drowned post) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(pre);
            if (data!= null) {
                if (data.getType().equals(ZGRZombieTypes.BUILDER)) {
                    ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(
                            post, ZGRZombieTypes.DROWNED_BUILDER.getId());
                } else if (data.getType().equals(ZGRZombieTypes.MINER)) {
                    ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(
                            post, ZGRZombieTypes.DROWNED_MINER.getId());
                } else if (data.getType().equals(ZGRZombieTypes.ENHANCED_VANILLA)){
                    ZombieTypeManager.initializeZombieWithNoWeaponAndArmor(
                            post, ZGRZombieTypes.ENHANCED_DROWNED.getId());
                }
            }
        }
    }
}
