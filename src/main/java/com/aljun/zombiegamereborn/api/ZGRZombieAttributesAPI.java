package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.Optional;

@SuppressWarnings("all")
public class ZGRZombieAttributesAPI {

    // ===================== Core Data Access =====================

    /**
     * 获取僵尸的数据能力
     */
    public static IZombieData getZombieData(Zombie zombie) {
        return zombie.getCapability(ZombieDataProvider.ZOMBIE_DATA).orElse(null);
    }

    // ==================== Custom Capability Properties ====================

    /**
     * 检查僵尸是否对阳光敏感
     */
    public static boolean isSunSensitive(IZombieData data) {
        return data.isSunSensitive();
    }

    /**
     * 设置僵尸是否对阳光敏感
     */
    public static void setSunSensitive(IZombieData data, boolean value) {
        data.setSunSensitive(value);
    }

    /**
     * 获取僵尸的类型 ID（可能返回 null）
     */
    @Nullable
    public static ResourceLocation getTypeID(IZombieData data) {
        return data.getTypeID();
    }

    /**
     * 设置僵尸的类型 ID
     */
    public static void setTypeID(IZombieData data, ResourceLocation typeId) {
    data.setTypeID(typeId);
}

    /**
     * 获取僵尸的类型对象（可能返回 null）
     */
    @Nullable
    public static ZombieType getType(IZombieData data) {
        return data.getType();
    }

    /**
     * 获取僵尸的挖掘速度（可能返回 null）
     */
    public static double getMiningSpeed(IZombieData data) {
        return data.getMiningSpeed();
    }

    /**
     * 设置僵尸的挖掘速度
     */
    public static void setMiningSpeed(IZombieData data, double speed) {
        if (speed <= 0.0) {
            sendErrorToPlayers("挖掘速度必须大于0.0，当前值: " + speed);
            throw new IllegalArgumentException("Mining speed must be greater than 0.0, but got: " + speed);
        }
        data.setMiningSpeed(speed);
    }

    /**
     * 检查僵尸是否可以游泳
     */
    public static boolean canSwim(IZombieData data) {
        return data.canSwim();
    }

    /**
     * 设置僵尸是否可以游泳
     */
    public static void setCanSwim(IZombieData data, boolean canSwim) {
        data.enableSwim(canSwim);
    }

    // ==================== Vanilla Attribute Operations ====================

