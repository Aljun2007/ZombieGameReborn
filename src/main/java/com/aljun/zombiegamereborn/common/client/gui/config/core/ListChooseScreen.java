package com.aljun.zombiegamereborn.common.client.gui.config.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * 列表选择屏幕 - 从注册的列表中单项选择
 * <p>
 * 功能特性：
 * 1. 滑动列表展示所有可选项
 * 2. 点击选中并高亮
 * 3. 确认选择后通过 Callback 返回
 * 4. 双击直接选中确认
 */
@OnlyIn(Dist.CLIENT)
public class ListChooseScreen<T> extends Screen {

    private static final int ITEM_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int MIN_SCROLLBAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 40;
    private static final int LIST_START_Y = 35;

    private final List<T> items;
    private final ItemRenderer<T> itemRenderer;
    private final Consumer<T> onSelectCallback;
    private final Screen lastScreen;

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartMouseY = 0;
    private int dragStartScrollOffset = 0;
    private int currentMouseY = 0;

    private Button confirmButton;
    private Button cancelButton;

    /**
     * 构造函数
     *
     * @param title           屏幕标题
     * @param items           可选项列表
     * @param itemRenderer    项渲染器
     * @param onSelectCallback 选中确认时的回调，返回选中的项
     * @param lastScreen      上一个屏幕
     */
    public ListChooseScreen(
            String title,
            List<T> items,
            ItemRenderer<T> itemRenderer,
            Consumer<T> onSelectCallback,
            Screen lastScreen
    ) {
        super(Component.literal(title));
        this.items = items;
        this.itemRenderer = itemRenderer;
        this.onSelectCallback = onSelectCallback;
        this.lastScreen = lastScreen;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseY = mouseY;

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title.getString(),
                10, 20, 0xFFFFFF);

