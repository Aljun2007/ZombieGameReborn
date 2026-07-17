package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.monster.Zombie;

import java.lang.reflect.Type;

import static com.aljun.zombiegamereborn.utils.JsonUtils.*;

public class ZombieProperty {

    private static final double DEFAULT_MOVEMENT_SPEED = 1.0;
    private static final double DEFAULT_ATTACK_DAMAGE = 1.0;
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double DEFAULT_ARMOR = 2.0;
    private static final double DEFAULT_MINING_SPEED = 1.0;
    private static final double DEFAULT_PROBABILITY = 0.0;
    @SerializedName("movement_speed_modify")
    public double movementSpeedModify = 1.0d;
    @SerializedName("attack_damage_modify")
    public double attackDamageModify = 1.0d;
    @SerializedName("max_health")
    public double maxHealth = 20.0d;
    @SerializedName("knockback_resistance")
    public double knockbackResistance = 0.0d;
    @SerializedName("armor")
    public double armor = 2.0d;
    @SerializedName("armor_toughness")
    public double armorToughness = 0.0d;
    @SerializedName("mining_speed_modify")
    public double miningSpeedModify = 1.0d;
    @SerializedName("can_swim_probability")
    public double canSwimProbability = 0.0d;
    @SerializedName("sun_immunity_probability")
    public double sunImmunityProbability = 0.0d;
    @SerializedName("fire_immune_probability")
    public double fireImmuneProbability = 0.0d;
    @SerializedName("baby_probability")
    public double babyProbability = 0.05d;
    @SerializedName("can_pick_up_loot_probability")
    public double canPickUpLootCoefficient = 0.55d;
    @SerializedName("max_empowered_zombie_builder_count")
    public int maxEmpoweredZombieBuilderCount = 100;
    @SerializedName("max_empowered_zombie_miner_count")
    public int maxEmpoweredZombieMinerCount = 100;
    @SerializedName("follow_range")
    public double followRange = 40.0d;
    @SerializedName("follow_must_see")
    public boolean followMustSee = true;
    @SerializedName("sense_bleeding_radius")
    public double senseBleedingRadius = 64d;
    @SerializedName("sense_bleeding_lifespan")
    public int senseBleedingLifespan = 400;
    @SerializedName("sense_block_radius")
    public double senseBlockRadius = 16.0d;
    @SerializedName("sense_block_lifespan")
    public int senseBlockLifespan = 100;
    @SerializedName("sense_gun_shot_radius")
    public double senseGunShotRadius = 64.0d;
    @SerializedName("sense_gun_shot_lifespan")
    public int senseGunShotLifespan = 400;
    @SerializedName("sense_gun_shot_silenced_radius")
    public double senseGunShotSilencedRadius = 16.0d;
    @SerializedName("sense_gun_shot_silenced_lifespan")
    public int senseGunShotSilencedLifespan = 100;
    @SerializedName("enhanced_sense")
    public boolean enhancedSense = false;
    @SerializedName("musket_mod_gun_damage_modify")
    public double musketModGunDamageModify = 0.5d;
    @SerializedName("do_swimming_zombie_convert")
    public boolean doSwimmingZombieConvert = false;
    @SerializedName("can_jump_attack")
    public boolean canJumpAttack = false;
    @SerializedName("flee_sun")
    public boolean fleeSun = false;
    @SerializedName("can_zombie_guard_continue_use_weapons")
    public boolean canZombieGuardContinueUseWeapons = false;
    @SerializedName("ambient_volume_modify")
    public double ambientVolumeModify = 1.0d;
    @SerializedName("step_volume_modify")
    public double stepVolumeModify = 1.0d;
    @SerializedName("equipment_quality_mean_offset")
    public double equipmentQualityMeanOffset = 0.0d;  // 装备品质均值偏移: 越大材质越好, 0=原版
    @SerializedName("equipment_probability_factor")
    public double equipmentProbabilityFactor = 1.0d;
    @SerializedName("equipment_enchantment_factor")
    public double equipmentEnchantmentFactor = 1.0d;
    @SerializedName("enable_piglin_collision_anger")
    public boolean enablePiglinCollisionAnger = false;
    @SerializedName("piglin_collision_anger_chance")
    public double piglinCollisionAngerChance = 0.25d;
    @SerializedName("piglin_angry_mode")
    public boolean piglinAngryMode = false;

