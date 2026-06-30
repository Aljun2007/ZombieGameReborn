package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.client.gui.config.stage.GamePropertyScreen;
import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GamePropertyDownloadPacket {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private final JsonObject settings;

    public GamePropertyDownloadPacket() {
        this.settings = new JsonObject();
    }

    public GamePropertyDownloadPacket(JsonObject settings) {
        this.settings = settings;
    }

    public GamePropertyDownloadPacket(FriendlyByteBuf buffer) {
        this.settings = decode(buffer).settings;
    }

    public void encode(FriendlyByteBuf buffer) {
        String jsonString = GSON.toJson(settings);
        buffer.writeUtf(jsonString);
    }

    public static GamePropertyDownloadPacket decode(FriendlyByteBuf buffer) {
        String jsonString = buffer.readUtf();
        JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
        return new GamePropertyDownloadPacket(jsonObject != null ? jsonObject : new JsonObject());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        
        context.enqueueWork(() -> {
            if (context.getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) {
                Minecraft.getInstance().setScreen(new GamePropertyScreen("游戏配置", this.settings));
            }
        });
        
        context.setPacketHandled(true);
    }
}