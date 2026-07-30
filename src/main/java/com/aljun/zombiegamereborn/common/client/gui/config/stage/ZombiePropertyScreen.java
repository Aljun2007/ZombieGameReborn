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
import net.minecraft.network.chat.Component;
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
        this.tabs.add(new ConfigTab(Component.translatable("gui.zombiegamereborn.zombieproperty.tab.attributes"), this::initAttributesTab));
        this.tabs.add(new ConfigTab(Component.translatable("gui.zombiegamereborn.zombieproperty.tab.abilities"), this::initAbilityTab));
        this.tabs.add(new ConfigTab(Component.translatable("gui.zombiegamereborn.zombieproperty.tab.volume"), this::initVolumeTab));
        this.tabs.add(new ConfigTab(Component.translatable("gui.zombiegamereborn.zombieproperty.tab.sense"), this::initSenseTab));
        this.tabs.add(new ConfigTab(Component.translatable("gui.zombiegamereborn.zombieproperty.tab.mod_compat"), this::initModCompatTab));

    }

    private void initAttributesTab(SimpleSettingsPanel panel) {
        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.attributes");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.movement_speed_modify", "movement_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.attack_damage_modify", "attack_damage_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.max_health", "max_health", 20.0, 1.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.armor", "armor", 2.0, 0.0, 30.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.armor_toughness", "armor_toughness", 0.0, 0.0, 20.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.knockback_resistance", "knockback_resistance", 0.0, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.mining_speed_modify", "mining_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.equipment_quality_mean_offset", "equipment_quality_mean_offset", 0.0, 0.0, 128.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.equipment_probability_factor", "equipment_probability_factor", 1.0, 0.0, 128.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.equipment_enchantment_factor", "equipment_enchantment_factor", 1.0, 0.0, 128.0);

    }

    private void initAbilityTab(SimpleSettingsPanel panel) {
        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.abilities");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.can_swim_probability", "can_swim_probability", 0.0, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.zombie_swim_speed_modify", "zombie_swim_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.drowned_swim_speed_modify", "drowned_swim_speed_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.do_swimming_zombie_convert", "do_swimming_zombie_convert", false);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.can_jump_attack", "can_jump_attack", false);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.can_throw_tnt", "can_throw_tnt", false);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.sun_immunity_probability", "sun_immunity_probability", 0.0d, 0.0, 1.0);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.flee_sun", "flee_sun", false);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.block_stab_immune_probability", "block_stab_immune_probability", 0.0d, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.ladder_climb_probability", "ladder_climb_probability", 0.0d, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.fire_immunity_probability", "fire_immunity_probability", 0.0d, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.baby_probability", "baby_probability", 0.05d, 0.0d, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.can_pick_up_loot_coefficient", "can_pick_up_loot_coefficient", 0.55d, 0.0, Double.MAX_VALUE);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.break_light_sources", "break_light_sources", false);
    }

    private void initVolumeTab(SimpleSettingsPanel panel) {
        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.volume");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.ambient_volume_modify", "ambient_volume_modify", 1.0d, 0.0d, 1.0d);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.step_volume_modify", "step_volume_modify", 1.0d, 0.0d, 1.0d);

    }

    private void initSenseTab(SimpleSettingsPanel panel) {
        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.vanilla_sense");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.follow_range", "follow_range", 40.0, 0.0, Double.MAX_VALUE);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.follow_must_see", "follow_must_see", true);

        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.piglin_sense");
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.enable_piglin_collision_anger", "enable_piglin_collision_anger", false);
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.piglin_collision_anger_chance","piglin_collision_anger_chance", 1.0, 0.0, 1.0);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.piglin_angry_mode", "piglin_angry_mode", false);
        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.enhanced_sense");
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.enhanced_sense_enabled", "enhanced_sense", false);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.blood_moon_boundless_hunting", "blood_moon_boundless_hunting", false);
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.boundless_hunting", "boundless_hunting", false);

        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.bleeding_sense");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.sense_radius", "sense_bleeding_radius", 64.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("gui.zombiegamereborn.zombieproperty.sense_lifespan", "sense_bleeding_lifespan", 400, 1, Integer.MAX_VALUE);

        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.block_sense");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.sense_radius", "sense_block_radius", 16.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("gui.zombiegamereborn.zombieproperty.sense_lifespan", "sense_block_lifespan", 100, 1, Integer.MAX_VALUE);

        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.gun_shot_sense");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.sense_radius", "sense_gun_shot_radius", 64.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("gui.zombiegamereborn.zombieproperty.sense_lifespan", "sense_gun_shot_lifespan", 400, 1, Integer.MAX_VALUE);

        panel.addLabel("gui.zombiegamereborn.zombieproperty.section.gun_shot_silenced_sense");
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.sense_radius", "sense_gun_shot_silenced_radius", 16.0, 0.0, Double.MAX_VALUE);
        panel.addIntEditBox("gui.zombiegamereborn.zombieproperty.sense_lifespan", "sense_gun_shot_silenced_lifespan", 100, 1, Integer.MAX_VALUE);
    }

    private void initModCompatTab(SimpleSettingsPanel panel) {
        if (ZGRDiplomacyCenter.MUSKETMOD_DIPLOMAT.isLoaded()) {
            panel.addLabel("gui.zombiegamereborn.zombieproperty.section.mod_compat_musket_loaded");
        } else {
            panel.addLabel("gui.zombiegamereborn.zombieproperty.section.mod_compat_musket_unloaded");
        }
        panel.addDoubleEditBox("gui.zombiegamereborn.zombieproperty.musket_damage_modify", "musket_mod_gun_damage_modify", 0.5, 0.0, Double.MAX_VALUE);

        if (ZGRDiplomacyCenter.GUARD_VILLAGERS_DIPLOMAT.isLoaded()) {
            panel.addLabel("gui.zombiegamereborn.zombieproperty.section.mod_compat_guard_loaded");
        } else {
            panel.addLabel("gui.zombiegamereborn.zombieproperty.section.mod_compat_guard_unloaded");
        }
        panel.addCheckBox("gui.zombiegamereborn.zombieproperty.can_guard_use_weapons", "can_zombie_guard_continue_use_weapons", false);
    }
}
