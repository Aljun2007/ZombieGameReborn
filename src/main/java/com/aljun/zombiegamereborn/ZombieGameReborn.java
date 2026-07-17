package com.aljun.zombiegamereborn;

import com.aljun.zombiegamereborn.common.client.config.ClientConfigManager;
import com.aljun.zombiegamereborn.common.config.ZGRConfigFileManager;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


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
        event.enqueueWork(() -> {
            ZGRDiplomacyCenter.init();

            // 在 FML 启动时创建配置目录并生成默认配置文件
            createConfigDirectories();
            LOGGER.info("正在生成默认配置文件...");
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientConfigManager.load();
            }
            ZGRConfigFileManager.getGlobalDefault();
            LOGGER.info("默认配置文件生成完成");

            LOGGER.info("Zombie Game Reborn initialized!");
        });
    }

    /**
     * 创建模组所需的 config 子目录（配置目录和自定义预设目录）
     */
    private void createConfigDirectories() {
        try {
            // config/zombiegamereborn/
            Path configDir = Path.of("config", MOD_ID);
            Files.createDirectories(configDir);

            // config/zombiegamereborn/game_properties/ （自定义预设存放目录）
            Path presetDir = configDir.resolve("game_properties");
            Files.createDirectories(presetDir);

            LOGGER.info("已创建配置目录: {}", configDir.toAbsolutePath().normalize());
            LOGGER.info("已创建自定义预设目录: {}", presetDir.toAbsolutePath().normalize());
        } catch (IOException e) {
            LOGGER.error("创建配置目录失败", e);
        }
    }
}