        renderListArea(guiGraphics, mouseX, mouseY, partialTick);
        renderScrollbar(guiGraphics, mouseX, mouseY, partialTick);

        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            String selectedText = "§7已选中: §f" + itemRenderer.render(items.get(selectedIndex));
            guiGraphics.drawString(this.font, selectedText, 10, this.height - BOTTOM_BAR_HEIGHT - 15, 0xAAAAAA);
        }
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null) {
            this.width = this.minecraft.getWindow().getGuiScaledWidth();
            this.height = this.minecraft.getWindow().getGuiScaledHeight();
        }

        int buttonY = this.height - BOTTOM_BAR_HEIGHT + 10;

        if (this.cancelButton != null) {
            this.removeWidget(this.cancelButton);
            this.removeWidget(this.confirmButton);
        }

        int padding = 10;
        this.cancelButton = Button.builder(
                Component.literal("§7✗ 取消"),
                btn -> onCancel()
        ).bounds(padding, buttonY, 55, 20).build();

        int rightPadding = 10;
        this.confirmButton = Button.builder(
                Component.literal("§a✓ 选择"),
                btn -> onConfirm()
        ).bounds(this.width - rightPadding - 55, buttonY, 55, 20).build();

        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(confirmButton);

        updateMaxScrollOffset();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        if (cancelButton != null) {
            init();
        }
        updateMaxScrollOffset();
    }

    private void updateMaxScrollOffset() {
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int contentHeight = items.size() * ITEM_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - listHeight);
    }

    private void onCancel() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    private void onConfirm() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            onSelectCallback.accept(items.get(selectedIndex));
            Minecraft.getInstance().setScreen(lastScreen);
        }
    }

    private void renderListArea(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listWidth = this.width - 40;
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;

        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + listHeight, 0x30000000);
        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + 1, 0xFF666666);
        guiGraphics.fill(15, LIST_START_Y + listHeight - 1, this.width - 15, LIST_START_Y + listHeight, 0xFF666666);

        for (int i = 0; i < items.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;

            if (itemY >= LIST_START_Y && itemY + ITEM_HEIGHT <= LIST_START_Y + listHeight) {
                boolean isSelected = (i == selectedIndex);

                if (isSelected) {
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + ITEM_HEIGHT, 0x404444FF);
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + 1, 0xFF4444FF);
                    guiGraphics.fill(16, itemY + ITEM_HEIGHT - 1, this.width - 16, itemY + ITEM_HEIGHT, 0xFF4444FF);
                }

                String itemText = itemRenderer.render(items.get(i));
                guiGraphics.drawString(this.font, itemText, 25, itemY + 7,
                        isSelected ? 0xFFFFAA : 0xDDDDDD);
            }
        }

        if (items.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7列表为空",
                    this.width / 2, LIST_START_Y + 50, 0x888888);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (maxScrollOffset <= 0) return;

        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;

        float scrollRatio = (float) scrollOffset / maxScrollOffset;
        int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                (int) (listHeight * listHeight / (float) (items.size() * ITEM_HEIGHT)));
        int availableTrackHeight = listHeight - scrollbarThumbHeight;
        int scrollbarThumbY = LIST_START_Y + (int) (availableTrackHeight * scrollRatio);

        guiGraphics.fill(scrollbarX, LIST_START_Y, scrollbarX + SCROLLBAR_WIDTH,
                LIST_START_Y + listHeight, 0x40000000);

        boolean isHovered = isMouseOverScrollbar(mouseX, mouseY, scrollbarX, scrollbarThumbY, scrollbarThumbHeight);
        int thumbColor = isDraggingScrollbar ? 0xFF888888 : (isHovered ? 0xFFAAAAAA : 0xFF666666);
        guiGraphics.fill(scrollbarX + 2, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 2,
                scrollbarThumbY + scrollbarThumbHeight, thumbColor);
        guiGraphics.fill(scrollbarX + 3, scrollbarThumbY + 1, scrollbarX + SCROLLBAR_WIDTH - 3,
                scrollbarThumbY + scrollbarThumbHeight - 1, 0xFFFFFFFF);
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY, int scrollbarX, int scrollbarThumbY, int scrollbarThumbHeight) {
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (maxScrollOffset > 0) {
            int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                    (int) (listHeight * listHeight / (float) (items.size() * ITEM_HEIGHT)));
            float trackAvailableHeight = listHeight - scrollbarThumbHeight;
            int thumbY = LIST_START_Y + (int) ((scrollOffset / (float) maxScrollOffset) * trackAvailableHeight);

            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
                    mouseY >= thumbY && mouseY < thumbY + scrollbarThumbHeight) {
                isDraggingScrollbar = true;
                dragStartMouseY = (int) mouseY;
                dragStartScrollOffset = scrollOffset;
                return true;
            }
        }

        if (mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_HEIGHT - 10) {
            int clickedIndex = (int) ((mouseY - LIST_START_Y + scrollOffset) / ITEM_HEIGHT);

            if (clickedIndex >= 0 && clickedIndex < items.size()) {
                selectedIndex = clickedIndex;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && maxScrollOffset > 0) {
            int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                    (int) (listHeight * listHeight / (float) (items.size() * ITEM_HEIGHT)));
            float trackAvailableHeight = listHeight - scrollbarThumbHeight;

            if (trackAvailableHeight > 0) {
                int deltaY = (int) mouseY - dragStartMouseY;
                float scrollRatio = deltaY / trackAvailableHeight;
                int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScrollOffset));

                if (newScrollOffset != scrollOffset) {
                    scrollOffset = newScrollOffset;
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        int scrollDelta = (int) (scrollAmount * ITEM_HEIGHT);
        int newScrollOffset = scrollOffset - scrollDelta;

        if (newScrollOffset < 0) newScrollOffset = 0;
        if (newScrollOffset > maxScrollOffset) newScrollOffset = maxScrollOffset;

        if (newScrollOffset != scrollOffset) {
            scrollOffset = newScrollOffset;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    /**
     * 项渲染器接口
     */
    @FunctionalInterface
    public interface ItemRenderer<T> {
        String render(T item);
    }
}
