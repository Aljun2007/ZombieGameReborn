package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class OpenClientConfigScreenPacket implements CustomPacketPayload {

    public static final Type<OpenClientConfigScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "open_client_config_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenClientConfigScreenPacket> STREAM_CODEC =
            StreamCodec.ofMember(OpenClientConfigScreenPacket::encode, OpenClientConfigScreenPacket::decode);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();

    private final JsonObject settings;

    public OpenClientConfigScreenPacket() {
        this.settings = new JsonObject();
    }

    public OpenClientConfigScreenPacket(JsonObject settings) {
        this.settings = settings;
    }

    public OpenClientConfigScreenPacket(FriendlyByteBuf buffer) {
        this.settings = decode(buffer).settings;
    }

    public void encode(FriendlyByteBuf buffer) {
        String jsonString = GSON.toJson(settings);
        buffer.writeUtf(jsonString);
    }

    public static OpenClientConfigScreenPacket decode(FriendlyByteBuf buffer) {
        String jsonString = buffer.readUtf();
        JsonObject jsonObject = GSON.fromJson(jsonString, JsonObject.class);
        return new OpenClientConfigScreenPacket(jsonObject != null ? jsonObject : new JsonObject());
    }

    public JsonObject getSettings() {
        return settings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
