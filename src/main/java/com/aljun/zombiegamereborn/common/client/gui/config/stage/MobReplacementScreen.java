package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListEditScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.MobReplacement;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MobReplacementScreen extends AbstractBranchConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(MobReplacement.MobElement.class, new MobReplacement.MobElementAdapter())
            .create();

    protected MobReplacementScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree(MobReplacement.getDefault()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab(Component.translatable("gui.zombiegamereborn.mobreplacement.settings"), this::initializeTab);
        this.tabs.add(tab);
    }

    private void initializeTab(SimpleSettingsPanel panel) {
        List<JsonElement> mobs = this.localJson.has("mobs") && this.localJson.get("mobs").isJsonArray()
                ? this.localJson.getAsJsonArray("mobs").asList()
                : java.util.Collections.emptyList();

        panel.addCallbackabeScreen(
                "gui.zombiegamereborn.mobreplacement.mobs",
                this,
                "mobs",
                (parentScreen, saveCallback) -> {
                    ListEditScreen<JsonElement> screen = new ListEditScreen<>(
                            "gui.zombiegamereborn.mobreplacement.edit_list_title",
                            parentScreen, mobs,

                            (updatedList) -> {
                                JsonArray jsonArray = GSON.toJsonTree(updatedList).getAsJsonArray();
                                saveCallback.accept(jsonArray);
                            },

                            (jsonElement) -> {
                                MobReplacement.MobElement element = GSON.fromJson(jsonElement, MobReplacement.MobElement.class);
                                String entityName = I18n.get("entity." + element.mobID().getNamespace() + "." + element.mobID().getPath());
                                String actionName = I18n.get("gui.zombiegamereborn.mobreplacement.action." + element.type().name().toLowerCase());
                                return entityName + " [" + actionName + "]";
                            },

                            (lastScreen1, jsonElement1, itemSaveCallback) -> {
                                MobReplacement.MobElement element = GSON.fromJson(jsonElement1, MobReplacement.MobElement.class);
                                JsonObject initialData = GSON.toJsonTree(element).getAsJsonObject();
                                Minecraft.getInstance().setScreen(new MobElementScreen(
                                        "gui.zombiegamereborn.mobreplacement.edit_title",
                                        initialData,
                                        itemSaveCallback,
                                        lastScreen1
                                ));
                            },

                            () -> GSON.toJsonTree(new MobReplacement.MobElement(
                                    net.minecraft.resources.ResourceLocation.parse("minecraft:zombie"),
                                    MobReplacement.ReplaceableType.REPLACE))
                    );
                    screen.setComparator((o1, o2) -> {
                        String id1 = ((JsonObject) o1).get("mob_id").getAsString();
                        String id2 = ((JsonObject) o2).get("mob_id").getAsString();
                        return id1.compareTo(id2) > 0;
                    });
                    return screen;
                }
        );
    }
}
