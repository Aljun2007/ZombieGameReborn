package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListChooseScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.MobReplacement;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MobElementScreen extends AbstractBranchConfigScreen {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final List<ResourceLocation> ALL_ENTITY_TYPES = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
            .sorted(Comparator.comparing(ResourceLocation::toString))
            .collect(Collectors.toList());

    protected MobElementScreen(String title, JsonObject initSettings, Consumer<JsonElement> onSaveCallback, Screen lastScreen) {
        super(title, initSettings, onSaveCallback, lastScreen);
    }

    @Override
    protected void loadDefaultSettings() {
        this.localJson = new JsonObject();
        this.localJson.addProperty("mob_id", "minecraft:zombie");
        this.localJson.addProperty("action", "replace");
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab(Component.translatable("gui.zombiegamereborn.mobreplacement.settings"), this::initializeTab);
        this.tabs.add(tab);
    }

    private void initializeTab(SimpleSettingsPanel panel) {
        // 实体类型显示 + 选择
        panel.addTextMonitor(
                "gui.zombiegamereborn.mobreplacement.entity_type",
                () -> {
                    String idStr = localJson.has("mob_id") ? localJson.get("mob_id").getAsString() : "minecraft:zombie";
                    ResourceLocation id = ResourceLocation.parse(idStr);
                    return I18n.get("entity." + id.getNamespace() + "." + id.getPath());
                }
        );
        panel.addCallbackabeScreen(
                "",
                this,
                "mob_id",
                (parent, saveCallback) -> new ListChooseScreen<>(
                        "gui.zombiegamereborn.mobreplacement.select_entity",
                        ALL_ENTITY_TYPES,
                        id -> I18n.get("entity." + id.getNamespace() + "." + id.getPath()),
                        selected -> saveCallback.accept(new JsonPrimitive(selected.toString())),
                        parent
                )
        );

        // 操作选择
        panel.addEnumCycleButton(
                "gui.zombiegamereborn.mobreplacement.action",
                "action",
                MobReplacement.ReplaceableType.values(),
                MobReplacement.ReplaceableType.REPLACE,
                e -> I18n.get("gui.zombiegamereborn.mobreplacement.action." + e.name().toLowerCase())
        );
    }
}
