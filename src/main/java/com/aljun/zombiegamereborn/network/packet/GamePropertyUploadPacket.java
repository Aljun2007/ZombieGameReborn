package com.aljun.zombiegamereborn.network.packet;


import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.ZGRConfigFileManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GamePropertyUploadPacket {
    
    private static final Gson GSON = new GsonBuilder().create();
    
    private final JsonObject settings;

    public GamePropertyUploadPacket() {
        this.settings = new JsonObject();
    }

    public GamePropertyUploadPacket(JsonObject settings) {
        this.settings = settings;
    }

    public GamePropertyUploadPacket(FriendlyByteBuf buffer) {
        this.settings = decode(buffer).settings;
    }

    public void encode(FriendlyByteBuf buffer) {
        String jsonString = GSON.toJson(settings);
        buffer.writeUtf(jsonString);
    }

    public static GamePropertyUploadPacket decode(FriendlyByteBuf buffer) {
        String jsonString = buffer.readUtf();
        JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
        return new GamePropertyUploadPacket(jsonObject != null ? jsonObject : new JsonObject());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            
            if (player != null && player.getServer() != null) {
                MinecraftServer server = player.getServer();
                GameProperty gameProperty = GameProperty.fromJsonObject(settings);
                ZGRConfigFileManager.saveConfig(server, gameProperty);
                ZGRGame.newGameProperty(gameProperty);
                player.displayClientMessage(
                    Component.literal("§a配置已保存到服务器"), false
                );
            }
        });
        
        context.setPacketHandled(true);
    }
}