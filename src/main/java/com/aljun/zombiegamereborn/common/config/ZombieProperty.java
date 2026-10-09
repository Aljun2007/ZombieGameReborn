package com.aljun.zombiegamereborn.common.config;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.awareness.AwarenessSettings;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.monster.Drowned;
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
    @SerializedName("zombie_swim_speed_modify")
    public double zombieSwimSpeedModify = 1.0d;
    @SerializedName("drowned_swim_speed_modify")
    public double drownedSwimSpeedModify = 1.0d;
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
    // ---- 感知系统（声音 / 气味 / 冲击 / 警报）----
    // 取代旧的 sense_* 常量组：旧实现把半径写死在静态字段上再靠 refresh 回写，
    // 现在统一由一份不可变设置快照驱动，见 AwarenessSettings / AwarenessTuning。
    @SerializedName("awareness_enabled")
    public boolean awarenessEnabled = true;
    @SerializedName("awareness_feedback_sounds")
    public boolean awarenessFeedbackSounds = true;
    @SerializedName("awareness_poll_interval")
    public int awarenessPollInterval = 10;
    @SerializedName("awareness_sound_radius")
    public double awarenessSoundRadius = 48.0d;
    @SerializedName("awareness_sound_strength")
    public int awarenessSoundStrength = 10;
    @SerializedName("awareness_impact_radius")
    public double awarenessImpactRadius = 64.0d;
    @SerializedName("awareness_alert_radius")
    public double awarenessAlertRadius = 24.0d;
    @SerializedName("awareness_light_radius")
    public double awarenessLightRadius = 24.0d;
    @SerializedName("awareness_scent_decay_ticks")
    public int awarenessScentDecayTicks = 600;
    @SerializedName("boundless_hunting")
    public boolean boundlessHunting = false;
    @SerializedName("blood_moon_boundless_hunting")
    public boolean bloodMoonBoundlessHunting = false;
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
    public double piglinCollisionAngerChance = 1.0d;
    @SerializedName("piglin_angry_mode")
    public boolean piglinAngryMode = false;
    @SerializedName("break_light_sources")
    public boolean breakLightSources = false;
    @SerializedName("can_throw_tnt")
    public boolean canThrowTNT = false;
    @SerializedName("block_stab_immune_probability")
    public double blockStabImmuneProbability = 0.0d;
    @SerializedName("ladder_climb_probability")
    public double ladderClimbProbability = 0.0d;

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
        property.zombieSwimSpeedModify = getDoubleOrDefault(obj, "zombie_swim_speed_modify", 1.0d);
        property.drownedSwimSpeedModify = getDoubleOrDefault(obj, "drowned_swim_speed_modify", 1.0d);
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
        property.awarenessEnabled = getBooleanOrDefault(obj, "awareness_enabled", true);
        property.awarenessFeedbackSounds = getBooleanOrDefault(obj, "awareness_feedback_sounds", true);
        property.awarenessPollInterval = getIntOrDefault(obj, "awareness_poll_interval", 10);
        property.awarenessSoundRadius = getDoubleOrDefault(obj, "awareness_sound_radius", 48.0d);
        property.awarenessSoundStrength = getIntOrDefault(obj, "awareness_sound_strength", 10);
        property.awarenessImpactRadius = getDoubleOrDefault(obj, "awareness_impact_radius", 64.0d);
        property.awarenessAlertRadius = getDoubleOrDefault(obj, "awareness_alert_radius", 24.0d);
        property.awarenessLightRadius = getDoubleOrDefault(obj, "awareness_light_radius", 24.0d);
        property.awarenessScentDecayTicks = getIntOrDefault(obj, "awareness_scent_decay_ticks", 600);
        property.boundlessHunting = getBooleanOrDefault(obj, "boundless_hunting", false);
        property.bloodMoonBoundlessHunting = getBooleanOrDefault(obj, "blood_moon_boundless_hunting", false);
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
        property.breakLightSources = getBooleanOrDefault(obj, "break_light_sources", false);
        property.canThrowTNT = getBooleanOrDefault(obj, "can_throw_tnt", false);
        property.blockStabImmuneProbability = getDoubleOrDefault(obj, "block_stab_immune_probability", 0.0d);
        property.ladderClimbProbability = getDoubleOrDefault(obj, "ladder_climb_probability", 0.0d);

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
        obj.addProperty("zombie_swim_speed_modify", zombieSwimSpeedModify);
        obj.addProperty("drowned_swim_speed_modify", drownedSwimSpeedModify);
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
        obj.addProperty("awareness_enabled", awarenessEnabled);
        obj.addProperty("awareness_feedback_sounds", awarenessFeedbackSounds);
        obj.addProperty("awareness_poll_interval", awarenessPollInterval);
        obj.addProperty("awareness_sound_radius", awarenessSoundRadius);
        obj.addProperty("awareness_sound_strength", awarenessSoundStrength);
        obj.addProperty("awareness_impact_radius", awarenessImpactRadius);
        obj.addProperty("awareness_alert_radius", awarenessAlertRadius);
        obj.addProperty("awareness_light_radius", awarenessLightRadius);
        obj.addProperty("awareness_scent_decay_ticks", awarenessScentDecayTicks);
        obj.addProperty("boundless_hunting", boundlessHunting);
        obj.addProperty("blood_moon_boundless_hunting", bloodMoonBoundlessHunting);
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
        obj.addProperty("break_light_sources", breakLightSources);
        obj.addProperty("can_throw_tnt", canThrowTNT);
        obj.addProperty("block_stab_immune_probability", blockStabImmuneProbability);
        obj.addProperty("ladder_climb_probability", ladderClimbProbability);

        return obj;

    }

    /**
     * 把阶段配置翻译成感知系统的参数快照。
     * <p>
     * 由 {@code GamePropertyRefresher} 在阶段推进时调用一次，整体替换全局快照 ——
     * 热路径（僵尸轮询、事件写入）因此只读一个 volatile 引用，不需要每次查配置。
     */
    public AwarenessSettings toAwarenessSettings() {
        AwarenessSettings settings = new AwarenessSettings();
        settings.enabled = this.awarenessEnabled;
        settings.feedbackSounds = this.awarenessFeedbackSounds;
        settings.pollIntervalTicks = this.awarenessPollInterval;
        settings.soundRadius = this.awarenessSoundRadius;
        settings.soundStrength = this.awarenessSoundStrength;
        settings.impactRadius = this.awarenessImpactRadius;
        settings.alertRadius = this.awarenessAlertRadius;
        settings.lightRadius = this.awarenessLightRadius;
        settings.scentDecayTicks = this.awarenessScentDecayTicks;
        return settings;
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

        // 感知系统不再按僵尸逐个开关：所有僵尸共用同一套由阶段配置驱动的感知参数
        ZGRZombieAttributesAPI.setFleeSun(data,this.fleeSun);

        ZGRZombieAttributesAPI.setAmbientVolumeModify(data,this.ambientVolumeModify);
        ZGRZombieAttributesAPI.setStepVolumeModify(data,this.stepVolumeModify);
        ZGRZombieAttributesAPI.setBlockStabImmune(data, RandomUtils.booleanByChance(this.blockStabImmuneProbability));
        ZGRZombieAttributesAPI.setLadderClimb(data, RandomUtils.booleanByChance(this.ladderClimbProbability));

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

        if (this.boundlessHunting || (this.bloodMoonBoundlessHunting
                && ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isBloodMoon(zombie.level().getServer()))) {
            ZGRZombieAttributesAPI.setFollowRange(zombie, 512.0);
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

        double baseSwimSpeed = ZGRZombieAttributesAPI.getSwimSpeedOptional(zombie).orElse(1.0);
        if (zombie instanceof Drowned) {
            ZGRZombieAttributesAPI.setSwimSpeed(zombie, this.drownedSwimSpeedModify * baseSwimSpeed);
        } else {
            ZGRZombieAttributesAPI.setSwimSpeed(zombie, this.zombieSwimSpeedModify * baseSwimSpeed);
        }

        double baseMiningSpeed = ZGRZombieAttributesAPI.getMiningSpeed(data);
        ZGRZombieAttributesAPI.setMiningSpeed(data, this.miningSpeedModify * baseMiningSpeed);

        data.enableJumpAttack(this.canJumpAttack);
        data.enableThrowTNT(this.canThrowTNT);

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