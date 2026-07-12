package com.aljun.zombiegamereborn.common.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobReplacement {

    public static final Type TYPE = new TypeToken<MobReplacement>() {}.getType();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(MobElement.class, new MobElementAdapter())
            .create();

    private final Map<ResourceLocation, ReplaceableType> replacementMap = new HashMap<>();

    @SerializedName("mobs")
    public List<MobElement> list = new ArrayList<>();

    public enum ReplaceableType {
        @SerializedName("remove")
        REMOVE,
        @SerializedName("replace")
        REPLACE;

        public static ReplaceableType byName(String name) {
            for (ReplaceableType t : values()) {
                if (t.name().equalsIgnoreCase(name)) return t;
            }
            return REPLACE;
        }
    }

    public record MobElement(
            @SerializedName("mob_id") ResourceLocation mobID,
            @SerializedName("action") ReplaceableType type) {

        public JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            obj.addProperty("mob_id", mobID.toString());
            obj.addProperty("action", type.name().toLowerCase());
            return obj;
        }

        public static MobElement fromJsonObject(JsonObject obj) {
            ResourceLocation id = ResourceLocation.parse(obj.get("mob_id").getAsString());
            ReplaceableType type = ReplaceableType.byName(obj.get("action").getAsString());
            return new MobElement(id, type);
        }
    }

    // ===================== 工具方法 =====================

    public void add(ResourceLocation mobID, ReplaceableType type) {
        replacementMap.put(mobID, type);
        syncListToMap();
    }

    @Nullable
    public ReplaceableType get(ResourceLocation mobID) {
        return replacementMap.get(mobID);
    }

    public boolean contains(ResourceLocation mobID) {
        return replacementMap.containsKey(mobID);
    }

    public void remove(ResourceLocation mobID) {
        replacementMap.remove(mobID);
        syncListToMap();
    }

    public void clear() {
        replacementMap.clear();
        list.clear();
    }

    private void syncListToMap() {
        list.clear();
        for (var entry : replacementMap.entrySet()) {
            list.add(new MobElement(entry.getKey(), entry.getValue()));
        }
    }

    private void syncMapToList() {
        replacementMap.clear();
        for (MobElement element : list) {
            if (element != null) {
                replacementMap.put(element.mobID(), element.type());
            }
        }
    }

    public static MobReplacement getDefault() {
        MobReplacement replacement = new MobReplacement();
        // 动物
        replacement.add(ResourceLocation.parse("minecraft:cow"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:sheep"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:pig"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:chicken"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:rabbit"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:horse"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:donkey"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:mule"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:llama"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:wolf"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:cat"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:ocelot"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:fox"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:bee"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:mooshroom"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:parrot"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:turtle"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:dolphin"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:squid"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:glow_squid"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:panda"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:polar_bear"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:goat"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:frog"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:axolotl"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:camel"), ReplaceableType.REMOVE);
        replacement.add(ResourceLocation.parse("minecraft:sniffer"), ReplaceableType.REMOVE);


        // ===== 替换为僵尸 =====
        replacement.add(ResourceLocation.parse("minecraft:hoglin"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:slime"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:ghast"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:skeleton"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:stray"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:wither_skeleton"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:creeper"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:spider"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:cave_spider"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:enderman"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:witch"), ReplaceableType.REPLACE);
        replacement.add(ResourceLocation.parse("minecraft:magma_cube"), ReplaceableType.REPLACE);

        return replacement;
    }

    // ===================== 序列化 =====================

    public static MobReplacement fromJsonObject(JsonObject obj) {
        MobReplacement replacement = new MobReplacement();
        if (obj.has("mobs") && obj.get("mobs").isJsonArray()) {
            JsonArray array = obj.getAsJsonArray("mobs");
            replacement.list = new ArrayList<>();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    replacement.list.add(MobElement.fromJsonObject(element.getAsJsonObject()));
                }
            }
        }
        replacement.syncMapToList();
        return replacement;
    }

    public JsonObject toJsonObject() {
        syncListToMap();
        JsonObject obj = new JsonObject();
        JsonArray array = new JsonArray();
        for (MobElement element : list) {
            if (element != null) {
                array.add(element.toJsonObject());
            }
        }
        obj.add("mobs", array);
        return obj;
    }

    // ===================== 适配器 =====================

    public static class MobElementAdapter implements JsonSerializer<MobElement>, JsonDeserializer<MobElement> {
        @Override
        public JsonElement serialize(MobElement src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            return src.toJsonObject();
        }

        @Override
        public MobElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull() || !json.isJsonObject()) {
                return null;
            }
            return MobElement.fromJsonObject(json.getAsJsonObject());
        }
    }
}
