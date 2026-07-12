package com.aljun.zombiegamereborn.common.config;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Type;

import static com.aljun.zombiegamereborn.utils.JsonUtils.getBooleanOrDefault;
import static com.aljun.zombiegamereborn.utils.JsonUtils.getDoubleOrDefault;

/**
 * 阶段属性配置类
 * <p>
 * 包含僵尸属性和生成选择器的配置
 * 使用自定义适配器进行 JSON 序列化和反序列化
 */
public class StageProperty {

    /**
     * Gson 实例，注册了自定义适配器
     */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ZombieProperty.class, new ZombieProperty.ZombiePropertyAdapter())
            .registerTypeAdapter(ZombieSpawnChooser.class, new ZombieSpawnChooser.ZombieSpawnChooserAdapter())
            .registerTypeAdapter(ZombieSpawnChooser.WrappedZombieType.class, new ZombieSpawnChooser.WrappedZombieType.WrappedZombieTypeAdapter())
            .registerTypeAdapter(StageProperty.class, new StagePropertyAdapter())
            .create();
    @SerializedName("zombie_property")
    public ZombieProperty zombieProperty = new ZombieProperty();
    @SerializedName("zombie_spawn_chooser")
    public ZombieSpawnChooser zombieSpawnChooser = ZombieSpawnChooser.getDefault();
    @SerializedName("replace_chance")
    public double replaceChance = 0.0d;
    @SerializedName("remove_chance")
    public double removeChance = 0.0d;
    @SerializedName("blood_moon_chance")
    public double bloodMoonChance = 0.0d;
    @SerializedName("day")
    public double day = 1.0;
    @SerializedName("zombie_count_modify")
    public double zombieCountModify = 1.0d;
    //清除所有僵尸）
    @SerializedName("holy_cleansing")
    public boolean holyCleansing = false;

    /**
     * 从 JsonObject 反序列化为 StageProperty 对象
     *
     * @param jsonObject JSON 对象
     * @return 反序列化后的 StageProperty 对象
     */
    private static StageProperty fromJsonObject(JsonObject jsonObject) {
        StageProperty property = new StageProperty();
        property.day = jsonObject.get("day").getAsDouble();
        // 反序列化 zombie_property
        if (jsonObject.has("zombie_property")) {
            JsonElement element = jsonObject.get("zombie_property");
            if (element.isJsonObject()) {
                property.zombieProperty = GSON.fromJson(element, ZombieProperty.class);
            }
        }

        // 反序列化 zombie_spawn_chooser
        if (jsonObject.has("zombie_spawn_chooser")) {
            JsonElement element = jsonObject.get("zombie_spawn_chooser");
            if (element.isJsonObject()) {
                property.zombieSpawnChooser = GSON.fromJson(element, ZombieSpawnChooser.class);
            }
        }
        property.replaceChance = getDoubleOrDefault(jsonObject, "replace_chance", 0.0d);
        property.removeChance = getDoubleOrDefault(jsonObject, "remove_chance", 0.0d);
        property.bloodMoonChance = getDoubleOrDefault(jsonObject, "blood_moon_chance", 0.0d);
        property.zombieCountModify = getDoubleOrDefault(jsonObject, "zombie_count_modify", 1.0d);
        property.holyCleansing = getBooleanOrDefault(jsonObject, "holy_cleansing", false);

        return property;
    }

    public float calculateDifficulty(ServerLevel level, BlockPos pos) {
        // 只使用世界难度等级，完全不触碰 chunk
        float baseOffset = switch (level.getDifficulty()) {
            case PEACEFUL -> 0.0f;
            case EASY -> 0.15f;
            case HARD -> 0.45f;
            default -> 0.30f;
        };

        return baseOffset + (1.0f - baseOffset) * baseOffset;
    }

    public void init() {
        this.zombieSpawnChooser.init();
    }

    /**
     * 将 StageProperty 对象转换为 JsonObject
     *
     * @return JSON 对象
     */
    private JsonElement toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.add("day", new JsonPrimitive(this.day));
        // 序列化 zombie_property
        if (this.zombieProperty != null) {
            obj.add("zombie_property", GSON.toJsonTree(this.zombieProperty));
        }

        // 序列化 zombie_spawn_chooser
        if (this.zombieSpawnChooser != null) {
            obj.add("zombie_spawn_chooser", GSON.toJsonTree(this.zombieSpawnChooser));
        }

        obj.addProperty("replace_chance", this.replaceChance);
        obj.addProperty("remove_chance", this.removeChance);
        obj.addProperty("blood_moon_chance", this.bloodMoonChance);
        obj.addProperty("zombie_count_modify", this.zombieCountModify);
        obj.addProperty("holy_cleansing", this.holyCleansing);

        return obj;
    }

    /**
     * StageProperty 适配器 - 实现 JSON 序列化和反序列化
     */
    public static class StagePropertyAdapter implements JsonSerializer<StageProperty>, JsonDeserializer<StageProperty> {

        @Override
        public JsonElement serialize(StageProperty src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return src.toJsonObject();
        }

        @Override
        public StageProperty deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return new StageProperty();
            }

            if (json.isJsonObject()) {
                return StageProperty.fromJsonObject(json.getAsJsonObject());
            }

            return new StageProperty();
        }
    }
}
