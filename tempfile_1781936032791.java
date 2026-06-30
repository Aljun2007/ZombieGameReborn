NEW_FILE_CODE
package com.aljun.zombiegamereborn.common.client.gui.list;

import com.aljun.zombiegamereborn.common.client.gui.config.AbstractConfigScreen;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 列表示例屏幕 - 演示如何使用 ListEditScreen
 */
@OnlyIn(Dist.CLIENT)
public class ExampleListScreen extends AbstractConfigScreen {

    private List<String> exampleList = new ArrayList<>(Arrays.asList(
            "第一项",
            "第二项",
            "第三项",
            "第四项",
            "第五项"
    ));

    public ExampleListScreen(JsonObject initSettings) {
        super("列表示例", initSettings);
    }

    @Override
    protected void loadDefaultSettings() {
    }

    @Override
    protected void initializeTabs() {
    }

    @Override
    protected void init() {
        super.init();
        
        // 打开列表编辑屏幕
        Minecraft.getInstance().setScreen(new ListEditScreen<>(
                "编辑示例列表",
                exampleList,
                savedList -> {
                    exampleList = savedList;
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§a已保存列表，共 " + savedList.size() + " 项"), 
                                false
                        );
                    }
                },
                item -> "§7[§f" + item + "§7]",
                new ListEditScreen.StringItemEditor()
        ));
    }

    @Override
    protected void handleOnClose() {
        super.handleOnClose();
    }
}
