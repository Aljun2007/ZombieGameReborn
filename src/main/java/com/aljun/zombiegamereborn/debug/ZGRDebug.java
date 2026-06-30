package com.aljun.zombiegamereborn.debug;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.DebugGuiPacket;
import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;


public class ZGRDebug {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();
    
    public static boolean isDebugMode() {
        return true;
    }

    public static void testItem() {

    }

    public static void testGUI(ServerPlayer player,String guiID) {
        ZGRNetwork.sendToClient(new DebugGuiPacket(DebugGuiPacket.GuiType.fromCode(guiID)),player);
    }

    public static void testConfigGui(ServerPlayer player) {
        GameProperty currentProperty = ZGRGame.getGameProperty();
        JsonObject configData = currentProperty.toJsonObject();
        ZGRNetwork.sendToClient(new GamePropertyDownloadPacket(configData), player);
    }
}
