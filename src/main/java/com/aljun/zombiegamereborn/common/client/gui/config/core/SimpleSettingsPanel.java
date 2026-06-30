package com.aljun.zombiegamereborn.common.client.gui.config.core;

import com.aljun.zombiegamereborn.common.client.gui.LabelWidget;
import com.aljun.zombiegamereborn.common.client.gui.ValidatedEditBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.*;

/**
 * 设置面板组件容器（支持滚动）
 *
 * 设计理念：
 * - 采用统一的 Row 概念，每个配置项包含左侧标签和右侧控件
 * - 彻底消除标签与控件错位问题
 * - 简化渲染逻辑，移除复杂的偏移计算
 */
@OnlyIn(Dist.CLIENT)
public class SimpleSettingsPanel extends AbstractContainerEventHandler implements GuiEventListener, NarratableEntry {

    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<AbstractWidget> renderables = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private final Map<String, ValidatedEditBox> editBoxes = new HashMap<>();
    private final Map<String, ForgeSlider> sliders = new HashMap<>();
    private final Map<String, Checkbox> checkboxes = new HashMap<>();
    private final Map<String, EnumButtonInfo> enumButtons = new HashMap<>();
    private final List<ButtonUpdateHandler> buttonUpdateHandlers = new ArrayList<>();

    private final Font font = Minecraft.getInstance().font;
    private int nextY;
    private int startX;
    private final int startY;
    private final int rowHeight = 24;
    private final int controlWidth = 150;
    private final int labelGap = 10;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int scrollOffset = 0;

    private BiConsumer<String, JsonElement> onValueChanged = null;

    public SimpleSettingsPanel(int screenWidth, int screenHeight, int startY) {
        this.startY = startY;
        this.nextY = startY;
        this.panelWidth = screenWidth;
        this.panelX = 0;
        this.panelY = 0;
        this.startX = (screenWidth - controlWidth) / 2;
    }

