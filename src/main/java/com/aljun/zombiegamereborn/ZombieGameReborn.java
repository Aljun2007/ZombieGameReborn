package com.aljun.zombiegamereborn;

import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(ZombieGameReborn.MOD_ID)
public class ZombieGameReborn {
    public static final String MOD_ID = "zombiegamereborn";

    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public ZombieGameReborn() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ZGRNetwork.register();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Zombie Game Reborn initialized!");
        ZGRDiplomacyCenter.init();
    }
}
