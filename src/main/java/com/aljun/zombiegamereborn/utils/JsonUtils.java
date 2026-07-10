package com.aljun.zombiegamereborn.utils;

import com.google.gson.JsonObject;

public class JsonUtils {
    public static double getDoubleOrDefault(JsonObject obj, String key, double defaultValue) {
        if (obj.has(key)) {
            return obj.get(key).getAsDouble();
        }
        return defaultValue;
    }

    public static int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        if (obj.has(key)) {
            return obj.get(key).getAsInt();
        }
        return defaultValue;
    }

    public static boolean getBooleanOrDefault(JsonObject obj, String key, boolean defaultValue) {
        if (obj.has(key)) {
            return obj.get(key).getAsBoolean();
        }
        return defaultValue;
    }
}
