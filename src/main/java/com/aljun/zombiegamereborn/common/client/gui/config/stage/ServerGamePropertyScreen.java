package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.GamePropertyUploadPacket;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ServerGamePropertyScreen extends AbstractGamePropertyScreen {

    public ServerGamePropertyScreen(String title, JsonObject initSettings) {
        super(title, initSettings);
    }

    @Override
    protected List<ButtonInfo> getCustomButtons() {
        List<ButtonInfo> buttons = new ArrayList<>(super.getCustomButtons());
        buttons.add(new ButtonInfo("gui.zombiegamereborn.gameproperty.save_sync", this::syncToServer, () -> hasUnsavedChanges));
        return buttons;
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
