package com.aljun.zombiegamereborn.common.client.gui.config.client;

import com.aljun.zombiegamereborn.common.client.config.ClientConfig;
import com.aljun.zombiegamereborn.common.client.config.ClientConfigManager;
import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.client.gui.config.stage.LocalDefaultGamePropertyScreen;
import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientConfigScreen extends AbstractConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(ClientConfig.class, new ClientConfig.ClientConfigAdapter())
            .create();

    public ClientConfigScreen(String title, JsonObject initSettings) {
        super(title, initSettings);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = GSON.toJsonTree(ClientConfig.defaultConfig()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab(Component.translatable("gui.zombiegamereborn.clientconfig.settings"), this::initializeTab);
        this.tabs.add(tab);
    }

    private void initializeTab(SimpleSettingsPanel panel) {
        panel.addCheckBox("gui.zombiegamereborn.clientconfig.time_broadcast", "time_broadcast_enabled", true);
        panel.addCheckBox("gui.zombiegamereborn.clientconfig.time_alarm", "time_alarm_enabled", true);
        panel.addCheckBox("gui.zombiegamereborn.clientconfig.login_message", "login_message_enabled", true);
        panel.addSimpleButton(
                "gui.zombiegamereborn.clientconfig.edit_local_default",
                () -> {
                    GameProperty defaultProp = GameProperty.getGlobalDefault();
                    JsonObject initData = defaultProp.toJsonObject();
                    Minecraft.getInstance().setScreen(new LocalDefaultGamePropertyScreen(
                            "编辑本地默认配置", initData));
                },
                () -> "§e✎ 编辑"
        );

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

    @Override
    protected List<ButtonInfo> getCustomButtons() {
        List<ButtonInfo> buttons = new ArrayList<>();
        buttons.add(new ButtonInfo("gui.zombiegamereborn.core.save", this::onSave));
        return buttons;
    }

    private void onSave() {
        ClientConfig config = ClientConfig.fromJsonObject(localJson);
        ClientConfigManager.save(config);
        hasUnsavedChanges = false;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.zombiegamereborn.core.save_success"), false
            );
        }
    }

    @Override
    public void onClose() {
        if (!hasInteracted) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.core.no_changes"), false
                );
            }
        } else if (hasUnsavedChanges) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.core.unsaved_exit"), false
                );
            }
        } else {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.clientconfig.save_success"), false
                );
            }
        }
        Minecraft.getInstance().setScreen(null);
    }
}