    public ZombieProperty() {
    }

    /**
     * 从 JSON 对象反序列化（供适配器使用）
     */
    private static ZombieProperty fromJsonObject(JsonObject obj) {
        ZombieProperty property = new ZombieProperty();

        property.movementSpeedModify = getDoubleOrDefault(obj, "movement_speed_modify", DEFAULT_MOVEMENT_SPEED);
        property.attackDamageModify = getDoubleOrDefault(obj, "attack_damage_modify", DEFAULT_ATTACK_DAMAGE);
        property.maxHealth = getDoubleOrDefault(obj, "max_health", DEFAULT_MAX_HEALTH);
        property.knockbackResistance = getDoubleOrDefault(obj, "knockback_resistance", 0.0);
        property.armor = getDoubleOrDefault(obj, "armor", DEFAULT_ARMOR);
        property.armorToughness = getDoubleOrDefault(obj, "armor_toughness", 0.0);
        property.miningSpeedModify = getDoubleOrDefault(obj, "mining_speed_modify", DEFAULT_MINING_SPEED);
        property.canSwimProbability = getDoubleOrDefault(obj, "can_swim_probability", DEFAULT_PROBABILITY);
        property.sunImmunityProbability = getDoubleOrDefault(obj, "sun_immunity_probability", DEFAULT_PROBABILITY);
        property.fireImmuneProbability = getDoubleOrDefault(obj, "fire_immune_probability", DEFAULT_PROBABILITY);
        property.babyProbability = getDoubleOrDefault(obj, "baby_probability", 0.05d);
        property.canPickUpLootCoefficient = getDoubleOrDefault(obj, "can_pick_up_loot_coefficient", 0.55d);
        property.maxEmpoweredZombieMinerCount = getIntOrDefault(obj, "max_empowered_zombie_miner_count", 100);
        property.maxEmpoweredZombieBuilderCount = getIntOrDefault(obj, "max_empowered_zombie_builder_count", 100);
        property.followRange = getDoubleOrDefault(obj, "follow_range", 40.0d);
        property.doSwimmingZombieConvert = getBooleanOrDefault(obj, "do_swimming_zombie_convert", false);
        property.canJumpAttack = getBooleanOrDefault(obj, "can_jump_attack", false);
        property.followMustSee = getBooleanOrDefault(obj, "follow_must_see", true);
        property.senseBleedingRadius = getDoubleOrDefault(obj, "sense_bleeding_radius", 16.0d);
        property.senseBleedingLifespan = getIntOrDefault(obj, "sense_bleeding_lifespan", 400);
        property.senseBlockRadius = getDoubleOrDefault(obj, "sense_block_radius", 16.0d);
        property.senseBlockLifespan = getIntOrDefault(obj, "sense_block_lifespan", 100);
        property.senseGunShotRadius = getDoubleOrDefault(obj, "sense_gun_shot_radius", 64.0d);
        property.senseGunShotLifespan = getIntOrDefault(obj, "sense_gun_shot_lifespan", 400);
        property.senseGunShotSilencedRadius = getDoubleOrDefault(obj, "sense_gun_shot_silenced_radius", 16.0d);
        property.senseGunShotSilencedLifespan = getIntOrDefault(obj, "sense_gun_shot_silenced_lifespan", 100);
        property.enhancedSense = getBooleanOrDefault(obj, "enhanced_sense", false);
        property.musketModGunDamageModify = getDoubleOrDefault(obj, "musket_mod_gun_damage_modify", 0.5d);
        property.canZombieGuardContinueUseWeapons = getBooleanOrDefault(obj, "can_zombie_guard_continue_use_weapons", false);
        property.fleeSun = getBooleanOrDefault(obj, "flee_sun", false);
        property.ambientVolumeModify = getDoubleOrDefault(obj, "ambient_volume_modify", 1.0d);
        property.stepVolumeModify = getDoubleOrDefault(obj, "step_volume_modify", 1.0d);
        property.equipmentQualityMeanOffset = getDoubleOrDefault(obj, "equipment_quality_mean_offset", 0.0d);
        property.equipmentProbabilityFactor = getDoubleOrDefault(obj, "equipment_probability_factor", 1.0d);
        property.equipmentEnchantmentFactor = getDoubleOrDefault(obj, "equipment_enchantment_factor", 1.0d);
        property.enablePiglinCollisionAnger = getBooleanOrDefault(obj, "enable_piglin_collision_anger", false);
        property.piglinCollisionAngerChance = getDoubleOrDefault(obj, "piglin_collision_anger_chance", 0.25d);
        property.piglinAngryMode = getBooleanOrDefault(obj, "piglin_angry_mode", false);

        return property;

    }

