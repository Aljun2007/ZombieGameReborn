package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.network.packet.AdvancementHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.aljun.zombiegamereborn.network.packet.AdvancementHandler.HORDE_THRESHOLD;

@Mod.EventBusSubscriber
public class CommonHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieType type = ZGRZombieAttributesAPI.getType(data);
            if (type != null) {
                type.onZombieHurt(event, zombie, data);
            }
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() instanceof Zombie zombie) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ResourceLocation typeId = data.getTypeID();
            if (typeId != null && typeId.equals(ZGRZombieTypes.IDs.ZOMBIE_GUARD_VILLAGER)) {
                AdvancementHandler.grantGuardBetrayal(player);
            }
            // 火枪僵尸检测
            if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded() && ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isHoldingGun(zombie)) {
                AdvancementHandler.grantMusketHit(player);
            }
            // 尸潮检测（当前维度）
            ResourceLocation dim = player.level().dimension().location();
            if (ZombieStatic.getLastTickZombieCount(dim) >= HORDE_THRESHOLD) {
                AdvancementHandler.grantZombieHorde(player);
            }

        }

    }
}
