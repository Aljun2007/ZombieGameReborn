package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.ZGRConfigFileManager;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class GamePropertyUploadPacket implements CustomPacketPayload {

    public static final Type<GamePropertyUploadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "game_property_upload"));

    public static final StreamCodec<FriendlyByteBuf, GamePropertyUploadPacket> STREAM_CODEC =
            StreamCodec.ofMember(GamePropertyUploadPacket::encode, GamePropertyUploadPacket::decode);

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

    public JsonObject getSettings() {
        return settings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer sp ? sp : null;

            if (player != null && player.getServer() != null) {
                // 检查权限：需要4级管理员（服务器操作员）
                if (!player.hasPermissions(2)) {
                    player.displayClientMessage(
                            Component.translatable("message.zombiegamereborn.gameproperty.no_permission"), false
                    );
                    return;
                }

                MinecraftServer server = player.getServer();
                GameProperty gameProperty = GameProperty.fromJsonObject(settings);
                ZGRConfigFileManager.saveConfig(server, gameProperty);
                ZGRGame.newGameProperty(gameProperty);
                player.displayClientMessage(
                    Component.literal("§a配置已保存到服务器"), false
                );
            }
        });
    }
}
