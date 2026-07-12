package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.aljun.zombiegamereborn.register.ZGRRegistries;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
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
        ConfigTab tab = new ConfigTab(Component.translatable("gui.zombiegamereborn.wrappedzombietype.tab.settings"), this::initializeExclusiveTab);
        this.tabs.add(tab);
    }

    private void initializeExclusiveTab(SimpleSettingsPanel panel) {

        List<ResourceLocation> types = ZGRRegistries.ZOMBIE_TYPE.get().getKeys().stream().toList();

        panel.addListChooseScreen("gui.zombiegamereborn.wrappedzombietype.zombie_type", "zombie_type", this, types, id -> I18n.get("zombie_type." + id.getNamespace() + "." + id.getPath()), () -> this.localJson.has("zombie_type") ? this.localJson.get("zombie_type").getAsString() : "");

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
        panel.addDoubleEditBox("gui.zombiegamereborn.wrappedzombietype.weight", "chance", 1.0, 0.0, Double.MAX_VALUE);
        panel.addFakeEnumCycleButton(
                "gui.zombiegamereborn.wrappedzombietype.spawn_type",
                "type",
                ZombieSpawnChooser.SpawnType.values(),
                ZombieSpawnChooser.SpawnType.NORMAL,
                type -> I18n.get("gui.zombiegamereborn.spawntype." + type.name ),
                type -> new JsonPrimitive(type.name),
                json -> ZombieSpawnChooser.SpawnType.byName(((JsonPrimitive) json).getAsString())
        );

    }
}
