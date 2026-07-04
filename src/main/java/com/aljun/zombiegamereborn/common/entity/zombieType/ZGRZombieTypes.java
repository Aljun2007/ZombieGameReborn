package com.aljun.zombiegamereborn.common.entity.zombieType;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.common.entity.zombieType.type.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = ZombieGameReborn.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
/**
 * 预定义的僵尸类型常量
 */
public class ZGRZombieTypes {


    public static class IDs{

        public static final ResourceLocation DUMMY_ID = id("dummy");

        public static final ResourceLocation VANILLA_ID = id("vanilla");
        public static final ResourceLocation ENHANCED_VANILLA_ID = id("enhanced_vanilla");
        public static final ResourceLocation BUILDER_ID = id("builder");
        public static final ResourceLocation MINER_ID = id("miner");
        public static final ResourceLocation MUSKET_MOD_GUNNER_ID = id("musket_mod_gunner");
        public static final ResourceLocation CROSSBOW_ATTACKER_ID = id("crossbow_attacker");
        public static final ResourceLocation BOW_ATTACKER_ID = id("bow_attacker");
        public static final ResourceLocation SHIELD_USER_ID = id("shield_user");

        public static final ResourceLocation ZOMBIE_GUARD_VILLAGER = id("zombie_guard_villager");

        private static ResourceLocation id(String id) {
            return ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, id);
        }

    }

    public static ZombieType DUMMY = new DummyType();

    public static ZombieType VANILLA =new ZombieType(IDs.VANILLA_ID);
    public static ZombieType ENHANCED_VANILLA =new EnhancedVanillaType();
    public static ZombieType BUILDER = new BuilderZombieType();
    public static ZombieType MINER = new MinerZombieType();
    public static ZombieType MUSKET_MOD_GUNNER = new MusketModGunnerType();
    public static ZombieType CROSSBOW_ATTACKER = new CrossbowAttackerType();
    public static ZombieType BOW_ATTACKER = new BowAttackerType();
    public static ZombieType SHIELD_USER = new ShieldUserType();

    public static ZombieType ZOMBIE_GUARD_VILLAGER = new ZombieGuardVillagerType();

    public static void register(RegisterEvent.RegisterHelper<ZombieType> helper) {

        helper.register(ZGRZombieTypes.IDs.DUMMY_ID, DUMMY);

        helper.register(ZGRZombieTypes.IDs.VANILLA_ID, VANILLA);
        helper.register(ZGRZombieTypes.IDs.ENHANCED_VANILLA_ID, ENHANCED_VANILLA);
        helper.register(ZGRZombieTypes.IDs.BUILDER_ID, BUILDER);
        helper.register(IDs.MINER_ID, MINER);
        helper.register(IDs.MUSKET_MOD_GUNNER_ID,MUSKET_MOD_GUNNER);
        helper.register(IDs.CROSSBOW_ATTACKER_ID,CROSSBOW_ATTACKER);
        helper.register(IDs.BOW_ATTACKER_ID,BOW_ATTACKER);
        helper.register(IDs.SHIELD_USER_ID,SHIELD_USER);

        helper.register(IDs.ZOMBIE_GUARD_VILLAGER,ZOMBIE_GUARD_VILLAGER);
    }
}
