package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public class WrappedZombieTypeScreen extends AbstractBranchConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ZombieSpawnChooser.WrappedZombieType.class, new ZombieSpawnChooser.WrappedZombieType.WrappedZombieTypeAdapter())
            .create();

    protected WrappedZombieTypeScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree(new ZombieSpawnChooser.WrappedZombieType()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab("设置", this::initializeExclusiveTab);
        this.tabs.add(tab);
    }

    private void initializeExclusiveTab(SimpleSettingsPanel panel) {
        panel.addEditBox("僵尸类型", "zombie_type", ZGRZombieTypes.DUMMY.getId().toString());
        panel.addDoubleEditBox("权重", "chance", 1.0, 0.0, Double.MAX_VALUE);
        panel.addEnumCycleButton(
                "生成类型",
                "type",
                ZombieSpawnChooser.SpawnType.values(),
                ZombieSpawnChooser.SpawnType.NORMAL,
                type -> ((ZombieSpawnChooser.SpawnType) type).name,
                e -> new JsonPrimitive(((ZombieSpawnChooser.SpawnType) e).name),
                json -> ZombieSpawnChooser.SpawnType.byName(json.getAsString())
        );
    }
}
