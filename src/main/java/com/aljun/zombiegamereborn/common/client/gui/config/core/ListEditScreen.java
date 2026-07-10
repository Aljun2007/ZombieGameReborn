package com.aljun.zombiegamereborn.common.client.gui.config.core;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 列表编辑屏幕 - 用于编辑 ArrayList
 * <p>
 * 功能特性：
 * 1. 滑动列表展示所有项
 * 2. 新建项
 * 3. 删除项
 * 4. 修改项（打开新窗口）
 * 5. 选中项高亮
 * 6. 复制选中项
 * 7. 通过 Callback 返回修改后的列表
 * 8. 退出时自动冒泡排序（通过 setComparator 指定排序规则）
 */
@OnlyIn(Dist.CLIENT)
public class ListEditScreen<T> extends Screen implements Callbackable<List<T>> {

    private static final int ITEM_HEIGHT = 24;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int MIN_SCROLLBAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 40;
    private static final int LIST_START_Y = 35;
    private final List<T> originalList;
    private final Consumer<List<T>> onSaveCallback;
    private final ItemRenderer<T> itemRenderer;
    private final ItemEditor<T> itemEditor;
    private final Supplier<T> defaultItemSupplier;
    private List<T> currentList;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartMouseY = 0;
    private int dragStartScrollOffset = 0;
    private int currentMouseY = 0;


    private Button addButton;
    private Button copyButton;
    private Button deleteButton;
    private Button editButton;
    private Button saveButton;
    private Button cancelButton;
    private Screen lastScreen;

    private BiPredicate<T, T> comparator = (e1, e2) -> false;

    /**
     * 设置冒泡排序的比较器
     * <p>
     * 在 {@link #onSave()} 保存时会自动按此比较器进行冒泡排序。
     * 默认值 {@code (e1, e2) -> false} 表示不排序。
     *
     * @param comparator 比较器，返回 true 表示 e1 应排在 e2 之前
     */
    public void setComparator(BiPredicate<T, T> comparator) {
        this.comparator = comparator;
    }

    /**
     * 构造函数（带默认项提供者）
     *
     * @param title               屏幕标题
     * @param lastScreen          上一个屏幕
     * @param initialList         初始列表
     * @param onSaveCallback      保存时的回调，返回修改后的列表
     * @param itemRenderer        项渲染器
     * @param itemEditor          项编辑器
     * @param defaultItemSupplier 默认项提供者（用于"新建"按钮）
     */
    public ListEditScreen(
            String title,
            Screen lastScreen,
            List<T> initialList,
            Consumer<List<T>> onSaveCallback,
            ItemRenderer<T> itemRenderer,
            ItemEditor<T> itemEditor,
            Supplier<T> defaultItemSupplier
    ) {
        super(Component.translatable(title));
        this.originalList = new ArrayList<>(initialList);
        this.currentList = new ArrayList<>(initialList);
        this.onSaveCallback = onSaveCallback;
        this.itemRenderer = itemRenderer;
        this.itemEditor = itemEditor;
        this.lastScreen = lastScreen;
        this.defaultItemSupplier = defaultItemSupplier;
    }

    /**
     * 构造函数（兼容旧版本，不提供默认项提供者）
     *
     * @param title          屏幕标题
     * @param lastScreen     上一个屏幕
     * @param initialList    初始列表
     * @param onSaveCallback 保存时的回调，返回修改后的列表
     * @param itemRenderer   项渲染器
     * @param itemEditor     项编辑器
     * @deprecated 请使用带 defaultItemSupplier 的构造函数
     */
    @Deprecated
    public ListEditScreen(
            String title,
            Screen lastScreen,
            List<T> initialList,
            Consumer<List<T>> onSaveCallback,
            ItemRenderer<T> itemRenderer,
            ItemEditor<T> itemEditor
    ) {
        this(title, lastScreen, initialList, onSaveCallback, itemRenderer, itemEditor, () -> null);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseY = mouseY;

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染标题（与 ConfigScreen Tab 标签文字位置一致）
        guiGraphics.drawString(this.font, this.title.getString(),
                10, 20, 0xFFFFFF);

        // 渲染列表区域
        renderListArea(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染滚动条
        renderScrollbar(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染选中提示
        if (selectedIndex >= 0 && selectedIndex < currentList.size()) {
            String selectedText = I18n.get("gui.zombiegamereborn.core.selected_prefix") + itemRenderer.render(currentList.get(selectedIndex));
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

        // 清除旧按钮
        if (this.addButton != null) {
            this.removeWidget(this.addButton);
            this.removeWidget(this.copyButton);
            this.removeWidget(this.deleteButton);
            this.removeWidget(this.editButton);
            this.removeWidget(this.saveButton);
            this.removeWidget(this.cancelButton);
        }

        // 左侧取消按钮（与 AbstractConfigScreen 一致）
        int padding = 10;
        this.cancelButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.core.cancel"),
                btn -> onCancel()
        ).bounds(padding, buttonY, 55, 20).build();
        
        int centerButtonWidth = 55;
        int buttonSpacing = 3;
        int centerGroupWidth = centerButtonWidth * 4 + buttonSpacing * 3;
        int centerX = this.width / 2;
        int centerGroupStartX = centerX - centerGroupWidth / 2;

        this.addButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.listedit.add"),
                btn -> onAddItem()
        ).bounds(centerGroupStartX, buttonY, centerButtonWidth, 20).build();

        this.copyButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.listedit.copy"),
                btn -> onCopyItem()
        ).bounds(centerGroupStartX + centerButtonWidth + buttonSpacing, buttonY, centerButtonWidth, 20).build();

        this.deleteButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.listedit.delete"),
                btn -> onDeleteItem()
        ).bounds(centerGroupStartX + (centerButtonWidth + buttonSpacing) * 2, buttonY, centerButtonWidth, 20).build();