    /**
     * 转换为 JsonObject（供适配器使用）
     */
    private JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("movement_speed_modify", movementSpeedModify);
        obj.addProperty("attack_damage_modify", attackDamageModify);
        obj.addProperty("max_health", maxHealth);
        obj.addProperty("knockback_resistance", knockbackResistance);
        obj.addProperty("armor", armor);
        obj.addProperty("armor_toughness", armorToughness);
        obj.addProperty("mining_speed_modify", miningSpeedModify);
        obj.addProperty("can_swim_probability", canSwimProbability);
        obj.addProperty("sun_immunity_probability", sunImmunityProbability);
        obj.addProperty("fire_immune_probability", fireImmuneProbability);
        obj.addProperty("baby_probability", babyProbability);
        obj.addProperty("can_pick_up_loot_coefficient", canPickUpLootCoefficient);
        obj.addProperty("max_empowered_zombie_miner_count", maxEmpoweredZombieMinerCount);
        obj.addProperty("max_empowered_zombie_builder_count", maxEmpoweredZombieBuilderCount);
        obj.addProperty("follow_range", followRange);
        obj.addProperty("do_swimming_zombie_convert", doSwimmingZombieConvert);
        obj.addProperty("can_jump_attack", canJumpAttack);
        obj.addProperty("follow_must_see", followMustSee);
        obj.addProperty("sense_bleeding_radius", senseBleedingRadius);
        obj.addProperty("sense_bleeding_lifespan", senseBleedingLifespan);
        obj.addProperty("sense_block_radius", senseBlockRadius);
        obj.addProperty("sense_block_lifespan", senseBlockLifespan);
        obj.addProperty("sense_gun_shot_radius", senseGunShotRadius);
        obj.addProperty("sense_gun_shot_lifespan", senseGunShotLifespan);
        obj.addProperty("sense_gun_shot_silenced_radius", senseGunShotSilencedRadius);
        obj.addProperty("sense_gun_shot_silenced_lifespan", senseGunShotSilencedLifespan);
        obj.addProperty("enhanced_sense", enhancedSense);
        obj.addProperty("musket_mod_gun_damage_modify", musketModGunDamageModify);
        obj.addProperty("can_zombie_guard_continue_use_weapons", canZombieGuardContinueUseWeapons);
        obj.addProperty("flee_sun", fleeSun);
        obj.addProperty("ambient_volume_modify", ambientVolumeModify);
        obj.addProperty("step_volume_modify", stepVolumeModify);
        obj.addProperty("equipment_quality_mean_offset", equipmentQualityMeanOffset);
        obj.addProperty("equipment_probability_factor", equipmentProbabilityFactor);
        obj.addProperty("equipment_enchantment_factor", equipmentEnchantmentFactor);
        obj.addProperty("enable_piglin_collision_anger", enablePiglinCollisionAnger);
        obj.addProperty("piglin_collision_anger_chance", piglinCollisionAngerChance);
        obj.addProperty("piglin_angry_mode", piglinAngryMode);

