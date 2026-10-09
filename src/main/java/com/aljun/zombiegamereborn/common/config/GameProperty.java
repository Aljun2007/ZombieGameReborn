package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.common.player.ReginalStageDetector;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class GameProperty {

    // TypeToken 用于泛型
    public static final Type TYPE = new TypeToken<GameProperty>() {
    }.getType();
    // 静态 Gson 实例，带自定义适配器
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(StageProperty.class, new StageProperty.StagePropertyAdapter())
            .create();
    @SerializedName("stage_properties")
    public ArrayList<StageProperty> stageProperties = new ArrayList<>();
    @SerializedName("can_zombie_break_block")
    public boolean canZombieBreakBlock = true;
    @SerializedName("can_zombie_place_block")
    public boolean canZombiePlaceBlock = true;
    @SerializedName("can_piglin_infection")
    public boolean canPiglinInfection = false;
    @SerializedName("mob_replacement")
    public MobReplacement mobReplacement = MobReplacement.getDefault();
    @SerializedName("keep_mob_loot_table")
    public boolean keepMobLootTable = true;
    @SerializedName("max_empowered_builder_count")
    public int maxEmpoweredBuilderCount = 30;
    @SerializedName("max_empowered_miner_count")
    public int maxEmpoweredMinerCount = 30;
    @SerializedName("disable_turtle_egg_seeking")
    public boolean disableTurtleEggSeeking = false;
    @SerializedName("max_zombie_count")
    public int maxZombieCount = 200;
    @SerializedName("infected_villager_can_break_blocks")
    public boolean infectedVillagerCanBreakBlocks = false;
    @SerializedName("simplified_builder_movement")
    public boolean simplifiedBuilderMovenment = true;

    @SerializedName("rough_pathfinding_threshold")
    public int roughPathfindingThreshold = 10;

    @SerializedName("rough_pathfinding_interval")
    public int roughPathfindingInterval = 400;

    private volatile ArrayList<StageProperty> sortedCache = null;
    private volatile int configHash = 0;
    private final Map<Double, StageProperty> dayCache = new ConcurrentHashMap<>();

    private GameProperty() {
        stageProperties.add(new StageProperty());
    }

    public static GameProperty empty() {
        return new GameProperty();
    }

    public static GameProperty getGlobalDefault() {
        return ZGRConfigFileManager.getGlobalDefault();
    }

    public static GameProperty getEmpty() {
        return new GameProperty();
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

        // 解析 can_piglin_infection
        if (obj.has("can_piglin_infection")) {
            property.canPiglinInfection = obj.get("can_piglin_infection").getAsBoolean();
        }

        // 解析 mob_replacement
        if (obj.has("mob_replacement") && obj.get("mob_replacement").isJsonObject()) {
            property.mobReplacement = MobReplacement.fromJsonObject(obj.getAsJsonObject("mob_replacement"));
        }

        // 解析 keep_mob_loot_table
        if (obj.has("keep_mob_loot_table")) {
            property.keepMobLootTable = obj.get("keep_mob_loot_table").getAsBoolean();
        }

        // 解析性能参数
        if (obj.has("max_empowered_builder_count")) {
            property.maxEmpoweredBuilderCount = obj.get("max_empowered_builder_count").getAsInt();
        }
        if (obj.has("max_empowered_miner_count")) {
            property.maxEmpoweredMinerCount = obj.get("max_empowered_miner_count").getAsInt();
        }

        // 解析 disable_turtle_egg_seeking
        if (obj.has("disable_turtle_egg_seeking")) {
            property.disableTurtleEggSeeking = obj.get("disable_turtle_egg_seeking").getAsBoolean();
        }

        // 解析 max_zombie_count
        if (obj.has("max_zombie_count")) {
            property.maxZombieCount = obj.get("max_zombie_count").getAsInt();
        }

        // 解析 infected_villager_can_break_blocks
        if (obj.has("infected_villager_can_break_blocks")) {
            property.infectedVillagerCanBreakBlocks = obj.get("infected_villager_can_break_blocks").getAsBoolean();
        }

        // 解析 simplified_builder_movement
        if (obj.has("simplified_builder_movement")) {
            property.simplifiedBuilderMovenment = obj.get("simplified_builder_movement").getAsBoolean();
        }

        if (obj.has("rough_pathfinding_threshold")) {
            property.roughPathfindingThreshold = obj.get("rough_pathfinding_threshold").getAsInt();
        }
        if (obj.has("rough_pathfinding_interval")) {
            property.roughPathfindingInterval = obj.get("rough_pathfinding_interval").getAsInt();
        }

        return property;
    }

    /**
     * 基于区域内玩家平均存活天数获取该位置匹配的阶段
     */
    public StageProperty getStageProperty(ServerLevel level, BlockPos pos) {
        double day = ReginalStageDetector.get(level, pos);
        return getStageProperty(day);
    }

    /**
     * 根据天数查找匹配的阶段（带 dayCache）
     */
    public StageProperty getStageProperty(double day) {
        StageProperty cached = dayCache.get(day);
        if (cached != null) {
            return cached;
        }

        ensureSortedCache();

        StageProperty result = null;
        for (int i = sortedCache.size() - 1; i >= 0; i--) {
            StageProperty prop = sortedCache.get(i);
            if (prop.day <= day) {
                result = prop;
                break;
            }
        }

        if (result == null) {
            result = new StageProperty();
        }

        dayCache.put(day, result);
        return result;
    }

    /**
     * 获取全体玩家平均天数匹配的全局阶段
     */
    public StageProperty getGlobalStage(MinecraftServer server) {
        double day = ReginalStageDetector.getGlobalAverage(server);
        return getStageProperty(day);
    }

    /**
     * 每 20 tick 由外部 Handler 调用，清理 dayCache
     */
    public void clearDayCache() {
        dayCache.clear();
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

    public void init() {
        this.stageProperties.forEach(StageProperty::init);
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
        obj.addProperty("can_piglin_infection", canPiglinInfection);
        obj.add("mob_replacement", mobReplacement.toJsonObject());
        obj.addProperty("keep_mob_loot_table", keepMobLootTable);
        obj.addProperty("max_empowered_builder_count", maxEmpoweredBuilderCount);
        obj.addProperty("max_empowered_miner_count", maxEmpoweredMinerCount);
        obj.addProperty("disable_turtle_egg_seeking", disableTurtleEggSeeking);
        obj.addProperty("max_zombie_count", maxZombieCount);
        obj.addProperty("infected_villager_can_break_blocks", infectedVillagerCanBreakBlocks);
        obj.addProperty("simplified_builder_movement", simplifiedBuilderMovenment);
        obj.addProperty("rough_pathfinding_threshold", roughPathfindingThreshold);
        obj.addProperty("rough_pathfinding_interval", roughPathfindingInterval);

        return obj;
    }

    public GameProperty copy() {
        return GameProperty.fromJsonObject(this.toJsonObject());
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

    public void addStageProperty(StageProperty property) {
        this.stageProperties.add(property);
        // 清除缓存以强制重新排序和哈希检查
        this.sortedCache = null;
        this.configHash = 0;
        this.dayCache.clear();
    }

    public void removeAllStageProperty(Predicate<StageProperty> stagePropertyPredicate) {
        this.stageProperties.removeIf(stagePropertyPredicate);
        // 清除缓存以强制重新排序和哈希检查
        this.sortedCache = null;
        this.configHash = 0;
        this.dayCache.clear();
    }
}