package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZombieSpawnChooser {

    public static final ArrayList<SpawnType> allKeys = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ZombieType.class, new ZombieType.ZombieTypeAdapter())
            .registerTypeAdapter(ZombieSpawnChooser.WrappedZombieType.class, new ZombieSpawnChooser.WrappedZombieType.WrappedZombieTypeAdapter())
            .create();

    static {
        allKeys.add(SpawnType.NORMAL);
        allKeys.add(SpawnType.DROWNED);
    }

    public enum SpawnType {
        NORMAL("normal"),
        DROWNED("drowned");

        public final String name;

        SpawnType(String name) {
            this.name = name;
        }

        public static SpawnType byName(String name) {
            for (SpawnType type : values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return NORMAL; // 默认返回 NORMAL
        }
    }

    // 只序列化这个字段
    @SerializedName("zombie_types")
    public ArrayList<WrappedZombieType> zombieTypes = new ArrayList<>();
    // 不序列化 - 运行时动态生成
    private transient Map<SpawnType, RandomUtils.RandomPool<ZombieType>> allPools = new HashMap<>();

    public ZombieSpawnChooser() {
    }

    public ZombieSpawnChooser(ArrayList<WrappedZombieType> zombieTypes) {
        this.zombieTypes = zombieTypes;
    }

    /**
     * 初始化所有池子
     */
    public void init() {
        allPools.clear();
        allKeys.forEach(key -> {
            RandomUtils.RandomPool.Builder<ZombieType> builder =
                    RandomUtils.RandomPool.builder(ZombieType.class);
            this.zombieTypes.forEach(wrapped -> {
                if (wrapped.type == key) {
                    builder.add(wrapped.zombieType, wrapped.chance);
                }
            });
            allPools.put(key, builder.build());
        });
    }

    /**
     * 随机获取僵尸类型
     */
    public ZombieType randomType(SpawnType spawnType) {
        if (this.allPools.containsKey(spawnType)) {
            ZombieType type = allPools.get(spawnType).nextValue();
            if (type != null) {
                return type;
            }
        }
        return ifNull();
    }

    public static ZombieType ifNull() {
        return ZGRZombieTypes.DUMMY;
    }

    /**
     * WrappedZombieType 内部类
     */
    public static class WrappedZombieType {

        @Override
        public String toString() {
            return this.type.name + " 几率: " + this.chance+  " 类型 : " + this.zombieType.toString();
        }

        @SerializedName("spawn_type")
        public SpawnType type = SpawnType.NORMAL;

        @SerializedName("chance")
        public double chance = 1.0d;

        @SerializedName("zombie_type")
        public ZombieType zombieType = ZGRZombieTypes.DUMMY;

        public WrappedZombieType() {
        }

        public WrappedZombieType(SpawnType type, double chance, ZombieType zombieType) {
            this.type = type;
            this.chance = chance;
            this.zombieType = zombieType;
        }

        public static class WrappedZombieTypeAdapter implements JsonSerializer<WrappedZombieType>, JsonDeserializer<WrappedZombieType> {

            @Override
            public JsonElement serialize(WrappedZombieType src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == null) {
                    return JsonNull.INSTANCE;
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("spawn_type", src.type.name);
                obj.addProperty("chance", src.chance);

                // 序列化 ZombieType（使用 registry name）
                if (src.zombieType != null && src.zombieType.getId() != null) {
                    obj.addProperty("zombie_type", src.zombieType.getId().toString());
                } else {
                    obj.addProperty("zombie_type", ZGRZombieTypes.DUMMY.getId().toString());
                }

                return obj;
            }

            @Override
            public WrappedZombieType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json == null || json.isJsonNull()) {
                    return new WrappedZombieType();
                }

                if (!json.isJsonObject()) {
                    return new WrappedZombieType();
                }

                JsonObject obj = json.getAsJsonObject();
                WrappedZombieType wrapped = new WrappedZombieType();

                // 解析 type
                if (obj.has("spawn_type")) {
                    wrapped.type = SpawnType.byName(obj.get("spawn_type").getAsString());
                }

                // 解析 chance
                if (obj.has("chance")) {
                    wrapped.chance = obj.get("chance").getAsDouble();
                }

                // 解析 zombie_type
                if (obj.has("zombie_type")) {
                    String typeId = obj.get("zombie_type").getAsString();
                    // 根据 ID 查找 ZombieType
                    wrapped.zombieType = ZombieType.getById(ResourceLocation.parse(typeId));
                    if (wrapped.zombieType == null) {
                        wrapped.zombieType = ZGRZombieTypes.DUMMY;
                    }
                }

                return wrapped;
            }
        }
    }

    public static class ZombieSpawnChooserAdapter implements JsonSerializer<ZombieSpawnChooser>, JsonDeserializer<ZombieSpawnChooser> {

        @Override
        public JsonElement serialize(ZombieSpawnChooser src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }

            JsonObject obj = new JsonObject();

            // 序列化 zombieTypes
            JsonArray array = new JsonArray();
            for (ZombieSpawnChooser.WrappedZombieType wrapped : src.zombieTypes) {
                if (wrapped != null) {
                    // 使用 context 序列化每个 WrappedZombieType
                    JsonElement wrappedElement = GSON.toJsonTree(wrapped);
                    array.add(wrappedElement);
                }
            }
            obj.add("zombie_types", array);

            return obj;
        }

        @Override
        public ZombieSpawnChooser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return new ZombieSpawnChooser();
            }

            if (!json.isJsonObject()) {
                return new ZombieSpawnChooser();
            }

            JsonObject obj = json.getAsJsonObject();
            ZombieSpawnChooser chooser = new ZombieSpawnChooser();

            // 反序列化 zombieTypes
            if (obj.has("zombie_types") && obj.get("zombie_types").isJsonArray()) {
                JsonArray array = obj.getAsJsonArray("zombie_types");
                chooser.zombieTypes = new ArrayList<>();
                for (JsonElement element : array) {
                    // 使用 context 反序列化每个 WrappedZombieType
                    ZombieSpawnChooser.WrappedZombieType wrapped =
                            GSON.fromJson(element, ZombieSpawnChooser.WrappedZombieType.class);
                    if (wrapped != null) {
                        chooser.zombieTypes.add(wrapped);
                    }
                }
            }

            return chooser;
        }
    }
}