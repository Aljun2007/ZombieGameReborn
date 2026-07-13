package com.aljun.zombiegamereborn.utils;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GamePropertyPresentUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();

    private static final String INITIAL_DEFAULT_PATH = "/data/zombiegamereborn/game_property/initial_default.json";

    public static GameProperty globalDefault() {
        return GameProperty.getGlobalDefault();
    }

    public static GameProperty initialDefault() {
        try (InputStream is = GamePropertyPresentUtils.class.getResourceAsStream(INITIAL_DEFAULT_PATH)) {
            if (is == null) return disabled();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return GSON.fromJson(json, GameProperty.TYPE);
        } catch (Exception e) {
            return disabled();
        }
    }

    public static GameProperty disabled() {
        return GameProperty.empty();
    }
}
