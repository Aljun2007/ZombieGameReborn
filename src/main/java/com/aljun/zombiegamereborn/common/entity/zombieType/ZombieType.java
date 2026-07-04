package com.aljun.zombiegamereborn.common.entity.zombieType;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.register.ZGRRegistries;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 僵尸类型基类
 * 每种僵尸类型可以重写方法来定义自己的行为
 */
public class ZombieType {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ZombieType> TYPE_CACHE = new ConcurrentHashMap<>();
    private final ResourceLocation id;

    @Override
    public String toString() {
        return this.getId().toString();
    }

    /**
     * 创建僵尸类型
     *
     * @param id 类型 ID（唯一标识符）
     */
    public ZombieType(ResourceLocation id) {
        this.id = id;
    }

    /**
     * 根据类型 ID 获取僵尸类型对象
     *
     * @param typeId 类型 ID
     * @return 僵尸类型对象，如果不存在则返回 null
     */
    @Nullable
    public static ZombieType getById(ResourceLocation typeId) {
        if (typeId == null) {
            return null;
        }
        return ZGRRegistries.ZOMBIE_TYPE.get().getValue(typeId);
    }

    /**
     * 获取类型 ID
     *
     * @return 类型 ID 字符串
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * 第一个 Tick 时调用
     * 用于重载 AI Goals 等一次性操作
     *
     * @param zombie 僵尸实体
     */
    public void onInitializeZombieGoals(Zombie zombie,IZombieData data) {
    }

    /**
     * 初始化僵尸类型（在僵尸生成时调用一次）
     * 可以修改属性、装备等
     *
     * @param zombie 僵尸实体
     */
    public void onInitializeZombieAttributes(Zombie zombie, IZombieData data) {
    }

    public void onInitializeZombieWeaponsAndArmors(Zombie zombie, IZombieData data) {
    }

    /**
     * 每个 Tick 时调用（服务端）
     *
     * @param zombie    僵尸实体
     * @param tickCount 当前 tick 计数
     */
    public void onTick(Zombie zombie,IZombieData dataLazyOptional, int tickCount) {
    }

    /**
     * 是否具备破坏方块能力
     * 重写此方法以启用自动破坏功能
     *
     * @return 默认返回 false
     */
    public boolean canBreakBlocks() {
        return false;
    }

    @SuppressWarnings("all")
    public void onZombieHurt(LivingHurtEvent event,Zombie zombie, IZombieData data) {

    }

    public boolean canPlaceBlock() {
        return false;
    }

    public static class ZombieTypeAdapter implements JsonSerializer<ZombieType>, JsonDeserializer<ZombieType> {

        @Override
        public JsonElement serialize(ZombieType src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            // 使用 ZombieType 的注册名或 ID
            return new JsonPrimitive(src.getId().toString());
        }

        @Override
        public ZombieType deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return ZGRZombieTypes.DUMMY;
            }
            String id = json.getAsString();
            // 根据 ID 查找对应的 ZombieType
            // 这里需要你的 ZombieType 注册系统
            return ZombieType.getById(ResourceLocation.parse(id));
        }
    }

    protected static void replaceGoal(GoalSelector goalSelector, Predicate<Goal> removePredicate, Supplier<Goal> newGoalSupplier,int i) {
        goalSelector.removeAllGoals(removePredicate);
        goalSelector.addGoal(i, newGoalSupplier.get());
    }


}
