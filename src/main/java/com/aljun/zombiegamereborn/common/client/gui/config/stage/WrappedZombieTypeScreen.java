package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListChooseScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.register.ZGRRegistries;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
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

        List<ResourceLocation> types = ZGRRegistries.ZOMBIE_TYPE.get().getKeys().stream().toList();

        panel.addListChooseScreen("僵尸类型", "zombie_type", this, types, ResourceLocation::toString, () -> this.localJson.has("zombie_type") ? this.localJson.get("zombie_type").getAsString() : "");

        panel.addLabel("");

        panel.setEditBoxSuggestionProvider("zombie_type", input -> {
            if (ZGRRegistries.ZOMBIE_TYPE == null || ZGRRegistries.ZOMBIE_TYPE.get() == null) {
                return List.of();
            }
            return ZGRRegistries.ZOMBIE_TYPE.get().getKeys().stream()
                    .map(ResourceLocation::toString)
                    .filter(id -> id.contains(input))
                    .sorted()
                    .toList();
        });
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
