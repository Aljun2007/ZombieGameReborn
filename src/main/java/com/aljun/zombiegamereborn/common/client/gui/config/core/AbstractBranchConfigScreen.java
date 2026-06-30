package com.aljun.zombiegamereborn.common.client.gui.config.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractBranchConfigScreen extends AbstractConfigScreen implements Callbackable<JsonElement> {
    private final Consumer<JsonElement> onSaveCallback;
    private final Screen lastScreen;

    protected AbstractBranchConfigScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings);
        this.onSaveCallback = onSaveCallback;
        this.lastScreen = lastScreen;
    }

    @Override
    protected List<ButtonInfo> getCustomButtons() {
        List<ButtonInfo> buttons = new ArrayList<>();
        buttons.add(new ButtonInfo("§b✓ 保存", this::onSave));
        return buttons;
    }

    protected void onSave() {
        if (currentTab != null && currentTab.getPanel() != null) {
            JsonObject currentValues = currentTab.getPanel().getCurrentValues();
            for (String key : currentValues.keySet()) {
                this.localJson.add(key, currentValues.get(key));
            }
        }
        this.backAndSave(this.localJson);
    }

    @Override
    public void onClose() {
        if (this.getLastScreen() !=null){
            this.backAndCancel();
        } else {
            super.onClose();
        }
    }

    @Override
    public Screen getLastScreen() {
        return this.lastScreen;
    }

    @Override
    public Consumer<JsonElement> getCallBack() {
        return this.onSaveCallback;
    }
}