        this.editButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.listedit.edit"),
                btn -> onEditItem()
        ).bounds(centerGroupStartX + (centerButtonWidth + buttonSpacing) * 3, buttonY, centerButtonWidth, 20).build();

        int rightPadding = 10;
        int rightButtonWidth = 55;
        int rightGroupStartX = this.width - rightPadding - rightButtonWidth;

        this.saveButton = Button.builder(
                Component.translatable("gui.zombiegamereborn.listedit.save"),
                btn -> onSave()
        ).bounds(rightGroupStartX, buttonY, rightButtonWidth, 20).build();

        this.addRenderableWidget(cancelButton);
        this.addRenderableWidget(addButton);
        this.addRenderableWidget(copyButton);
        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(editButton);
        this.addRenderableWidget(saveButton);

        updateButtonsState();
        updateMaxScrollOffset();
        bubbleSort();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;

        // 重新初始化布局，但不清除已有组件
        if (addButton != null) {
            // 重新计算按钮位置
            init();
        }

        updateMaxScrollOffset();
    }

    /**
     * 更新最大滚动偏移
     */
    private void updateMaxScrollOffset() {
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int contentHeight = currentList.size() * ITEM_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - listHeight);
    }

    /**
     * 新建项
     */
    private void onAddItem() {
        createNewItem();
    }

    /**
     * 复制选中项
     */
    private void onCopyItem() {
        if (selectedIndex >= 0 && selectedIndex < currentList.size()) {
            T selectedItem = currentList.get(selectedIndex);
            int i = selectedIndex;
            itemEditor.edit(this, selectedItem, newItem -> {
                currentList.add(i + 1, newItem);
                selectedIndex = i + 1;
                updateButtonsState();
                updateMaxScrollOffset();

                if (scrollOffset > maxScrollOffset) {
                    scrollOffset = maxScrollOffset;
                }
            });
        }
    }

    /**
     * 删除选中项（带确认对话框）
     */
    private void onDeleteItem() {
        if (selectedIndex >= 0 && selectedIndex < currentList.size()) {
            T selectedItem = currentList.get(selectedIndex);
            String itemText = itemRenderer.render(selectedItem);
            
            Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(
                    this,
                    itemText,
                    () -> {
                        currentList.remove(selectedIndex);
                        selectedIndex = Math.min(selectedIndex, currentList.size() - 1);
                        updateButtonsState();
                        updateMaxScrollOffset();
                        
                        if (scrollOffset > maxScrollOffset) {
                            scrollOffset = maxScrollOffset;
                        }
                    }
            ));
        }
    }

    /**
     * 编辑选中项
     */
    private void onEditItem() {
        if (selectedIndex >= 0 && selectedIndex < currentList.size()) {
            T selectedItem = currentList.get(selectedIndex);
            int i = selectedIndex;
            itemEditor.edit(this, selectedItem, editedItem -> {
                currentList.set(i, editedItem);
            });
        }
    }

    /**
     * 取消并关闭（不保存更改）
     */
    private void onCancel() {
        backAndCancel();
    }

    /**
     * 保存并关闭（保存当前列表，自动冒泡排序）
     */
    private void onSave() {
        bubbleSort();
        backAndSave(new ArrayList<>(currentList));
    }

    /**
     * 对当前列表进行冒泡排序
     */
    private void bubbleSort() {
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < currentList.size() - 1; i++) {
                if (comparator.test(currentList.get(i), currentList.get(i + 1))) {
                    T temp = currentList.get(i);
                    currentList.set(i, currentList.get(i + 1));
                    currentList.set(i + 1, temp);
                    swapped = true;
                }
            }
        } while (swapped);
    }

    /**
     * 创建新项（使用提供的默认项或子类重写的方法）
     */
    protected void createNewItem() {
        if (itemEditor != null) {
            T defaultItem = (defaultItemSupplier != null) ? defaultItemSupplier.get() : createDefaultItem();
            itemEditor.edit(this, defaultItem, newItem -> {
                currentList.add(newItem);
                selectedIndex = currentList.size() - 1;
                updateButtonsState();
                updateMaxScrollOffset();

                if (maxScrollOffset > 0) {
                    scrollOffset = maxScrollOffset;
                }
            });
        }
    }

    /**
     * 创建默认项（子类可重写，但建议使用 Supplier 方式）
     * @deprecated 建议在构造函数中传入 defaultItemSupplier
     */
    @Deprecated
    protected T createDefaultItem() {
        return null;
    }

    /**
     * 渲染列表区域
     */
    private void renderListArea(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int listWidth = this.width - 40;
        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;

        // 绘制列表背景
        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + listHeight, 0x30000000);
        guiGraphics.fill(15, LIST_START_Y, this.width - 15, LIST_START_Y + 1, 0xFF666666);
        guiGraphics.fill(15, LIST_START_Y + listHeight - 1, this.width - 15, LIST_START_Y + listHeight, 0xFF666666);

        // 渲染列表项
        for (int i = 0; i < currentList.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;

            // 检查是否在可视区域内
            if (itemY >= LIST_START_Y && itemY + ITEM_HEIGHT <= LIST_START_Y + listHeight) {
                boolean isSelected = (i == selectedIndex);

                // 绘制选中背景
                if (isSelected) {
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + ITEM_HEIGHT, 0x404444FF);
                    guiGraphics.fill(16, itemY, this.width - 16, itemY + 1, 0xFF4444FF);
                    guiGraphics.fill(16, itemY + ITEM_HEIGHT - 1, this.width - 16, itemY + ITEM_HEIGHT, 0xFF4444FF);
                }

                // 绘制项文本
                String itemText = itemRenderer.render(currentList.get(i));
                guiGraphics.drawString(this.font, itemText, 25, itemY + 7,
                        isSelected ? 0xFFFFAA : 0xDDDDDD);
            }
        }

        // 如果列表为空，显示提示
        if (currentList.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.core.list_empty_hint"),
                    this.width / 2, LIST_START_Y + 50, 0x888888);
        }
    }

    /**
     * 渲染滚动条
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (maxScrollOffset <= 0) {
            return;
        }

        int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
        int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;

        float scrollRatio = (float) scrollOffset / maxScrollOffset;
        int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                (int) (listHeight * listHeight / (float) (currentList.size() * ITEM_HEIGHT)));
        int availableTrackHeight = listHeight - scrollbarThumbHeight;
        int scrollbarThumbY = LIST_START_Y + (int) (availableTrackHeight * scrollRatio);

        // 绘制轨道
        guiGraphics.fill(scrollbarX, LIST_START_Y, scrollbarX + SCROLLBAR_WIDTH,
                LIST_START_Y + listHeight, 0x40000000);

        // 绘制滑块
        boolean isHovered = isMouseOverScrollbar(mouseX, mouseY, scrollbarX, scrollbarThumbY, scrollbarThumbHeight);
        int thumbColor = isDraggingScrollbar ? 0xFF888888 : (isHovered ? 0xFFAAAAAA : 0xFF666666);
        guiGraphics.fill(scrollbarX + 2, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 2,
                scrollbarThumbY + scrollbarThumbHeight, thumbColor);
        guiGraphics.fill(scrollbarX + 3, scrollbarThumbY + 1, scrollbarX + SCROLLBAR_WIDTH - 3,
                scrollbarThumbY + scrollbarThumbHeight - 1, 0xFFFFFFFF);
    }

    /**
     * 检查鼠标是否在滚动条上
     */
    private boolean isMouseOverScrollbar(int mouseX, int mouseY, int scrollbarX, int scrollbarThumbY, int scrollbarThumbHeight) {
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了滚动条
        if (maxScrollOffset > 0) {
            int listHeight = this.height - LIST_START_Y - BOTTOM_BAR_HEIGHT - 20;
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 20;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT,
                    (int) (listHeight * listHeight / (float) (currentList.size() * ITEM_HEIGHT)));
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

        // 检查是否点击了列表项
        if (mouseY >= LIST_START_Y && mouseY < this.height - BOTTOM_BAR_HEIGHT - 10) {
            int clickedIndex = (int) ((mouseY - LIST_START_Y + scrollOffset) / ITEM_HEIGHT);

            if (clickedIndex >= 0 && clickedIndex < currentList.size()) {
                selectedIndex = clickedIndex;
                updateButtonsState();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonsState() {
        boolean hasSelection = selectedIndex >= 0 && selectedIndex < currentList.size();
        copyButton.active = hasSelection;
        deleteButton.active = hasSelection;
        editButton.active = hasSelection;
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
                    (int) (listHeight * listHeight / (float) (currentList.size() * ITEM_HEIGHT)));
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

        if (newScrollOffset < 0) {
            newScrollOffset = 0;
        }
        if (newScrollOffset > maxScrollOffset) {
            newScrollOffset = maxScrollOffset;
        }

        if (newScrollOffset != scrollOffset) {
            scrollOffset = newScrollOffset;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public Screen getLastScreen() {
        return this.lastScreen;
    }

    @Override
    public Consumer<List<T>> getCallBack() {
        return this.onSaveCallback;
    }

    /**
     * 项渲染器接口 - 用于自定义如何渲染每一项
     */
    @FunctionalInterface
    public interface ItemRenderer<T> {
        String render(T item);
    }

    /**
     * 项编辑器接口 - 用于自定义如何编辑项
     */
    @FunctionalInterface
    public interface ItemEditor<T> {
        void edit(Screen parent, T item, Consumer<T> callback);
    }

    /**
     * 默认的字符串项编辑器
     */
    public static class StringItemEditor implements ItemEditor<String> {
        @Override
        public void edit(Screen parent, String item, Consumer<String> callback) {
            Minecraft.getInstance().setScreen(new StringEditScreen(parent, item, callback));
        }
    }

    /**
     * 字符串编辑子屏幕
     */
    private static class StringEditScreen extends Screen {
        private final Screen parent;
        private final String initialValue;
        private final Consumer<String> callback;
        private EditBox editBox;
        private Button confirmButton;
        private Button cancelButton;

        protected StringEditScreen(Screen parent, String initialValue, Consumer<String> callback) {
            super(Component.translatable("gui.zombiegamereborn.listedit.edit_title"));
            this.parent = parent;
            this.initialValue = initialValue != null ? initialValue : "";
            this.callback = callback;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);

            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.listedit.edit_title"), this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        }

        @Override
        protected void init() {
            super.init();

            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.editBox = new EditBox(this.font, centerX - 100, centerY - 30, 200, 20, Component.translatable("gui.zombiegamereborn.listedit.value_label"));
            this.editBox.setValue(initialValue);
            this.addWidget(this.editBox);
            this.setFocused(this.editBox);

            this.confirmButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.confirm"),
                    btn -> {
                        callback.accept(editBox.getValue());
                        Minecraft.getInstance().setScreen(parent);
                    }
            ).bounds(centerX - 110, centerY + 10, 100, 20).build();

            this.cancelButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.cancel"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).bounds(centerX + 10, centerY + 10, 100, 20).build();

            this.addRenderableWidget(confirmButton);
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }

    /**
     * 删除确认对话框
     */
    private static class ConfirmDeleteScreen extends Screen {
        private final Screen parent;
        private final String itemText;
        private final Runnable onConfirm;
        private Button confirmButton;
        private Button cancelButton;

        protected ConfirmDeleteScreen(Screen parent, String itemText, Runnable onConfirm) {
            super(Component.translatable("gui.zombiegamereborn.listedit.confirm_delete_title"));
            this.parent = parent;
            this.itemText = itemText;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();

            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.confirmButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.listedit.confirm_delete_button"),
                    btn -> {
                        onConfirm.run();
                        Minecraft.getInstance().setScreen(parent);
                    }
            ).bounds(centerX - 110, centerY + 30, 100, 20).build();

            this.cancelButton = Button.builder(
                    Component.translatable("gui.zombiegamereborn.core.cancel"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).bounds(centerX + 10, centerY + 30, 100, 20).build();

            this.addRenderableWidget(confirmButton);
            this.addRenderableWidget(cancelButton);
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTick);

            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.listedit.delete_title"), 
                    this.width / 2, this.height / 2 - 40, 0xFF5555);

            guiGraphics.drawCenteredString(this.font, I18n.get("gui.zombiegamereborn.listedit.delete_confirm_text"), 
                    this.width / 2, this.height / 2 - 10, 0xFFFFFF);

            String displayText = itemText.length() > 40 ? 
                    itemText.substring(0, 37) + "..." : itemText;
            guiGraphics.drawCenteredString(this.font, "§7" + displayText, 
                    this.width / 2, this.height / 2 + 5, 0xAAAAAA);
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }

}
