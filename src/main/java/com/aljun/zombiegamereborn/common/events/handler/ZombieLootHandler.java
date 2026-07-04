package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ZombieLootHandler {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide) return;
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        int looting = event.getLootingLevel();

        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            if (data.getType() == ZGRZombieTypes.MUSKET_MOD_GUNNER) {
                int bulletCount = RandomUtils.nextInt(0, 2);
                if (bulletCount > 0) {
                    bulletCount += RandomUtils.nextInt(0, looting + 1);
                    event.getDrops().add(new ItemEntity(
                            zombie.level(),
                            zombie.getX(), zombie.getY(), zombie.getZ(),
                            ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.getBulletStack(bulletCount)));
                }
            }
        }
        if (data.getType() == ZGRZombieTypes.BOW_ATTACKER || data.getType() == ZGRZombieTypes.CROSSBOW_ATTACKER) {
            int arrowCount = RandomUtils.nextInt(0, 2);
            if (arrowCount > 0) {
                arrowCount += RandomUtils.nextInt(0, looting + 1);
                event.getDrops().add(new ItemEntity(
                        zombie.level(),
                        zombie.getX(), zombie.getY(), zombie.getZ(),
                        new ItemStack(Items.ARROW, arrowCount)));
            }
        }
    }

}