    /**
     * 设置滚动偏移（关键方法）
     */
    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
    }

    public void setOnValueChanged(BiConsumer<String,JsonElement> callback) {
        this.onValueChanged = callback;
    }

    public void setPosition(int x, int y) {
        this.panelX = x;
        this.panelY = y;
    }

    public void setSize(int screenWidth, int screenHeight) {
        this.panelWidth = screenWidth;
        this.startX = (screenWidth - controlWidth) / 2;
    }

    public void clear() {
        children.clear();
        renderables.clear();
        rows.clear();
        editBoxes.clear();
        sliders.clear();
        checkboxes.clear();
        enumButtons.clear();
        buttonUpdateHandlers.clear();
        nextY = startY;
    }

    public List<AbstractWidget> getRenderables() {
        return renderables;
    }

    public int getContentHeight() {
        return nextY - startY;
    }

    public void tick() {
        for (ValidatedEditBox editBox : editBoxes.values()) {
            editBox.tick();
        }

        for (ButtonUpdateHandler handler : buttonUpdateHandlers) {
            handler.update();
        }
    }

    // ==================== 添加组件方法 ====================

    public void addEditBox(String labelText, String key, String defaultValue) {
        ValidatedEditBox editBox = ValidatedEditBox.createStringEditBox(
                font, startX + panelX, nextY + panelY, controlWidth, 18, 
                Component.literal(labelText), defaultValue
        );

        editBox.setResponder((newValue) -> {
            if (onValueChanged != null && editBox.hasValidInput()) {
                onValueChanged.accept(key, new JsonPrimitive(newValue));
            }
        });

        children.add(editBox);
        renderables.add(editBox);
        editBoxes.put(key, editBox);

        rows.add(new Row(labelText, editBox, 0xFFFFFF));
        nextY += rowHeight;
    }

    public void addSlider(String labelText, String key, double min, double max, double currentValue) {
        final double[] lastValue = {currentValue};

        ForgeSlider slider = new ForgeSlider(
                startX + panelX, nextY + panelY, controlWidth, 18,
                Component.literal(labelText + ": "),
                Component.literal(""),
                min, max, currentValue,
                true
        ) {
            @Override
            protected void applyValue() {
                if (onValueChanged != null) {
                    double value = this.getValue();

                    if (Math.abs(value - lastValue[0]) > 0.001) {
                        lastValue[0] = value;
                        onValueChanged.accept(key, new JsonPrimitive(value));
                    }
                }
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        };

        children.add(slider);
        renderables.add(slider);
        sliders.put(key, slider);

        rows.add(new Row(labelText, slider, 0xFFFFFF));
        nextY += rowHeight;
    }

    public void addIntSlider(String labelText, String key, int min, int max, int currentValue) {
        final int[] lastValue = {currentValue};

        ForgeSlider slider = new ForgeSlider(
                startX + panelX, nextY + panelY, controlWidth, 18,
                Component.literal(labelText + ": "),
                Component.literal(""),
                min, max, currentValue,
                1.0, 0, true
        ) {
            @Override
            protected void applyValue() {
                if (onValueChanged != null) {
                    int intValue = this.getValueInt();

                    if (intValue != lastValue[0]) {
                        lastValue[0] = intValue;
                        onValueChanged.accept(key, new JsonPrimitive(intValue));
                    }
                }
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        };

        children.add(slider);
        renderables.add(slider);
        sliders.put(key, slider);

        rows.add(new Row(labelText, slider, 0xFFFFFF));
        nextY += rowHeight;
    }

    public void addCallbackabeScreen(String labelText, AbstractConfigScreen screen, String key, BiFunction<Screen,Consumer<JsonElement>,Screen> setUpper) {
        this.addSimpleButton(labelText, () -> {
            Minecraft.getInstance().setScreen(setUpper.apply(screen,(jsonObject)->{
                screen.applyValue(key,jsonObject) ;
            }));
        },()->"§e✎ 编辑");
    }

    public void addCheckBox(String labelText, String key, boolean defaultValue) {
        Checkbox checkbox = new Checkbox(
                startX + panelX,
                nextY + panelY,
                20,
                18,
                Component.empty(),
                defaultValue,
                true
        ) {
            @Override
            public void onPress() {
                super.onPress();
                if (onValueChanged != null) {
                    onValueChanged.accept(key, new JsonPrimitive(this.selected()));
                }
            }
        };

        children.add(checkbox);
        renderables.add(checkbox);
        checkboxes.put(key, checkbox);

        rows.add(new Row(labelText, checkbox, 0xFFFFFF));
        nextY += rowHeight;
    }

    /**
     * 添加整数输入框（带输入验证）
     */
    public void addIntEditBox(String labelText, String key, int defaultValue, int minValue, int maxValue) {
        ValidatedEditBox editBox = ValidatedEditBox.createIntEditBox(
                font, startX + panelX, nextY + panelY, controlWidth, 18,
                Component.literal(labelText), defaultValue, minValue, maxValue
        );

        editBox.setResponder((newValue) -> {
            if (onValueChanged != null && editBox.hasValidInput()) {
                onValueChanged.accept(key, new JsonPrimitive(editBox.getLastValidIntValue()));
            }
        });

        children.add(editBox);
        renderables.add(editBox);
        editBoxes.put(key, editBox);

        rows.add(new Row(labelText, editBox, 0xFFFFFF));
        nextY += rowHeight;
    }

    public <E extends Enum<E>> void addEnumCycleButton(
            String labelText,
            String key,
            E[] enumValues,
            E defaultValue,
            Function<E, String> displayFunc,
            Function<E, JsonElement> serializeFunc,
            Function<JsonElement, E> deserializeFunc
    ) {
        final int[] currentIndex = {defaultValue.ordinal()};

        enumButtons.put(key, new EnumButtonInfo(enumValues, currentIndex, displayFunc, serializeFunc, deserializeFunc));

        Button button = addSimpleButtonInternal(
                labelText,
                () -> {
                    currentIndex[0] = (currentIndex[0] + 1) % enumValues.length;
                    if (onValueChanged != null) {
                        JsonElement jsonElement = serializeFunc.apply(enumValues[currentIndex[0]]);
                        onValueChanged.accept(key, jsonElement);
                    }
                },
                () -> displayFunc.apply(enumValues[currentIndex[0]])
        );

        rows.add(new Row(labelText, button, 0xFFFFFF));
        nextY += rowHeight;
    }

    public <E extends Enum<E>> void addEnumCycleButton(String labelText, String key, E[] enumValues, E defaultValue, Function<E, String> displayFunc) {
        addEnumCycleButton(
                labelText,
                key,
                enumValues,
                defaultValue,
                displayFunc,
                e -> new JsonPrimitive(e.name()),
                json -> {
                    String name = json.getAsString();
                    for (E enumValue : enumValues) {
                        if (enumValue.name().equals(name)) {
                            return enumValue;
                        }
                    }
                    return defaultValue;
                }
        );
    }

    public <E extends Enum<E>> void addEnumCycleButton(String labelText, String key, E[] enumValues, E defaultValue) {
        addEnumCycleButton(labelText, key, enumValues, defaultValue, Enum::name);
    }

    /**
     * 添加浮点数输入框（带输入验证）
     */
    public void addDoubleEditBox(String labelText, String key, double defaultValue, double minValue, double maxValue) {
        ValidatedEditBox editBox = ValidatedEditBox.createDoubleEditBox(
                font, startX + panelX, nextY + panelY, controlWidth, 18,
                Component.literal(labelText), defaultValue, minValue, maxValue
        );

        editBox.setResponder((newValue) -> {
            if (onValueChanged != null && editBox.hasValidInput()) {
                onValueChanged.accept(key, new JsonPrimitive(editBox.getLastValidDoubleValue()));
            }
        });

        children.add(editBox);
        renderables.add(editBox);
        editBoxes.put(key, editBox);

        rows.add(new Row(labelText, editBox, 0xFFFFFF));
        nextY += rowHeight;
    }

    public void addSimpleButton(String text, Runnable onClick, @NotNull Supplier<String> displayText) {
        Button button = addSimpleButtonInternal(text, onClick, displayText);
        rows.add(new Row(text, button, 0xFFFFFF));
        nextY += rowHeight;
    }

    private Button addSimpleButtonInternal(String text, Runnable onClick, @NotNull Supplier<String> displayText) {
        int buttonX = startX + panelX;

        Button button = Button.builder(
                Component.literal(displayText.get()),
                btn -> onClick.run()
        ).bounds(buttonX, nextY + panelY, controlWidth, 20).build();

        children.add(button);
        renderables.add(button);

        buttonUpdateHandlers.add(() -> {
            String newText = displayText.get();
            if (!button.getMessage().getString().equals(newText)) {
                button.setMessage(Component.literal(newText));
            }
        });

        return button;
    }

    /**
     * 添加居中标签（作为独立组件）
     */
    public void addLabel(String text) {
        int labelWidth = 120;
        LabelWidget label = new LabelWidget(0, nextY + panelY, labelWidth, 20, text, 0xFFAA00);
        children.add(label);
        renderables.add(label);

        rows.add(new Row(null, label, 0xFFAA00, true));
        nextY += rowHeight;
    }

    // ==================== 获取值方法 ====================

    public JsonObject getCurrentValues() {
        JsonObject values = new JsonObject();
        int validationFailCount = 0;

        for (Map.Entry<String, ValidatedEditBox> entry : editBoxes.entrySet()) {
            String key = entry.getKey();
            ValidatedEditBox editBox = entry.getValue();
            
            if (!editBox.hasValidInput()) {
                validationFailCount++;
                continue;
            }

            switch (editBox.getType()) {
                case STRING:
                    values.addProperty(key, editBox.getLastValidStringValue());
                    break;
                case INTEGER:
                    values.addProperty(key, editBox.getLastValidIntValue());
                    break;
                case DOUBLE:
                    values.addProperty(key, editBox.getLastValidDoubleValue());
                    break;
            }
        }

        for (Map.Entry<String, ForgeSlider> entry : sliders.entrySet()) {
            double value = entry.getValue().getValue();
            if (Math.abs(value - Math.round(value)) < 0.001) {
                values.addProperty(entry.getKey(), (int) Math.round(value));
            } else {
                values.addProperty(entry.getKey(), value);
            }
        }

        for (Map.Entry<String, Checkbox> entry : checkboxes.entrySet()) {
            values.addProperty(entry.getKey(), entry.getValue().selected());
        }

        for (Map.Entry<String, EnumButtonInfo> entry : enumButtons.entrySet()) {
            EnumButtonInfo info = entry.getValue();
            JsonElement jsonElement = info.serializeFunc.apply(info.enumValues[info.currentIndex[0]]);
            values.add(entry.getKey(), jsonElement);
        }

        if (validationFailCount > 0) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(
                        "§c已忽略 " + validationFailCount + " 个无效的输入值，已自动恢复为上次的有效值"
                    ),
                    false
                );
            }
        }

        return values;
    }

    public void updateValues(JsonObject settings) {
        for (Map.Entry<String, ValidatedEditBox> entry : editBoxes.entrySet()) {
            String key = entry.getKey();
            if (settings.has(key)) {
                ValidatedEditBox editBox = entry.getValue();
                JsonElement element = settings.get(key);
                
                switch (editBox.getType()) {
                    case STRING:
                        editBox.setValue(element.getAsString());
                        break;
                    case INTEGER:
                        editBox.setValue(String.valueOf(element.getAsInt()));
                        break;
                    case DOUBLE:
                        editBox.setValue(String.valueOf(element.getAsDouble()));
                        break;
                }
            }
        }

        for (Map.Entry<String, ForgeSlider> entry : sliders.entrySet()) {
            String key = entry.getKey();
            if (settings.has(key)) {
                try {
                    double value = settings.get(key).getAsDouble();
                    entry.getValue().setValue(value);
                } catch (Exception ignored) {}
            }
        }

        for (Map.Entry<String, Checkbox> entry : checkboxes.entrySet()) {
            String key = entry.getKey();
            if (settings.has(key)) {
                boolean newValue = settings.get(key).getAsBoolean();
                Checkbox checkbox = entry.getValue();
                if (checkbox.selected() != newValue) {
                    checkbox.onPress();
                }
            }
        }

        for (Map.Entry<String, EnumButtonInfo> entry : enumButtons.entrySet()) {
            String key = entry.getKey();
            if (settings.has(key)) {
                try {
                    EnumButtonInfo info = entry.getValue();
                    JsonElement jsonElement = settings.get(key);
                    Enum<?> newEnumValue = info.deserializeFunc.apply(jsonElement);
                    for (int i = 0; i < info.enumValues.length; i++) {
                        if (info.enumValues[i] == newEnumValue) {
                            info.currentIndex[0] = i;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ==================== 渲染和事件 ====================

    /**
     * 渲染面板（统一渲染逻辑）
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int scrollOffset) {
        int currentY = startY - scrollOffset;

        for (Row row : rows) {
            if (row.isLabelOnly) {
                if (row.widget != null) {
                    int labelWidth = 120;
                    int labelX = (panelWidth - labelWidth) / 2 + panelX;
                    row.widget.setX(labelX);
                    row.widget.setY(currentY + panelY + 2);
                    row.widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            } else {
                if (row.labelText != null && !row.labelText.isEmpty()) {
                    int labelWidth = font.width(row.labelText);
                    int labelX = startX - labelWidth - labelGap + panelX;
                    int labelY = currentY + panelY + 5;
                    guiGraphics.drawString(font, row.labelText, labelX, labelY, row.labelColor);
                }

                if (row.widget != null) {
                    row.widget.setY(currentY + panelY);
                    row.widget.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }

            currentY += row.height;
        }
    }
    public void clearFocus() {
        for (ValidatedEditBox editBox : editBoxes.values()) {
            editBox.setFocused(false);
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {}

    // ==================== 内部类 ====================

    /**
     * 统一的行概念 - 解决标签与控件错位问题
     */
    private static class Row {
        String labelText;
        AbstractWidget widget;
        int labelColor;
        boolean isLabelOnly;
        int height;

        Row(String labelText, AbstractWidget widget, int labelColor) {
            this.labelText = labelText;
            this.widget = widget;
            this.labelColor = labelColor;
            this.isLabelOnly = false;
            this.height = 24;
        }

        Row(String labelText, AbstractWidget widget, int labelColor, boolean isLabelOnly) {
            this.labelText = labelText;
            this.widget = widget;
            this.labelColor = labelColor;
            this.isLabelOnly = isLabelOnly;
            this.height = 24;
        }
    }

    @FunctionalInterface
    private interface ButtonUpdateHandler {
        void update();
    }

    private static class EnumButtonInfo {
        Enum<?>[] enumValues;
        int[] currentIndex;
        Function<Enum<?>, String> displayFunc;
        Function<Enum<?>, JsonElement> serializeFunc;
        Function<JsonElement, Enum<?>> deserializeFunc;

        @SuppressWarnings("unchecked")
        EnumButtonInfo(
                Enum<?>[] enumValues,
                int[] currentIndex,
                Function<?, String> displayFunc,
                Function<?, JsonElement> serializeFunc,
                Function<JsonElement, ?> deserializeFunc
        ) {
            this.enumValues = enumValues;
            this.currentIndex = currentIndex;
            this.displayFunc = (Function<Enum<?>, String>) displayFunc;
            this.serializeFunc = (Function<Enum<?>, JsonElement>) serializeFunc;
            this.deserializeFunc = (Function<JsonElement, Enum<?>>) deserializeFunc;
        }
    }
}
