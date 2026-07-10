package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListEditScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.GamePropertyUploadPacket;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GamePropertyScreen extends AbstractConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .registerTypeAdapter(StageProperty.class, new StageProperty.StagePropertyAdapter())
            .create();
    
    public GamePropertyScreen(String title, JsonObject initSettings) {
        super(title, initSettings);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree( GameProperty.empty()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab ruleTab = new ConfigTab(Component.translatable("gui.zombiegamereborn.gameproperty.tab.rules"), this::initializeRuleTab);
        ConfigTab stageTab = new ConfigTab(Component.translatable("gui.zombiegamereborn.gameproperty.tab.stages"), this::initializeStageTab);
        this.tabs.add(ruleTab);
        this.tabs.add(stageTab);
    }

    private void initializeRuleTab(SimpleSettingsPanel panel) {
        panel.addCheckBox("gui.zombiegamereborn.gameproperty.can_break", "can_zombie_break_block", true);
        panel.addCheckBox("gui.zombiegamereborn.gameproperty.can_place", "can_zombie_place_block", true);
        
        panel.setOnValueChanged((key, value) -> {
            if (!isInitializing) {
                localJson.add(key, value);
                hasUnsavedChanges = true;
                hasInteracted = true;
            }
        });
    }

    @Override
    public void applyValue(String key, JsonElement jsonObject) {
        super.applyValue(key, jsonObject);
        hasUnsavedChanges = true;
        hasInteracted = true;
    }

    private void initializeStageTab(SimpleSettingsPanel panel) {
        panel.addCallbackabeScreen(
                "gui.zombiegamereborn.gameproperty.stage_list",
                this,
                "stage_properties",
                (parentScreen, saveCallback) -> {
                    ListEditScreen<JsonElement> screen = new ListEditScreen<JsonElement>(
                            "gui.zombiegamereborn.gameproperty.stage_list_edit_title",
                            parentScreen,
                            this.localJson.has("stage_properties") && this.localJson.get("stage_properties").isJsonArray() 
                                ? this.localJson.getAsJsonArray("stage_properties").asList()
                                : java.util.Collections.emptyList(),

                            (updatedList) -> {
                                JsonArray jsonArray = GSON.toJsonTree(updatedList).getAsJsonArray();
                                saveCallback.accept((JsonElement)jsonArray);
                            },

                            (jsonElement) -> I18n.get("gui.zombiegamereborn.gameproperty.stage_day_prefix") + GSON.fromJson(jsonElement, StageProperty.class).day + I18n.get("gui.zombiegamereborn.gameproperty.stage_day_suffix"),

                            (lastScreen1, jsonElement1, itemSaveCallback) -> {
                                StageProperty stageProperty =
                                        GSON.fromJson(jsonElement1, StageProperty.class);
                                
                                JsonObject initialData = GSON.toJsonTree(stageProperty).getAsJsonObject();

                                Minecraft.getInstance().setScreen(new StagePropertyScreen(
                                        "gui.zombiegamereborn.gameproperty.stage_edit_title",
                                        initialData,
                                        (updatedElement) -> {
                                            itemSaveCallback.accept(updatedElement);
                                            hasUnsavedChanges = true;
                                            hasInteracted = true;
                                        },
                                        lastScreen1
                                ));
                            },
                            
                            () -> GSON.toJsonTree(new StageProperty())
                    );
                    screen.setComparator((o1, o2) -> {
                        return ((JsonObject) o1).get("day").getAsDouble() > ((JsonObject) o2).get("day").getAsDouble();
                    });
                    return screen;
                }
        );
    }

    @Override
    protected List<ButtonInfo> getCustomButtons() {
        List<ButtonInfo> buttons = new ArrayList<>();
        buttons.add(new ButtonInfo("gui.zombiegamereborn.gameproperty.import_export", this::importScreen));
        buttons.add(new ButtonInfo("gui.zombiegamereborn.gameproperty.save_sync", this::syncToServer, () -> hasUnsavedChanges));
        return buttons;
    }


    private void importScreen() {

    }

    private void syncToServer() {
        checkSyncToServer();
    }

    private void checkSyncToServer() {
        if (hasUnsavedChanges) {
            sendConfigToServer(localJson);
            hasUnsavedChanges = false;
            
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.zombiegamereborn.gameproperty.sync_success"), false
                );
            }
        }
    }

    private void sendConfigToServer(JsonObject config) {
        ZGRNetwork.sendToServer(new GamePropertyUploadPacket(config));
    }

    @Override
    protected void handleSaveAndClose() {
        checkSyncToServer();
    }

    @Override
    protected void handleOnClose() {
        if (!hasInteracted) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.core.no_changes"), false
                );
            }
        } else if (hasUnsavedChanges) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.gameproperty.unsaved_warning"), false
                );
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.gameproperty.sync_hint"), false
                );
            }
        } else {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.gameproperty.safe_exit"), false
                );
            }
        }
    }
}
