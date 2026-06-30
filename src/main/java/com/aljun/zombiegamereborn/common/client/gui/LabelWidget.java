package com.aljun.zombiegamereborn.common.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 标签组件 - 用于显示居中的标题或分隔线
 */
@OnlyIn(Dist.CLIENT)
public class LabelWidget extends AbstractWidget {

    private final Font font = Minecraft.getInstance().font;
    private final int textColor;

    public LabelWidget(int x, int y, int width, int height, String text, int textColor) {
        super(x, y, width, height, Component.literal(text));
        this.textColor = textColor;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 计算居中位置
        String displayText = this.getMessage().getString();
        int textWidth = font.width(displayText);
        
        // X 坐标：相对于组件起始位置的居中
        int textX = this.getX() + (this.width - textWidth) / 2;
        
        // Y 坐标：垂直居中（考虑字体基线）
        int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
        
        // 绘制文本
        guiGraphics.drawString(font, displayText, textX, textY, this.textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 标签不需要叙述
    }
}

