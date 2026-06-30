package com.aljun.zombiegamereborn.common.client.gui.config.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象配置屏幕基类（支持滚动、标签页、自定义按钮）
 *
 * 功能特性：
 * 1. Tab 名称和内部内容完全自定义
 * 2. 底部按钮可自定义（保留返回按钮）
 * 3. 关闭屏幕时调用 Supplier 处理客户端 local JSON
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractConfigScreen extends Screen {

    protected TabManager tabManager;
    protected TabNavigationBar tabNavigationBar;
    protected final List<ConfigTab> tabs = new ArrayList<>();
    protected ConfigTab currentTab;

    protected JsonObject localJson = new JsonObject();
    protected boolean hasUnsavedChanges = false;
    protected boolean hasInteracted = false;
    protected boolean isInitializing = true;

    protected int scrollOffset = 0;
    protected int maxScrollOffset = 0;
    protected int contentHeight = 0;
    protected int savedScrollOffset = 0;
    protected int savedTabIndex = 0;

    protected static final int BOTTOM_BAR_HEIGHT = 40;
    protected int bottomBarY = 0;

    private static final int SCROLLBAR_WIDTH = 12;
    private static final int MIN_SCROLLBAR_HEIGHT = 20;
    protected boolean isDraggingScrollbar = false;
    protected int dragStartMouseY = 0;
    protected int dragStartScrollOffset = 0;
    protected int currentMouseY = 0;

    /**
     * 构造函数
     * @param title 屏幕标题
     * @param initSettings 初始配置数据
     */
    protected AbstractConfigScreen(String title, JsonObject initSettings) {
        super(Component.literal(title));
        this.loadDefaultSettings();
        this.loadServerSettings(initSettings);
        this.hasUnsavedChanges = false;
        this.hasInteracted = false;
    }

    /**
     * 加载服务端下发的配置
     */
    protected void loadServerSettings(JsonObject initSettings) {
        if (initSettings != null) {
            for (String key : initSettings.keySet()) {
                localJson.add(key, initSettings.get(key));
            }
        }
    }

    /**
     * 加载默认配置（子类必须实现）
     */
    protected abstract void loadDefaultSettings();

    /**
     * 初始化所有标签页（子类必须实现）
     * 在此方法中创建并注册所有的 ConfigTab
     */
    protected abstract void initializeTabs();

    /**
     * 获取底部自定义按钮列表（子类可重写）
     * 返回的按钮会自动渲染在右侧（返回按钮固定在左侧）
     */
    protected List<ButtonInfo> getCustomButtons() {
        return new ArrayList<>();
    }

    public void applyValue(String key, JsonElement jsonObject) {
        this.localJson.add(key, jsonObject);
    }

    /**
         * 按钮信息封装类
         */
        public record ButtonInfo(String text, Runnable onClick, java.util.function.BooleanSupplier isActive) {
        public ButtonInfo(String text, Runnable onClick) {
            this(text, onClick, () -> true);
        }
    }

    @Override
    protected void init() {
        super.init();

        this.width = this.minecraft.getWindow().getGuiScaledWidth();
        this.height = this.minecraft.getWindow().getGuiScaledHeight();

        this.tabs.clear();
        initializeTabs();

        this.tabManager = new TabManager(
                this::addRenderableWidget,
                this::removeWidget
        ) {
            @Override
            public void setCurrentTab(Tab tab, boolean playSound) {
                super.setCurrentTab(tab, playSound);
                if (tab instanceof ConfigTab) {
                    onTabChanged((ConfigTab) tab);
                }
            }
        };

        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
                .addTabs(this.tabs.toArray(new Tab[0]))
                .build();

        this.tabNavigationBar.arrangeElements();
        this.addRenderableWidget(this.tabNavigationBar);

        ScreenRectangle rectangle = this.tabNavigationBar.getRectangle();
        this.tabManager.setTabArea(rectangle);

        int topOffset = 35;
        int availableHeight = this.height - rectangle.bottom() - BOTTOM_BAR_HEIGHT - topOffset;
        this.bottomBarY = rectangle.bottom() + availableHeight + topOffset;
        ScreenRectangle contentArea = new ScreenRectangle(0, rectangle.bottom() + topOffset, this.width, availableHeight);

        for (ConfigTab tab : this.tabs) {
            tab.doLayout(contentArea);
        }

        if (!this.tabs.isEmpty()) {
            if (savedTabIndex >= 0 && savedTabIndex < this.tabs.size()) {
                this.tabManager.setCurrentTab(this.tabs.get(savedTabIndex), false);
            } else {
                this.tabManager.setCurrentTab(this.tabs.get(0), true);
            }
        }

        this.isInitializing = false;
        this.updateMaxScrollOffset();
        
        if (currentTab != null && currentTab.getPanel() != null) {
            this.scrollOffset = savedScrollOffset;
            currentTab.getPanel().setScrollOffset(scrollOffset);
        }
    }

    /**
     * 标签页切换时的处理
     */
    protected void onTabChanged(ConfigTab tab) {
        this.scrollOffset = 0;
        this.isDraggingScrollbar = false;
        this.currentTab = tab;

        if (tab != null && tab.getPanel() != null) {
            tab.getPanel().setScrollOffset(0);
            tab.getPanel().updateValues(localJson);
        }

        updateMaxScrollOffset();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.currentMouseY = mouseY;

        this.renderBackground(guiGraphics);

        updateMaxScrollOffset();
        if (currentTab != null && currentTab.getPanel() != null) {
            currentTab.getPanel().setScrollOffset(scrollOffset);
            currentTab.getPanel().render(guiGraphics, mouseX, mouseY, partialTick, scrollOffset);

            ScreenRectangle contentRect = tabNavigationBar.getRectangle();
            int clipStartY = contentRect.bottom();
            int clipHeight = bottomBarY - clipStartY;
            renderScrollbar(guiGraphics, mouseX, mouseY, partialTick, clipStartY, clipHeight);
        }

        if (isDraggingScrollbar && currentTab != null && currentTab.getPanel() != null && maxScrollOffset > 0) {
            ScreenRectangle contentRect = tabNavigationBar.getRectangle();
            int clipStartY = contentRect.bottom();
            int clipHeight = bottomBarY - clipStartY;

            int deltaY = this.currentMouseY - dragStartMouseY;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) (clipHeight * clipHeight / (float) contentHeight));
            float trackAvailableHeight = clipHeight - scrollbarThumbHeight;

            if (trackAvailableHeight > 0) {
                float scrollRatio = deltaY / trackAvailableHeight;
                int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScrollOffset));

                if (newScrollOffset != scrollOffset) {
                    scrollOffset = newScrollOffset;
                    if (currentTab != null && currentTab.getPanel() != null) {
                        currentTab.getPanel().setScrollOffset(scrollOffset);
                    }
                }
            }
        }

        renderBottomButtons(guiGraphics, mouseX, mouseY, partialTick);

        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 渲染底部按钮栏
     */
    protected void renderBottomButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int buttonY = bottomBarY + 10;
        int padding = 10;
        int rightPadding = 10;
        int buttonWidth = 100;
        int spacing = 5;

        String cancelButtonText = hasUnsavedChanges ? "§7✗ 取消" : "§7✓ 退出";
        Button backButton = Button.builder(
                        Component.literal(cancelButtonText),
                        btn -> onClose()
                )
                .bounds(padding, buttonY, 80, 20)
                .build();
        backButton.render(guiGraphics, mouseX, mouseY, partialTick);

        List<ButtonInfo> customButtons = getCustomButtons();
        int totalCustomWidth = customButtons.size() * buttonWidth + (customButtons.size() - 1) * spacing;
        int startX = this.width - rightPadding - totalCustomWidth;

        for (int i = 0; i < customButtons.size(); i++) {
            ButtonInfo buttonInfo = customButtons.get(i);
            int buttonX = startX + i * (buttonWidth + spacing);

            boolean active = buttonInfo.isActive() != null ? buttonInfo.isActive().getAsBoolean() : true;
            
            Button button = Button.builder(
                            Component.literal(buttonInfo.text()),
                            btn -> {
                                if (active) {
                                    buttonInfo.onClick().run();
                                }
                            }
                    )
                    .bounds(buttonX, buttonY, buttonWidth, 20)
                    .build();
            
            if (!active) {
                button.active = false;
            }
            
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab != null && currentTab.getPanel() != null && maxScrollOffset > 0) {
            ScreenRectangle contentRect = tabNavigationBar.getRectangle();
            int clipStartY = contentRect.bottom();
            int clipHeight = bottomBarY - clipStartY;

            int scrollbarX = this.width - SCROLLBAR_WIDTH - 5;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) (clipHeight * clipHeight / (float) contentHeight));
            float trackAvailableHeight = clipHeight - scrollbarThumbHeight;
            int thumbY = clipStartY + (int) ((scrollOffset / (float) maxScrollOffset) * trackAvailableHeight);

            if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
                    mouseY >= thumbY && mouseY < thumbY + scrollbarThumbHeight) {
                isDraggingScrollbar = true;
                dragStartMouseY = (int) mouseY;
                dragStartScrollOffset = scrollOffset;
                return true;
            }
        }

        if (currentTab != null && currentTab.getPanel() != null) {
            ScreenRectangle contentRect = tabNavigationBar.getRectangle();

            if (mouseY >= contentRect.bottom() && mouseY < bottomBarY) {
                for (GuiEventListener child : currentTab.getPanel().children()) {
                    if (child instanceof AbstractWidget widget) {
                        if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth() &&
                                mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                            setFocused(widget);
                            return widget.mouseClicked(mouseX, mouseY, button);
                        }
                    }
                }
                
                currentTab.getPanel().clearFocus();
            }
        }

        if (mouseY >= bottomBarY && mouseY < this.height) {
            int buttonY = bottomBarY + 10;
            int padding = 10;
            int rightPadding = 10;
            int buttonWidth = 100;
            int spacing = 5;

            if (mouseX >= padding && mouseX < padding + 80 &&
                    mouseY >= buttonY && mouseY < buttonY + 20) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
                onClose();
                return true;
            }

            List<ButtonInfo> customButtons = getCustomButtons();
            int totalCustomWidth = customButtons.size() * buttonWidth + (customButtons.size() - 1) * spacing;
            int startX = this.width - rightPadding - totalCustomWidth;

            for (int i = 0; i < customButtons.size(); i++) {
                ButtonInfo buttonInfo = customButtons.get(i);
                int buttonX = startX + i * (buttonWidth + spacing);

                boolean active = buttonInfo.isActive() != null ? buttonInfo.isActive().getAsBoolean() : true;
                
                if (active && mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                        mouseY >= buttonY && mouseY < buttonY + 20) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );
                    buttonInfo.onClick().run();
                    return true;
                }
            }
            
            if (currentTab != null && currentTab.getPanel() != null) {
                currentTab.getPanel().clearFocus();
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener listener) {
        super.setFocused(listener);

        if (currentTab != null && currentTab.getPanel() != null) {
            for (GuiEventListener child : currentTab.getPanel().children()) {
                if (child instanceof AbstractWidget widget && widget != listener) {
                    widget.setFocused(false);
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && currentTab != null && currentTab.getPanel() != null && maxScrollOffset > 0) {
            ScreenRectangle contentRect = tabNavigationBar.getRectangle();
            int clipStartY = contentRect.bottom();
            int clipHeight = bottomBarY - clipStartY;

            int deltaY = (int) mouseY - dragStartMouseY;
            int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) (clipHeight * clipHeight / (float) contentHeight));
            float trackAvailableHeight = clipHeight - scrollbarThumbHeight;

            if (trackAvailableHeight > 0) {
                float scrollRatio = deltaY / trackAvailableHeight;
                int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScrollOffset));

                if (newScrollOffset != scrollOffset) {
                    scrollOffset = newScrollOffset;
                    if (currentTab != null && currentTab.getPanel() != null) {
                        currentTab.getPanel().setScrollOffset(scrollOffset);
                    }
                    return true;
                }
            }
        }

        if (currentTab != null && currentTab.getPanel() != null) {
            for (GuiEventListener child : currentTab.getPanel().children()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        return widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                    }
                }
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (currentTab != null && currentTab.getPanel() != null) {
            for (GuiEventListener child : currentTab.getPanel().children()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        widget.mouseMoved(mouseX, mouseY);
                        return;
                    }
                }
            }
        }

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;

        if (currentTab != null && currentTab.getPanel() != null) {
            for (GuiEventListener child : currentTab.getPanel().children()) {
                if (child instanceof AbstractWidget widget) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        return widget.mouseReleased(mouseX, mouseY, button);
                    }
                }
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        int scrollDelta = (int) (scrollAmount * 15);
        int newScrollOffset = scrollOffset - scrollDelta;

        if (newScrollOffset < 0) {
            newScrollOffset = 0;
        }
        if (newScrollOffset > maxScrollOffset) {
            newScrollOffset = maxScrollOffset;
        }

        if (newScrollOffset != scrollOffset) {
            scrollOffset = newScrollOffset;
            if (currentTab != null && currentTab.getPanel() != null) {
                currentTab.getPanel().setScrollOffset(scrollOffset);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollAmount);
    }

    @Override
    public void tick() {
        super.tick();
        for (ConfigTab tab : tabs) {
            if (tab.getPanel() != null) {
                tab.getPanel().tick();
            }
        }
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;

        if (tabNavigationBar != null) {
            tabNavigationBar.setWidth(width);
            tabNavigationBar.arrangeElements();

            ScreenRectangle rectangle = tabNavigationBar.getRectangle();
            tabManager.setTabArea(rectangle);

            int topOffset = 35;
            int availableHeight = height - rectangle.bottom() - BOTTOM_BAR_HEIGHT - topOffset;
            ScreenRectangle contentArea = new ScreenRectangle(0, rectangle.bottom() + topOffset, width, availableHeight);
            this.bottomBarY = rectangle.bottom() + availableHeight + topOffset;

            for (ConfigTab tab : tabs) {
                tab.doLayout(contentArea);
            }

            Tab current = tabManager.getCurrentTab();
            if (current != null) {
                tabManager.setCurrentTab(current, false);
            }

            scrollOffset = 0;
            if (currentTab != null && currentTab.getPanel() != null) {
                currentTab.getPanel().setScrollOffset(0);
            }
            updateMaxScrollOffset();
        }
    }

    protected void updateMaxScrollOffset() {
        if (currentTab != null && currentTab.getPanel() != null) {
            contentHeight = currentTab.getPanel().getContentHeight();
            int availableHeight = bottomBarY - tabNavigationBar.getRectangle().bottom();
            maxScrollOffset = Math.max(0, contentHeight - availableHeight);
        }
    }

    protected void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int clipStartY, int clipHeight) {
        if (maxScrollOffset <= 0) {
            return;
        }

        int scrollbarX = this.width - SCROLLBAR_WIDTH - 5;
        int scrollbarY = clipStartY;
        int scrollbarTrackHeight = clipHeight;

        float scrollRatio = maxScrollOffset > 0 ? (float) scrollOffset / maxScrollOffset : 0;
        int scrollbarThumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) (clipHeight * clipHeight / (float) contentHeight));
        int availableTrackHeight = scrollbarTrackHeight - scrollbarThumbHeight;
        int scrollbarThumbY = scrollbarY + (int) (availableTrackHeight * scrollRatio);

        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarTrackHeight, 0x40000000);

        int thumbColor = isDraggingScrollbar ? 0xFF888888 : (isMouseOverScrollbar(mouseX, mouseY, scrollbarX, scrollbarThumbY, scrollbarThumbHeight) ? 0xFFAAAAAA : 0xFF666666);
        guiGraphics.fill(scrollbarX + 2, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 2, scrollbarThumbY + scrollbarThumbHeight, thumbColor);
        guiGraphics.fill(scrollbarX + 3, scrollbarThumbY + 1, scrollbarX + SCROLLBAR_WIDTH - 3, scrollbarThumbY + scrollbarThumbHeight - 1, 0xFFFFFFFF);
    }

    protected boolean isMouseOverScrollbar(int mouseX, int mouseY, int scrollbarX, int scrollbarThumbY, int scrollbarThumbHeight) {
        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;
    }

    @Override
    public void onClose() {
        saveScreenState();
        handleOnClose();
        super.onClose();
    }

    /**
     * 保存当前屏幕状态（滚动位置和 Tab 索引）
     */
    protected void saveScreenState() {
        if (currentTab != null) {
            savedScrollOffset = scrollOffset;
            
            for (int i = 0; i < tabs.size(); i++) {
                if (tabs.get(i) == currentTab) {
                    savedTabIndex = i;
                    break;
                }
            }
        }
    }

    /**
     * 处理关闭屏幕时的逻辑（子类可重写此方法实现自定义行为）
     * 默认实现：检查是否有未保存的更改并给出提示
     */
    protected void handleOnClose() {
        if (!hasInteracted) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§7配置没有修改"), false
                );
            }
        } else if (hasUnsavedChanges) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§c§l意外退出"), false
                );
            }
        } else {
            handleSaveAndClose();
        }
    }

    /**
     * 处理保存并关闭（子类可重写此方法实现自定义保存逻辑）
     */
    protected void handleSaveAndClose() {
        // 默认不做任何操作，子类可以重写
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 配置标签页内部类
     */
    protected class ConfigTab implements Tab {
        private final String title;
        private final PanelInitializer initializer;
        private SimpleSettingsPanel panel;

        public ConfigTab(String title, PanelInitializer initializer) {
            this.title = title;
            this.initializer = initializer;
        }

        @Override
        public @NotNull Component getTabTitle() {
            return Component.literal(title);
        }

        @Override
        public void visitChildren(@NotNull java.util.function.Consumer<AbstractWidget> consumer) {
            if (panel != null) {
                for (AbstractWidget widget : panel.getRenderables()) {
                    consumer.accept(widget);
                }
            }
        }

        @Override
        public void doLayout(@NotNull ScreenRectangle rectangle) {
            if (panel == null) {
                panel = new SimpleSettingsPanel(rectangle.width(), rectangle.height(), rectangle.top());
                initializer.init(panel);
                updateMaxScrollOffset();
            }

            panel.setPosition(rectangle.left(), rectangle.top());
            panel.setSize(rectangle.width(), rectangle.height());
            panel.updateValues(localJson);

            if (tabManager != null && tabManager.getCurrentTab() == this) {
                currentTab = this;
            }
        }

        @Override
        public void tick() {
            if (panel != null) {
                panel.tick();
            }
        }

        public SimpleSettingsPanel getPanel() {
            return panel;
        }
    }

    @FunctionalInterface
    protected interface PanelInitializer {
        void init(SimpleSettingsPanel panel);
    }
}
