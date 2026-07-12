package com.aljun.zombiegamereborn.common.client.config;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientConfigManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "zombiegamereborn-client.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ClientConfig.class, new ClientConfig.ClientConfigAdapter())
            .create();

    private static ClientConfig cachedConfig = null;

    private static Path getConfigPath() {
        return Paths.get("config").resolve(FILE_NAME);
    }

    public static ClientConfig load() {
        if (cachedConfig != null) return cachedConfig;

        Path path = getConfigPath();
        if (!Files.exists(path)) {
            cachedConfig = ClientConfig.defaultConfig();
            save(cachedConfig);
            return cachedConfig;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            cachedConfig = ClientConfig.fromJsonObject(json != null ? json : new JsonObject());
        } catch (Exception e) {
            LOGGER.error("读取客户端配置失败: {}", path, e);
            cachedConfig = ClientConfig.defaultConfig();
        }
        return cachedConfig;
    }

    public static void save(ClientConfig config) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config.toJsonObject(), writer);
            }
            cachedConfig = config;
        } catch (Exception e) {
            LOGGER.error("保存客户端配置失败: {}", path, e);
        }
    }

    public static ClientConfig get() {
        return cachedConfig != null ? cachedConfig : load();
    }
}
