package com.aljun.zombiegamereborn.common.client.gui.config.stage;

import com.aljun.zombiegamereborn.common.client.gui.config.core.AbstractBranchConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.ListEditScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.core.SimpleSettingsPanel;
import com.aljun.zombiegamereborn.common.config.ZombieSpawnChooser;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
        this.localJson = GSON.toJsonTree(new ZombieSpawnChooser()).getAsJsonObject();
    }

    @Override
    protected void initializeTabs() {
        ConfigTab tab = new ConfigTab("设置", this::initializeExclusiveTab);
        this.tabs.add(tab);
    }

    private void initializeExclusiveTab(SimpleSettingsPanel panel) {
        // 添加嵌套屏幕编辑按钮
        // 点击后会打开 ListEditScreen 来编辑 zombie_types 数组
        List<JsonElement> zombieTypes = this.localJson.has("zombie_types") && this.localJson.get("zombie_types").isJsonArray()
                ? this.localJson.getAsJsonArray("zombie_types").asList()
                : java.util.Collections.emptyList();
        panel.addCallbackabeScreen(
                "僵尸类型", 
                this, 
                "zombie_types",
                (parentScreen, saveCallback) -> {
                    // ========================================
                    // 第一层：列表编辑屏幕（ListEditScreen）
                    // 功能：显示所有 WrappedZombieType 的列表，支持增删改
                    // ========================================
                    return new ListEditScreen<JsonElement>(
                            "编辑僵尸类型列表",
                            parentScreen,zombieTypes
                            // 初始数据：从 localJson 中获取 zombie_types 数组（如果不存在则返回空数组）
                            ,
                            
                            // 【列表保存回调】当用户在列表屏幕点击保存时触发
                            // updatedList: 修改后的 List<JsonElement>
                            (updatedList) -> {
                                // 将 List<JsonElement> 转换为 JsonArray
                                JsonArray jsonArray = GSON.toJsonTree(updatedList).getAsJsonArray();
                                saveCallback.accept(jsonArray);
                            },
                            
                            // 【渲染器】决定列表中每个元素的显示文本
                            // jsonElement: 列表中的一个元素（JsonElement）
                            // 返回值：在列表中显示的文本
                            (jsonElement) -> {
                                 ZombieSpawnChooser.WrappedZombieType wrappedType = GSON.fromJson(jsonElement, ZombieSpawnChooser.WrappedZombieType.class);
                                 return wrappedType.toString();
                            },
                            
                            // 【编辑器工厂】当用户点击列表项的"修改"按钮时，创建详情编辑屏幕
                            // lastScreen1: 当前屏幕（ListEditScreen）
                            // jsonElement1: 要编辑的列表项（JsonElement）
                            // itemSaveCallback: 单项保存回调，接收修改后的 JsonElement
                            (lastScreen1, jsonElement1, itemSaveCallback) -> {
                                // 反序列化当前列表项为 WrappedZombieType 对象
                                ZombieSpawnChooser.WrappedZombieType wrappedType = 
                                        GSON.fromJson(jsonElement1, ZombieSpawnChooser.WrappedZombieType.class);
                                
                                // 将对象转换为 JsonObject 作为初始数据传递给详情屏幕
                                JsonObject initialData = GSON.toJsonTree(wrappedType).getAsJsonObject();
                                
                                // ========================================
                                // 第二层：详情编辑屏幕（WrappedZombieTypeScreen）
                                // 功能：编辑单个 WrappedZombieType 的字段
                                // ========================================
                                Minecraft.getInstance().setScreen(new WrappedZombieTypeScreen(
                                        "编辑僵尸类型",
                                        initialData,
                                        // 直接传递回调，不需要额外的包装
                                        itemSaveCallback,
                                        lastScreen1
                                ));
                            },
                            
                            // 【默认项提供者】点击"新建"按钮时创建默认的 WrappedZombieType
                            () -> GSON.toJsonTree(new ZombieSpawnChooser.WrappedZombieType())
                    );
                }
        );
    }
}
