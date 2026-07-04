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
        ConfigTab ruleTab = new ConfigTab("游戏规则", this::initializeRuleTab);
        ConfigTab stageTab = new ConfigTab("阶段设置", this::initializeStageTab);
        this.tabs.add(ruleTab);
        this.tabs.add(stageTab);
    }

    private void initializeRuleTab(SimpleSettingsPanel panel) {
        panel.addCheckBox("允许僵尸挖掘", "can_zombie_break_block", true);
        panel.addCheckBox("允许僵尸建造", "can_zombie_place_block", true);
        
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
                "阶段列表",
                this,
                "stage_properties",
                (parentScreen, saveCallback) -> {
                    ListEditScreen<JsonElement> screen = new ListEditScreen<JsonElement>(
                            "编辑阶段列表",
                            parentScreen,
                            this.localJson.has("stage_properties") && this.localJson.get("stage_properties").isJsonArray() 
                                ? this.localJson.getAsJsonArray("stage_properties").asList()
                                : java.util.Collections.emptyList(),

                            (updatedList) -> {
                                JsonArray jsonArray = GSON.toJsonTree(updatedList).getAsJsonArray();
                                saveCallback.accept((JsonElement)jsonArray);
                            },

                            (jsonElement) -> "第" + GSON.fromJson(jsonElement, StageProperty.class).day + "天",

                            (lastScreen1, jsonElement1, itemSaveCallback) -> {
                                StageProperty stageProperty =
                                        GSON.fromJson(jsonElement1, StageProperty.class);
                                
                                JsonObject initialData = GSON.toJsonTree(stageProperty).getAsJsonObject();

                                Minecraft.getInstance().setScreen(new StagePropertyScreen(
                                        "编辑阶段设置",
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
        buttons.add(new ButtonInfo("§e导入/导出", this::importScreen));
        buttons.add(new ButtonInfo("§b✓ 保存", this::syncToServer, () -> hasUnsavedChanges));
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
                    Component.literal("§a配置已同步到服务器"), false
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
                        Component.literal("§7配置没有修改"), false
                );
            }
        } else if (hasUnsavedChanges) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§c§l意外退出，配置未同步到服务器！"), false
                );
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§e提示：点击设置面板中'上传到服务器'按钮"), false
                );
            }
        } else {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§7配置已同步，安全退出"), false
                );
            }
        }
    }
}
