package com.aljun.zombiegamereborn.common.events.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
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

        // 替换的僵尸 → 使用原生物的 loot table 生成掉落
        ResourceLocation customLoot = null;
        if (data != null) {
            customLoot = data.getCustomLootTable();
        }
        if (customLoot != null) {

            var server = zombie.level().getServer();
            if (server == null) return;

            LootTable lootTable = server.getLootData().getLootTable(customLoot);
            LootParams params = new LootParams.Builder((ServerLevel) zombie.level())
                    .withParameter(LootContextParams.ORIGIN, zombie.position())
                    .withParameter(LootContextParams.THIS_ENTITY, zombie)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, event.getSource().getEntity())
                    .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, event.getSource().getDirectEntity())
                    .create(LootContextParamSets.ENTITY);
            lootTable.getRandomItems(params).forEach(stack -> {
                // 非射手僵尸不掉落箭矢
                if (stack.getItem() instanceof ArrowItem
                        && data.getType() != ZGRZombieTypes.BOW_ATTACKER
                        && data.getType() != ZGRZombieTypes.CROSSBOW_ATTACKER) {
                    return;
                }
                event.getDrops().add(new ItemEntity(zombie.level(), zombie.getX(), zombie.getY(), zombie.getZ(), stack));
            });

            return;
        }

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
        if (data != null && (data.getType() == ZGRZombieTypes.BOW_ATTACKER || data.getType() == ZGRZombieTypes.CROSSBOW_ATTACKER)) {
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
