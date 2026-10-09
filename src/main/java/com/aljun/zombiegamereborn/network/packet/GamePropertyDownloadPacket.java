package com.aljun.zombiegamereborn.network.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class GamePropertyDownloadPacket implements CustomPacketPayload {

    public static final Type<GamePropertyDownloadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "game_property_download"));

    public static final StreamCodec<FriendlyByteBuf, GamePropertyDownloadPacket> STREAM_CODEC =
            StreamCodec.ofMember(GamePropertyDownloadPacket::encode, GamePropertyDownloadPacket::decode);

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

    public JsonObject getSettings() {
        return settings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
