package com.aljun.zombiegamereborn.debug;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class ZGRDebug {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();
    
    public static boolean isDebugMode() {
        return true;
    }

    public static void testItem() {

    }
}
