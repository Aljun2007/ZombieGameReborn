package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.StageProperty;
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
public class StagePropertyScreen extends AbstractBranchConfigScreen {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(StageProperty.class, new StageProperty.StagePropertyAdapter())
            .create();

    protected StagePropertyScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree(new StageProperty()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab zombieTab = new ConfigTab(Component.translatable("gui.zombiegamereborn.stageproperty.tab.zombie"), this::initializeZombieTab);
        this.tabs.add(zombieTab);

        ConfigTab elseTab = new ConfigTab(Component.translatable("gui.zombiegamereborn.stageproperty.tab.other"), this::initializeElseTab);
        this.tabs.add(elseTab);
    }

    private void initializeZombieTab(SimpleSettingsPanel panel) {
        panel.addDoubleEditBox("gui.zombiegamereborn.stageproperty.day", "day", 1.0, 1.0, Double.MAX_VALUE);
        panel.addCallbackabeScreen("gui.zombiegamereborn.stageproperty.zombie_property", this, "zombie_property",
                (screen, callback) -> new ZombiePropertyScreen("", this.localJson.getAsJsonObject("zombie_property"),
                        callback, screen));
        panel.addCallbackabeScreen("gui.zombiegamereborn.stageproperty.zombie_spawn", this, "zombie_spawn_chooser",
                (screen, callback) -> new ZombieSpawnChooserScreen("", this.localJson.getAsJsonObject("zombie_spawn_chooser"),
                        callback, screen));
        panel.addDoubleEditBox("gui.zombiegamereborn.stageproperty.replace_chance", "replace_chance", 0.0, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.stageproperty.remove_chance", "remove_chance", 0.0, 0.0, 1.0);
        panel.addDoubleEditBox("gui.zombiegamereborn.stageproperty.zombie_count_modify","zombie_count_modify", 1.0, 0.0, Double.MAX_VALUE);
        panel.addCheckBox("gui.zombiegamereborn.stageproperty.holy_cleansing","holy_cleansing", true);

    }

    private void initializeElseTab(SimpleSettingsPanel panel) {
        if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isLoaded()) {
            panel.addLabel("gui.zombiegamereborn.stageproperty.moon_event_loaded");
        } else {
            panel.addLabel("gui.zombiegamereborn.stageproperty.moon_event_unloaded");
        }
        panel.addDoubleEditBox("gui.zombiegamereborn.stageproperty.blood_moon_chance", "blood_moon_chance", 0.0, 0.0, 1.0);
    }
}
