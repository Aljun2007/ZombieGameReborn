package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListEditScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ZombieSpawnChooserScreen extends AbstractBranchConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ZombieSpawnChooser.class, new ZombieSpawnChooser.ZombieSpawnChooserAdapter())
            .registerTypeAdapter(ZombieSpawnChooser.WrappedZombieType.class, new ZombieSpawnChooser.WrappedZombieType.WrappedZombieTypeAdapter())
            .create();

    protected ZombieSpawnChooserScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree(ZombieSpawnChooser.getDefault()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab(Component.translatable("gui.zombiegamereborn.zombiespawnchooser.settings"), this::initializeExclusiveTab);
        this.tabs.add(tab);
    }

    private void initializeExclusiveTab(SimpleSettingsPanel panel) {
        List<JsonElement> zombieTypes = this.localJson.has("zombie_types") && this.localJson.get("zombie_types").isJsonArray()
                ? this.localJson.getAsJsonArray("zombie_types").asList()
                : java.util.Collections.emptyList();
        panel.addCallbackabeScreen(
                "gui.zombiegamereborn.zombiespawnchooser.zombie_types", 
                this, 
                "zombie_types",
                (parentScreen, saveCallback) -> {
                    return new ListEditScreen<JsonElement>(
                            "gui.zombiegamereborn.zombiespawnchooser.edit_list_title",
                            parentScreen, zombieTypes,

                            (updatedList) -> {
                                JsonArray jsonArray = GSON.toJsonTree(updatedList).getAsJsonArray();
                                saveCallback.accept(jsonArray);
                            },

                            (jsonElement) -> {
                                ZombieSpawnChooser.WrappedZombieType wrappedType = GSON.fromJson(jsonElement, ZombieSpawnChooser.WrappedZombieType.class);
                                String name = Component.translatable(
                                        "zombie_type." + wrappedType.zombieType.getId().getNamespace() + "." + wrappedType.zombieType.getId().getPath()
                                ).getString();
                                return Component.translatable("gui.zombiegamereborn.wrappedzombietype.display_format",
                                        name, wrappedType.chance, wrappedType.type.name).getString();
                            },

                            (lastScreen1, jsonElement1, itemSaveCallback) -> {
                                ZombieSpawnChooser.WrappedZombieType wrappedType = 
                                        GSON.fromJson(jsonElement1, ZombieSpawnChooser.WrappedZombieType.class);
                                
                                JsonObject initialData = GSON.toJsonTree(wrappedType).getAsJsonObject();

                                Minecraft.getInstance().setScreen(new WrappedZombieTypeScreen(
                                        "gui.zombiegamereborn.zombiespawnchooser.edit_type_title",
                                        initialData,
                                        itemSaveCallback,
                                        lastScreen1
                                ));
                            },

                            () -> GSON.toJsonTree(new ZombieSpawnChooser.WrappedZombieType())
                    );
                }
        );
    }
}
