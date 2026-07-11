package com.aljun.zombiegamereborn.common.entity.equipement;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZombieEquipmentHelper {

    public static final String WEAPON_SWORD = "sword";
    public static final String WEAPON_AXE = "axe";
    public static final String WEAPON_PICKAXE = "pickaxe";
    public static final String WEAPON_SHOVEL = "shovel";
    public static final String WEAPON_HOE = "hoe";

    public static final String SLOT_HELMET = "helmet";
    public static final String SLOT_CHESTPLATE = "chestplate";
    public static final String SLOT_LEGGINGS = "leggings";
    public static final String SLOT_BOOTS = "boots";

    public static final String MATERIAL_LEATHER = "leather";
    public static final String MATERIAL_GOLD = "gold";
    public static final String MATERIAL_CHAIN = "chain";
    public static final String MATERIAL_IRON = "iron";
    public static final String MATERIAL_DIAMOND = "diamond";
    public static final String MATERIAL_NETHERITE = "netherite";

    public static final List<EquipmentEntry> EQUIPMENTS = new ArrayList<>();
    public static final List<MaterialEntry> MATERIALS = new ArrayList<>();
    public static final Map<String, MaterialData> MATERIAL_DATA = new HashMap<>();

    private static RandomUtils.RandomPool<String> equipmentPool = null;
    private static RandomUtils.NormalRoadPool<String> materialPool = null;
    private static double lastMeanOffset = Double.NaN;

    static {
        register();
    }

    public static void register() {
        registerWeapon(WEAPON_SWORD, 1.0d);
        registerWeapon(WEAPON_AXE, 0.8d);
        registerWeapon(WEAPON_PICKAXE, 0.6d);
        registerWeapon(WEAPON_SHOVEL, 0.4d);
        registerWeapon(WEAPON_HOE, 0.2d);

        registerMaterial(MATERIAL_LEATHER, 2.2,
                new MaterialData(MATERIAL_LEATHER)
                        .addEquipment(SLOT_HELMET, Items.LEATHER_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.LEATHER_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.LEATHER_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.LEATHER_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.WOODEN_SWORD)
                        .addEquipment(WEAPON_AXE, Items.WOODEN_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.WOODEN_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.WOODEN_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.WOODEN_HOE)
        );

        registerMaterial(MATERIAL_GOLD, 3.8,
                new MaterialData(MATERIAL_GOLD)
                        .addEquipment(SLOT_HELMET, Items.GOLDEN_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.GOLDEN_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.GOLDEN_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.GOLDEN_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.GOLDEN_SWORD)
                        .addEquipment(WEAPON_AXE, Items.GOLDEN_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.GOLDEN_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.GOLDEN_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.GOLDEN_HOE)
        );

        registerMaterial(MATERIAL_CHAIN, 5.5,
                new MaterialData(MATERIAL_CHAIN)
                        .addEquipment(SLOT_HELMET, Items.CHAINMAIL_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.CHAINMAIL_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.CHAINMAIL_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.STONE_SWORD)
                        .addEquipment(WEAPON_AXE, Items.STONE_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.STONE_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.STONE_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.STONE_HOE)
        );

        registerMaterial(MATERIAL_IRON, 7.5,
                new MaterialData(MATERIAL_IRON)
                        .addEquipment(SLOT_HELMET, Items.IRON_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.IRON_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.IRON_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.IRON_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.IRON_SWORD)
                        .addEquipment(WEAPON_AXE, Items.IRON_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.IRON_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.IRON_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.IRON_HOE)
        );

        registerMaterial(MATERIAL_DIAMOND, 9.0,
                new MaterialData(MATERIAL_DIAMOND)
                        .addEquipment(SLOT_HELMET, Items.DIAMOND_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.DIAMOND_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.DIAMOND_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.DIAMOND_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.DIAMOND_SWORD)
                        .addEquipment(WEAPON_AXE, Items.DIAMOND_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.DIAMOND_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.DIAMOND_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.DIAMOND_HOE)
        );

        registerMaterial(MATERIAL_NETHERITE, 10.0,
                new MaterialData(MATERIAL_NETHERITE)
                        .addEquipment(SLOT_HELMET, Items.NETHERITE_HELMET)
                        .addEquipment(SLOT_CHESTPLATE, Items.NETHERITE_CHESTPLATE)
                        .addEquipment(SLOT_LEGGINGS, Items.NETHERITE_LEGGINGS)
                        .addEquipment(SLOT_BOOTS, Items.NETHERITE_BOOTS)
                        .addEquipment(WEAPON_SWORD, Items.NETHERITE_SWORD)
                        .addEquipment(WEAPON_AXE, Items.NETHERITE_AXE)
                        .addEquipment(WEAPON_PICKAXE, Items.NETHERITE_PICKAXE)
                        .addEquipment(WEAPON_SHOVEL, Items.NETHERITE_SHOVEL)
                        .addEquipment(WEAPON_HOE, Items.NETHERITE_HOE)
        );
    }

    public static void registerWeapon(String id, double weight) {
        EQUIPMENTS.add(new EquipmentEntry(id, weight));
        equipmentPool = null;
    }

    public static void registerMaterial(String id, double threshold, MaterialData data) {
        MATERIALS.add(new MaterialEntry(id, threshold, data));
        MATERIAL_DATA.put(id, data);
        materialPool = null;
    }

    public static void applyFullEquipment(Zombie zombie) {
        ServerLevel serverLevel = (ServerLevel) zombie.level();
        var stageProperty = ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(),zombie.blockPosition());
        float difficulty = stageProperty.calculateDifficulty(serverLevel, zombie.blockPosition());
        double meanOffset = stageProperty.zombieProperty.equipmentQualityMeanOffset;
        double probFactor = stageProperty.zombieProperty.equipmentProbabilityFactor;

        RandomSource random = zombie.getRandom();
        ensurePoolsBuilt(meanOffset);
        if (equipmentPool == null || materialPool == null || MATERIAL_DATA.isEmpty()) {
            return;
        }

        float d = clamp(difficulty, 0.0f, 1.0f);

        String materialId = zombie instanceof ZombifiedPiglin ? MATERIAL_GOLD : materialPool.nextValue();
        MaterialData material = MATERIAL_DATA.get(materialId);
        if (material == null) return;

        float weaponChance = (0.01f + 0.04f * d) * (float) probFactor;
        if (random.nextFloat() < weaponChance) {
            String weaponType = equipmentPool.nextValue();
            zombie.setItemSlot(EquipmentSlot.MAINHAND, material.getStack(weaponType));
        }

        float armorChance = 0.16f * (d - 0.10f) * (float) probFactor;
        if (RandomUtils.booleanByChance(armorChance, random)) {
            if (RandomUtils.booleanByChance(0.8f + 0.2f * d, random))
                zombie.setItemSlot(EquipmentSlot.HEAD, material.getStack(SLOT_HELMET));
            if (RandomUtils.booleanByChance(0.6f + 0.3f * d, random))
                zombie.setItemSlot(EquipmentSlot.CHEST, material.getStack(SLOT_CHESTPLATE));
            if (RandomUtils.booleanByChance(0.5f + 0.3f * d, random))
                zombie.setItemSlot(EquipmentSlot.LEGS, material.getStack(SLOT_LEGGINGS));
            if (RandomUtils.booleanByChance(0.4f + 0.3f * d, random))
                zombie.setItemSlot(EquipmentSlot.FEET, material.getStack(SLOT_BOOTS));
        }
    }

    public static void applyEnchantments(Zombie zombie) {
        ServerLevel serverLevel = (ServerLevel) zombie.level();
        var stageProperty = ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(),zombie.blockPosition());
        float difficulty = stageProperty.calculateDifficulty(serverLevel, zombie.blockPosition());
        double enchantFactor = stageProperty.zombieProperty.equipmentEnchantmentFactor;

        RandomSource random = zombie.getRandom();
        ItemStack mainHand = zombie.getMainHandItem();
        if (!mainHand.isEmpty() && RandomUtils.booleanByChance(0.25f * difficulty * enchantFactor, random)) {
            int level = (int) (5.0f + difficulty * random.nextInt(18));
            zombie.setItemSlot(EquipmentSlot.MAINHAND,
                    EnchantmentHelper.enchantItem(random, mainHand, level, false));
        }

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = zombie.getItemBySlot(slot);
            if (!armor.isEmpty() && RandomUtils.booleanByChance(0.5f * difficulty * enchantFactor, random)) {
                int level = (int) (5.0f + difficulty * random.nextInt(18));
                zombie.setItemSlot(slot,
                        EnchantmentHelper.enchantItem(random, armor, level, false));
            }
        }
    }

    private static void ensurePoolsBuilt(double meanOffset) {
        if (equipmentPool == null) buildWeaponPool();
        if (materialPool == null || meanOffset != lastMeanOffset) {
            buildMaterialPool(meanOffset);
            lastMeanOffset = meanOffset;
        }
    }

    private static void buildWeaponPool() {
        if (EQUIPMENTS.isEmpty()) { equipmentPool = null; return; }
        var builder = RandomUtils.RandomPool.builder(String.class);
        for (EquipmentEntry entry : EQUIPMENTS) builder.add(entry.id, entry.weight);
        equipmentPool = builder.build();
    }

    private static void buildMaterialPool(double meanOffset) {
        if (MATERIALS.isEmpty()) { materialPool = null; return; }
        MATERIALS.sort((a, b) -> Double.compare(a.threshold, b.threshold));
        var builder = RandomUtils.NormalRoadPool.<String>builder();
        for (MaterialEntry entry : MATERIALS) builder.add(entry.id, entry.threshold);
        builder.defaultValue(MATERIALS.get(0).id).mu(2.58 + meanOffset).sigma(1.14);
        materialPool = builder.build();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public record EquipmentEntry(String id, double weight) {}

    public record MaterialEntry(String id, double threshold, MaterialData data) {}

    public static class MaterialData {
        public final String id;
        public final Map<String, Item> items = new HashMap<>();

        public MaterialData(String id) {
            this.id = id;
        }

        public MaterialData addEquipment(String type, Item item) {
            items.put(type, item);
            return this;
        }

        public ItemStack getStack(String type) {
            Item item = items.get(type);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        }
    }
}