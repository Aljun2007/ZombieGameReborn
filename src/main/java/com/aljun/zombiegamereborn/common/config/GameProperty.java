package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.common.game.SurvivalDayManager;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;

public class GameProperty {

    @SerializedName("stage_properties")
    public ArrayList<StageProperty> stageProperties = new ArrayList<>();

    @SerializedName("can_zombie_break_block")
    public boolean canZombieBreakBlock = true;

    @SerializedName("can_zombie_place_block")
    public boolean canZombiePlaceBlock = true;

    private volatile ArrayList<StageProperty> sortedCache = null;
    private volatile int configHash = 0;
    
    private volatile StageProperty cachedResult = null;
    private volatile double cachedDayValue = -1.0;

    public static GameProperty empty() {
        return new GameProperty();
    }

    public static GameProperty globalDefault() {
        return ZGRConfigFileManager.getGlobalDefault();
    }

    /**
     * ⚠️ 此方法仅供无法直接使用 Server 的方法使用。
     * 其他逻辑请勿直接调用
     */

    public StageProperty getCurrentStageProperty() {
        return this.cachedResult;
    }

    public StageProperty getStageProperty(MinecraftServer server) {
        if (stageProperties.isEmpty()) {
            return new StageProperty();
        }

        double day = SurvivalDayManager.getDay(server);
        
        ensureSortedCache();
        
        if (cachedResult != null && day == cachedDayValue) {
            return cachedResult;
        }
        
        StageProperty result = findStageProperty(day);
        
        if (result != null) {
            cachedResult = result;
            cachedDayValue = day;
        }
        
        return result != null ? result : new StageProperty();
    }

    private StageProperty findStageProperty(double day) {
        for (int i = sortedCache.size() - 1; i >= 0; i--) {
            StageProperty prop = sortedCache.get(i);
            if (prop.day <= day) {
                return prop;
            }
        }
        return null;
    }

    private void ensureSortedCache() {
        int currentHash = stageProperties.hashCode();
        if (sortedCache == null || configHash != currentHash) {
            synchronized (this) {
                currentHash = stageProperties.hashCode();
                if (sortedCache == null || configHash != currentHash) {
                    ArrayList<StageProperty> sorted = new ArrayList<>(stageProperties);
                    sorted.sort(Comparator.comparingDouble(p -> p.day));
                    this.sortedCache = sorted;
                    this.configHash = currentHash;
                }
            }
        }
    }

    // TypeToken 用于泛型
    public static final Type TYPE = new TypeToken<GameProperty>() {}.getType();

    // 静态 Gson 实例，带自定义适配器
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(StageProperty.class, new StageProperty.StagePropertyAdapter())
            .create();

    private GameProperty() {
        stageProperties.add(new StageProperty());
    }


    public void init() {
        this.stageProperties.forEach(StageProperty::init);
    }

    /**
     * 从 JsonObject 反序列化（供适配器使用）
     */

    public static GameProperty fromJsonObject(JsonObject obj) {
        GameProperty property = new GameProperty();

        // 解析 zombie_properties
        if (obj.has("stage_properties") && obj.get("stage_properties").isJsonArray()) {
            JsonArray array = obj.getAsJsonArray("stage_properties");
            property.stageProperties = new ArrayList<>();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    StageProperty stageProperty = GSON.fromJson(element, StageProperty.class);
                    property.stageProperties.add(stageProperty);
                }
            }
        }

        // 解析 can_zombie_break_block
        if (obj.has("can_zombie_break_block")) {
            property.canZombieBreakBlock = obj.get("can_zombie_break_block").getAsBoolean();
        }

        // 解析 can_zombie_place_block
        if (obj.has("can_zombie_place_block")) {
            property.canZombiePlaceBlock = obj.get("can_zombie_place_block").getAsBoolean();
        }

        return property;
    }

    /**
     * 转换为 JsonObject（供适配器使用）
     */
    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();

        // 序列化
        JsonArray array = new JsonArray();
        for (StageProperty prop : stageProperties) {
            if (prop != null) {
                array.add(GSON.toJsonTree(prop));
            }
        }
        obj.add("stage_properties", array);

        // 序列化其他字段
        obj.addProperty("can_zombie_break_block", canZombieBreakBlock);
        obj.addProperty("can_zombie_place_block", canZombiePlaceBlock);

        return obj;
    }

    public static class GamePropertyAdapter implements JsonSerializer<GameProperty>, JsonDeserializer<GameProperty> {

        @Override
        public JsonElement serialize(GameProperty src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return src.toJsonObject();
        }

        @Override
        public GameProperty deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return new GameProperty();
            }

            if (json.isJsonObject()) {
                return GameProperty.fromJsonObject(json.getAsJsonObject());
            }

            return new GameProperty();
        }
    }
}