    /**
     * 获取僵尸的原版属性实例
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends Number> T getVanillaAttribute(Zombie zombie, Attribute attribute) {
        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null) {
            return (T) Double.valueOf(instance.getValue());
        }
        return null;
    }

    /**
     * 获取僵尸的原版属性实例（返回 Optional）
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> Optional<T> getVanillaAttributeOptional(Zombie zombie, Attribute attribute) {
        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null) {
            return Optional.of((T) Double.valueOf(instance.getValue()));
        }
        return Optional.empty();
    }

    /**
     * 设置僵尸的原版属性基础值
     */
    public static <T extends Number> void setVanillaAttribute(Zombie zombie, Attribute attribute, T value) {
        if (value == null) {
            sendErrorToPlayers("属性值不能为null");
            throw new IllegalArgumentException("Attribute value cannot be null");
        }

        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value.doubleValue());
        } else {
            sendErrorToPlayers("僵尸没有该属性: " + attribute.getDescriptionId());
            throw new IllegalArgumentException("Zombie does not have attribute: " + attribute.getDescriptionId());
        }
    }

    /**
     * 获取僵尸的最大生命值（可能返回 null）
     */
    @Nullable
    public static Double getMaxHealth(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.MAX_HEALTH);
    }

    /**
     * 获取僵尸的最大生命值（返回 Optional）
     */
    public static Optional<Double> getMaxHealthOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.MAX_HEALTH);
    }

    /**
     * 设置僵尸的最大生命值
     */
    public static void setMaxHealth(Zombie zombie, double health) {
        if (health <= 0.0) {
            sendErrorToPlayers("最大生命值必须大于0.0，当前值: " + health);
            throw new IllegalArgumentException("Max health must be greater than 0.0, but got: " + health);
        }
        setVanillaAttribute(zombie, Attributes.MAX_HEALTH, health);
    }

    /**
     * 获取僵尸的移动速度（可能返回 null）
     */
    @Nullable
    public static Double getMovementSpeed(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.MOVEMENT_SPEED);
    }

    /**
     * 获取僵尸的移动速度（返回 Optional）
     */
    public static Optional<Double> getMovementSpeedOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.MOVEMENT_SPEED);
    }

    /**
     * 设置僵尸的移动速度
     */
    public static void setMovementSpeed(Zombie zombie, double speed) {
        if (speed < 0.0) {
            sendErrorToPlayers("移动速度必须大于0.0，当前值: " + speed);
            throw new IllegalArgumentException("Movement speed must be greater than 0.0, but got: " + speed);
        }
        setVanillaAttribute(zombie, Attributes.MOVEMENT_SPEED, speed);
    }

    /**
     * 获取僵尸的攻击伤害（可能返回 null）
     */
    @Nullable
    public static Double getAttackDamage(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.ATTACK_DAMAGE);
    }

    /**
     * 获取僵尸的攻击伤害（返回 Optional）
     */
    public static Optional<Double> getAttackDamageOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.ATTACK_DAMAGE);
    }

    /**
     * 设置僵尸的攻击伤害
     */
    public static void setAttackDamage(Zombie zombie, double damage) {
        if (damage < 0.0) {
            sendErrorToPlayers("攻击伤害不能为负数，当前值: " + damage);
            throw new IllegalArgumentException("Attack damage must be non-negative, but got: " + damage);
        }
        setVanillaAttribute(zombie, Attributes.ATTACK_DAMAGE, damage);
    }

    public static void setArmorToughness(Zombie zombie, double armorToughness) {
        if (armorToughness < 0.0) {
            sendErrorToPlayers("盔甲韧性不能为负数，当前值: " + armorToughness);
            throw new IllegalArgumentException("Attack damage must be non-negative, but got: " + armorToughness);
        }
        setVanillaAttribute(zombie, Attributes.ARMOR_TOUGHNESS, armorToughness);
    }

    public static Optional<Double> getArmorToughnessOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.ARMOR_TOUGHNESS);
    }

    public static Double getArmorToughness(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.ARMOR_TOUGHNESS);
    }

    /**
     * 获取僵尸的护甲值（可能返回 null）
     */
    @Nullable
    public static Double getArmor(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.ARMOR);
    }

    /**
     * 获取僵尸的护甲值（返回 Optional）
     */
    public static Optional<Double> getArmorOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.ARMOR);
    }

    /**
     * 设置僵尸的护甲值
     */
    public static void setArmor(Zombie zombie, double armor) {
        if (armor < 0.0) {
            sendErrorToPlayers("护甲值不能为负数，当前值: " + armor);
            throw new IllegalArgumentException("Armor must be non-negative, but got: " + armor);
        }
        setVanillaAttribute(zombie, Attributes.ARMOR, armor);
    }

    /**
     * 获取僵尸的击退抗性（可能返回 null）
     */
    @Nullable
    public static Double getKnockbackResistance(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.KNOCKBACK_RESISTANCE);
    }

    /**
     * 获取僵尸的击退抗性（返回 Optional）
     */
    public static Optional<Double> getKnockbackResistanceOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.KNOCKBACK_RESISTANCE);
    }

    /**
     * 设置僵尸的击退抗性
     */
    public static void setKnockbackResistance(Zombie zombie, double resistance) {
        if (resistance < 0.0 || resistance > 1.0) {
            sendErrorToPlayers("击退抗性必须在0.0到1.0之间，当前值: " + resistance);
            throw new IllegalArgumentException("Knockback resistance must be between 0.0 and 1.0, but got: " + resistance);
        }
        setVanillaAttribute(zombie, Attributes.KNOCKBACK_RESISTANCE, resistance);
    }

    /**
     * 获取僵尸的跟随范围（可能返回 null）
     */
    @Nullable
    public static Double getFollowRange(Zombie zombie) {
        return getVanillaAttribute(zombie, Attributes.FOLLOW_RANGE);
    }

    /**
     * 获取僵尸的跟随范围（返回 Optional）
     */
    public static Optional<Double> getFollowRangeOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, Attributes.FOLLOW_RANGE);
    }

    /**
     * 设置僵尸的跟随范围
     */
    public static void setFollowRange(Zombie zombie, double range) {
        if (range <= 0.0) {
            sendErrorToPlayers("跟随范围必须大于0.0，当前值: " + range);
            throw new IllegalArgumentException("Follow range must be greater than 0.0, but got: " + range);
        }
        setVanillaAttribute(zombie, Attributes.FOLLOW_RANGE, range);
    }


    /**
     * 获取僵尸的游泳速度（Forge 属性，可能返回 null）
     */
    @Nullable
    public static Double getSwimSpeed(Zombie zombie) {
        return getVanillaAttribute(zombie, ForgeMod.SWIM_SPEED.get());
    }

    /**
     * 获取僵尸的游泳速度（Forge 属性，返回 Optional）
     */
    public static Optional<Double> getSwimSpeedOptional(Zombie zombie) {
        return getVanillaAttributeOptional(zombie, ForgeMod.SWIM_SPEED.get());
    }

    /**
     * 设置僵尸的游泳速度（Forge 属性）
     */
    public static void setSwimSpeed(Zombie zombie, double speed) {
        if (speed <= 0.0) {
            sendErrorToPlayers("游泳速度必须大于0.0，当前值: " + speed);
            throw new IllegalArgumentException("Swim speed must be greater than 0.0, but got: " + speed);
        }
        setVanillaAttribute(zombie, ForgeMod.SWIM_SPEED.get(), speed);
    }

    public static boolean fireImmune(IZombieData data) {
        return data.fireImmune();
    }

    public static void setFireImmune(IZombieData data, boolean value) {
        data.setFireImmune(value);
    }

    public static void setEnhancedSense(IZombieData data, boolean value) {
        data.setEnhancedSense(value);
    }

    // ==================== Helper Methods ====================

    /**
     * 向所有在线玩家发送错误消息
     * @param message 错误消息内容
     */
    private static void sendErrorToPlayers(String message) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.displayClientMessage(Component.literal("§c[错误] " + message), false);
                });
            }
        } catch (Exception e) {
            // 静默失败，不影响原有逻辑
        }
    }

}