package com.aljun.zombiegamereborn.common.client.gui.game;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.Environment;
import org.lwjgl.glfw.GLFW;

public class ZGRTestScreen extends Screen {

    // 可选：自定义背景图（如果没有可以不使用）
//    private static final ResourceLocation BACKGROUND =
//             ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "textures/gui/test_gui.png");

    private int clickCount = 0;
    private StringWidget counterText;  // 计数器文字组件
    private Button testButton;         // 按钮组件

    public ZGRTestScreen() {
        super(Component.literal("这是我的GUI界面"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. 标题文字
        StringWidget titleText = new StringWidget(
                centerX - 50, centerY - 60, 100, 20,
                Component.literal("§l§6欢迎使用原版GUI"),
                this.font
        );
        titleText.setColor(0xFFFFFF);  // 白色
        this.addRenderableWidget(titleText);

        // 2. 计数器文字（保存为成员变量，方便更新）
        this.counterText = new StringWidget(
                centerX - 50, centerY - 30, 100, 20,
                Component.literal("按钮点击次数: 0"),
                this.font
        );
        this.counterText.setColor(0xFFFFFF);
        this.addRenderableWidget(this.counterText);

        // 3. 按钮（一行搞定，文字和点击回调都在这里）
        this.testButton = this.addRenderableWidget(Button.builder(
                                Component.literal("点我试试"),
                                button -> {
                                    clickCount++;
                                    // 更新计数器文字
                                    counterText.setMessage(Component.literal("按钮点击次数: " + clickCount));
                                }
                        )
                        .bounds(centerX - 50, centerY, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // 可选：按 ESC 关闭界面
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {  // ESC 键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}