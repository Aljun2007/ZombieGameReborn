package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.config.ZGRConfigFileManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LocalDefaultGamePropertyScreen extends AbstractGamePropertyScreen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson LOCAL_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public LocalDefaultGamePropertyScreen(String title, JsonObject initSettings) {
        super(title, initSettings);
    }

    @Override
    protected List<ButtonInfo> getCustomButtons() {
        List<ButtonInfo> buttons = new ArrayList<>(super.getCustomButtons());
        buttons.add(new ButtonInfo("gui.zombiegamereborn.core.save", this::saveToLocal));
        return buttons;
    }

    private void saveToLocal() {
        GameProperty gameProperty = GSON.fromJson(this.localJson, GameProperty.class);
        saveLocalDefaultConfig(gameProperty);
        hasUnsavedChanges = false;

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.zombiegamereborn.core.save_success"), false
            );
        }
    }

    private void saveLocalDefaultConfig(GameProperty gameProperty) {
        try {
            Path configPath = ZGRConfigFileManager.getClientConfigDirectory().resolve("default_game_property.json");
            Files.createDirectories(configPath.getParent());

            JsonObject jsonObject = gameProperty.toJsonObject();
            String jsonString = LOCAL_GSON.toJson(jsonObject);

            try (Writer writer = Files.newBufferedWriter(configPath)) {
                writer.write(jsonString);
            }
        } catch (Exception e) {
            ZGRConfigFileManager.LOGGER.error("保存本地默认配置文件失败", e);
        }
    }

    @Override
    protected void handleSaveAndClose() {
        saveToLocal();
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
    }
}
