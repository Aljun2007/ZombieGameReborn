package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.diplomat.ZGRDiplomacyCenter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ZombiePropertyScreen extends AbstractBranchConfigScreen {


    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ZombieProperty.class, new ZombieProperty.ZombiePropertyAdapter())
            .create();

    protected ZombiePropertyScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        localJson = GSON.toJsonTree(new ZombieProperty()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        this.tabs.add(new ConfigTab("基础属性", this::initAttributesTab));
        this.tabs.add(new ConfigTab("特殊能力", this::initAbilityTab));
        this.tabs.add(new ConfigTab("音量", this::initVolumeTab));
        this.tabs.add(new ConfigTab("索敌感知", this::initSenseTab));
        this.tabs.add(new ConfigTab("模组联动", this::initModCompatTab));
        this.tabs.add(new ConfigTab("性能", this::initPerformanceTab));
    }

    private void initAttributesTab(SimpleSettingsPanel panel) {
        panel.addLabel("§6§l基础属性");
        panel.addDoubleEditBox("移速修正比", "movement_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("伤害修正比", "attack_damage_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("最大生命值", "max_health", 20.0, 1.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("护甲值", "armor", 2.0, 0.0, 30.0);
        panel.addDoubleEditBox("护甲韧性", "armor_toughness", 0.0, 0.0, 20.0);
        panel.addDoubleEditBox("击退抗性", "knockback_resistance", 0.0, 0.0, 1.0);
        panel.addDoubleEditBox("挖掘速度修正比", "mining_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
    }

    private void initAbilityTab(SimpleSettingsPanel panel) {
        panel.addLabel("§6§l特殊能力");
        panel.addDoubleEditBox("能游泳概率", "can_swim_probability", 0.0, 0.0, 1.0);
        panel.addCheckBox("游泳僵尸水淹转化", "do_swimming_zombie_convert", false);
        panel.addCheckBox("能跳跃攻击", "can_jump_attack", false);
        panel.addDoubleEditBox("阳光免疫概率", "sun_immunity_probability", 0.0d, 0.0, 1.0);
        panel.addCheckBox("敏感时避免阳光", "flee_sun", false);
        panel.addDoubleEditBox("火焰免疫概率", "fire_immunity_probability", 0.0d, 0.0, 1.0);
        panel.addDoubleEditBox("幼体概率", "baby_probability", 0.05d, 0.0d, 1.0);
        panel.addDoubleEditBox("捡物品计算系数", "can_pick_up_loot_coefficient", 0.55d, 0.0, Double.MAX_VALUE);
    }

    private void initVolumeTab(SimpleSettingsPanel panel) {
        panel.addLabel("§6§l音量");
        panel.addDoubleEditBox("咆哮音量修正比", "ambient_volume_modify", 1.0d, 0.0d, 1.0d);
        panel.addDoubleEditBox("脚步音量修正比", "step_volume_modify", 1.0d, 0.0d, 1.0d);

    }

    private void initPerformanceTab(SimpleSettingsPanel panel) {
        panel.addLabel("§6§l性能");
        panel.addIntEditBox("最大激活挖掘者数量", "max_empowered_zombie_miner_count", 100, 0, Integer.MAX_VALUE);
        panel.addIntEditBox("最大激活建造者数量", "max_empowered_zombie_builder_count", 100, 0, Integer.MAX_VALUE);
    }

    private void initSenseTab(SimpleSettingsPanel panel) {
        panel.addLabel("§6§l原版索敌");
        panel.addDoubleEditBox("基础索敌范围", "follow_range", 40.0, 0.0, Double.MAX_VALUE);
        panel.addCheckBox("基础索敌必须看见", "follow_must_see", true);

        panel.addLabel("§6§l高级感知");
        panel.addCheckBox("开启高级感知", "enhanced_sense", false);

        panel.addLabel("§a血液感知");
        panel.addDoubleEditBox("感知半径", "sense_bleeding_radius", 64.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("持续时长(tick)", "sense_bleeding_lifespan", 400, 1, Integer.MAX_VALUE);

        panel.addLabel("§a方块感知");
        panel.addDoubleEditBox("感知半径", "sense_block_radius", 16.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("持续时长(tick)", "sense_block_lifespan", 100, 1, Integer.MAX_VALUE);

        panel.addLabel("§a普通枪声");
        panel.addDoubleEditBox("感知半径", "sense_gun_shot_radius", 64.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("持续时长(tick)", "sense_gun_shot_lifespan", 400, 1, Integer.MAX_VALUE);

        panel.addLabel("§a消音枪声");
        panel.addDoubleEditBox("感知半径", "sense_gun_shot_silenced_radius", 16.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("持续时长(tick)", "sense_gun_shot_silenced_lifespan", 100, 1, Integer.MAX_VALUE);
    }

    private void initModCompatTab(SimpleSettingsPanel panel) {
        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            panel.addLabel("§6§l火枪模组联动");
        } else {
            panel.addLabel("§8§l火枪模组联动【未安装】");
        }
        panel.addDoubleEditBox("子弹伤害修正比", "musket_mod_gun_damage_modify", 0.5, 0.0, Double.MAX_VALUE);

        if (ZGRDiplomacyCenter.GUARD_VILLAGERS_DIPLOMAT.isLoaded()) {
            panel.addLabel("§6§l警卫村民模组联动");
        } else {
            panel.addLabel("§8§l警卫村民模组联动【未安装】");
        }
        panel.addCheckBox("允许感染警卫继续使用武器", "can_zombie_guard_continue_use_weapons", false);
    }
}
