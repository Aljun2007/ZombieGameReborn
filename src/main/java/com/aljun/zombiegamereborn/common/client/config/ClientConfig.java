package com.aljun.zombiegamereborn.common.client.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class ClientConfig {

    public static final Type TYPE = new TypeToken<ClientConfig>() {}.getType();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ClientConfig.class, new ClientConfigAdapter())
            .create();

    @SerializedName("time_broadcast_enabled")
    public boolean timeBroadcastEnabled = true;
    @SerializedName("time_alarm_enabled")
    public boolean timeAlarmEnabled = true;
    @SerializedName("login_message_enabled")
    public boolean loginMessageEnabled = true;

    public static ClientConfig defaultConfig() {
        return new ClientConfig();
    }

    public static ClientConfig fromJsonObject(JsonObject obj) {
        ClientConfig config = new ClientConfig();
        if (obj.has("time_broadcast_enabled")) {
            config.timeBroadcastEnabled = obj.get("time_broadcast_enabled").getAsBoolean();
        }
        if (obj.has("time_alarm_enabled")) {
            config.timeAlarmEnabled = obj.get("time_alarm_enabled").getAsBoolean();
        }
        if (obj.has("login_message_enabled")) {
            config.loginMessageEnabled = obj.get("login_message_enabled").getAsBoolean();
        }
        return config;
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("time_broadcast_enabled", timeBroadcastEnabled);
        obj.addProperty("time_alarm_enabled", timeAlarmEnabled);
        obj.addProperty("login_message_enabled", loginMessageEnabled);
        return obj;
    }

    public static class ClientConfigAdapter implements JsonSerializer<ClientConfig>, JsonDeserializer<ClientConfig> {
        @Override
        public JsonElement serialize(ClientConfig src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            return src.toJsonObject();
        }

        @Override
        public ClientConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonObject()) {
                return new ClientConfig();
            }
            return ClientConfig.fromJsonObject(json.getAsJsonObject());
        }
    }
}
