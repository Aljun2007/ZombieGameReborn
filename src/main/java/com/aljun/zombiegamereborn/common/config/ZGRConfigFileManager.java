package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件管理器
 * 负责读取、保存游戏配置文件到世界存档根目录
 */
public class ZGRConfigFileManager {
    
    private static final String CONFIG_FILE_NAME = "game_property.json";
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();
    
    /**
     * 从世界存档根目录读取配置文件
     * 
     * @param server Minecraft 服务器实例
     * @return 读取到的 GameProperty，如果文件不存在则返回默认配置
     */
    public static GameProperty loadConfig(MinecraftServer server) {
        Path configPath = getWorldConfigPath(server);
        
        if (!Files.exists(configPath)) {
            return GameProperty.globalDefault();
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
            return GameProperty.fromJsonObject(jsonObject != null ? jsonObject : new JsonObject());
        } catch (Exception e) {
            LOGGER.error("读取配置文件失败: {}", configPath, e);
            return GameProperty.empty();
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 保存配置文件到世界存档根目录
     * 
     * @param server Minecraft 服务器实例
     * @param gameProperty 要保存的游戏配置
     */
    public static void saveConfig(MinecraftServer server, GameProperty gameProperty) {
        Path configPath = getWorldConfigPath(server);
        
        try {
            // 确保父目录存在
            Files.createDirectories(configPath.getParent());
            
            // 序列化并写入文件
            JsonObject jsonObject = gameProperty.toJsonObject();
            String jsonString = GSON.toJson(jsonObject);
            
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write(jsonString);
            }

        } catch (Exception e) {
            LOGGER.error("保存配置文件失败: {}", configPath, e);
        }
    }
    
    /**
     * 获取世界存档中的配置文件路径
     * 
     * @param server Minecraft 服务器实例
     * @return 配置文件的完整路径
     */
    private static Path getWorldConfigPath(MinecraftServer server) {
        // 获取世界文件夹路径，配置文件保存在世界根目录
        return server.getWorldPath(CONFIG_PATH).resolve(CONFIG_FILE_NAME);
    }

    private static final LevelResource CONFIG_PATH = new LevelResource(ZombieGameReborn.MOD_ID);

    private static final String GLOBAL_DEFAULT_CONFIG_FILE = "game_property.json";
    
    private static GameProperty globalDefault = null;

    public static GameProperty getGlobalDefault() {
        if (globalDefault != null) {
            return globalDefault;
        }

        Path configPath = null;
        try {
            configPath = getClientConfigDirectory().resolve(GLOBAL_DEFAULT_CONFIG_FILE);

            if (!Files.exists(configPath)) {
                globalDefault = createAndSaveDefaultConfig(configPath);
                return globalDefault;
            }

            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                globalDefault = GameProperty.fromJsonObject(jsonObject != null ? jsonObject : new JsonObject());
                return globalDefault;
            }
        } catch (Exception e) {
            LOGGER.error("读取全局默认配置文件失败: {}", configPath, e);
            globalDefault = GameProperty.empty();
            return globalDefault;
        }
    }

    private static Path getClientConfigDirectory() {
        return Path.of("config/"+ ZombieGameReborn.MOD_ID);
    }

    private static GameProperty createAndSaveDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            
            GameProperty defaultProperty = createDefault();
            
            JsonObject jsonObject = defaultProperty.toJsonObject();
            String jsonString = GSON.toJson(jsonObject);
            
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write(jsonString);
            }

            return defaultProperty;
        } catch (Exception e) {
            LOGGER.error("创建全局默认配置文件失败", e);
            return GameProperty.empty();
        }
    }

    private static GameProperty createDefault() {
        return GameProperty.empty();
    }
}