        return obj;

    }

    @SuppressWarnings("all")
    public void loadZombieAttributes(Zombie zombie) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        applyProbabilisticTraits(zombie, data);
        applyAttributeModifiers(zombie, data);
    }

    private void applyProbabilisticTraits(Zombie zombie, IZombieData data) {

        ZGRZombieAttributesAPI.setCanSwim(data, RandomUtils.booleanByChance(this.canSwimProbability));
        ZGRZombieAttributesAPI.setSunSensitive(data, !RandomUtils.booleanByChance(this.sunImmunityProbability));
        ZGRZombieAttributesAPI.setFireImmune(data, RandomUtils.booleanByChance(this.fireImmuneProbability));

        ZGRZombieAttributesAPI.setEnhancedSense(data,this.enhancedSense);
        ZGRZombieAttributesAPI.setFleeSun(data,this.fleeSun);

        ZGRZombieAttributesAPI.setAmbientVolumeModify(data,this.ambientVolumeModify);
        ZGRZombieAttributesAPI.setStepVolumeModify(data,this.stepVolumeModify);

        if (this.babyProbability > 0.05d) {
            if (!zombie.isBaby()) {
                zombie.setBaby(RandomUtils.booleanByChance((this.babyProbability - 0.05d) / 0.95d));
            }
        } else if (this.babyProbability < 0.05d) {
            if (zombie.isBaby()) {
                if (!RandomUtils.booleanByChance(this.babyProbability / 0.05d)) {
                    zombie.setBaby(false);
                }
            }
        }


    }

    private void applyAttributeModifiers(Zombie zombie, IZombieData data) {
        ZGRZombieAttributesAPI.setArmor(zombie, this.armor);
        ZGRZombieAttributesAPI.setArmorToughness(zombie, this.armorToughness);
        ZGRZombieAttributesAPI.setKnockbackResistance(zombie, this.knockbackResistance);
        ZGRZombieAttributesAPI.setMaxHealth(zombie, this.maxHealth);

        if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(zombie.level().getServer())) {
            ZGRZombieAttributesAPI.setFollowRange(zombie, this.followRange * 2);
            data.setFollowMustSee(true);
        } else {
            ZGRZombieAttributesAPI.setFollowRange(zombie, this.followRange);
            data.setFollowMustSee(this.followMustSee);
        }

        double baseAttackDamage = ZGRZombieAttributesAPI.getAttackDamageOptional(zombie).orElse(3.0);
        ZGRZombieAttributesAPI.setAttackDamage(zombie, this.attackDamageModify * baseAttackDamage);

        double baseMovementSpeed = ZGRZombieAttributesAPI.getMovementSpeedOptional(zombie).orElse(0.23);
        ZGRZombieAttributesAPI.setMovementSpeed(zombie, this.movementSpeedModify * baseMovementSpeed);
        data.setAttributesMovementSpeedModify(this.movementSpeedModify);

        double baseMiningSpeed = ZGRZombieAttributesAPI.getMiningSpeed(data);
        ZGRZombieAttributesAPI.setMiningSpeed(data, this.miningSpeedModify * baseMiningSpeed);

        data.enableJumpAttack(this.canJumpAttack);

    }

    /**
     * ZombieProperty 的 JSON 适配器
     */
    public static class ZombiePropertyAdapter implements JsonSerializer<ZombieProperty>, JsonDeserializer<ZombieProperty> {

        @Override
        public JsonElement serialize(ZombieProperty src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return src.toJsonObject();
        }

        @Override
        public ZombieProperty deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return new ZombieProperty();
            }

            if (json.isJsonObject()) {
                return ZombieProperty.fromJsonObject(json.getAsJsonObject());
            }

            return new ZombieProperty();
        }
    }
}