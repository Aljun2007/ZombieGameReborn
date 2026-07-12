package com.aljun.zombiegamereborn.utils;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GamePropertyPresentUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();
    public static GameProperty globalDefault() {
        return GameProperty.getGlobalDefault();
    }

    public static GameProperty generalDefault() {

        String json = "{}"; // 默认配置，硬编码，待填入
        return GSON.fromJson(json, GameProperty.TYPE);
    }

    public static GameProperty disabled() {
        return GameProperty.empty();
    }
}
