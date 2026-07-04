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
        ConfigTab zombieTab = new ConfigTab("僵尸设置", this::initializeZombieTab);
        this.tabs.add(zombieTab);

        ConfigTab elseTab = new ConfigTab("其他设置", this::initializeElseTab);
        this.tabs.add(elseTab);
    }

    private void initializeZombieTab(SimpleSettingsPanel panel) {
        panel.addDoubleEditBox("天数", "day", 1.0, 1.0, Double.MAX_VALUE);
        panel.addCallbackabeScreen("僵尸属性", this, "zombie_property",
                (screen, callback) -> new ZombiePropertyScreen("", this.localJson.getAsJsonObject("zombie_property"),
                        callback, screen));
        panel.addCallbackabeScreen("僵尸种类", this, "zombie_spawn_chooser",
                (screen, callback) -> new ZombieSpawnChooserScreen("", this.localJson.getAsJsonObject("zombie_spawn_chooser"),
                        callback, screen));

    }

    private void initializeElseTab(SimpleSettingsPanel panel) {
        if (ZGRDiplomacyCenter.ENHANCED_CELERESTIALS_DIPLOMAT.isLoaded()) {
            panel.addLabel("§6§l月亮事件联动");
        } else {
            panel.addLabel("§8§l月亮事件联动【未安装】");
        }
        panel.addDoubleEditBox("覆盖版血月概率", "blood_moon_chance", 0.0, 0.0, 1.0);
    }
}
