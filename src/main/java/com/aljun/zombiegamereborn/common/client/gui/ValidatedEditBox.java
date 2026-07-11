package com.aljun.zombiegamereborn.common.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;


@OnlyIn(Dist.CLIENT)
public class ValidatedEditBox extends EditBox {

    @FunctionalInterface
    public interface SuggestionProvider {
        List<String> getSuggestions(String input);
    }

    private final EditType type;
    private final double minValue;
    private final double maxValue;
    private Object lastValidValue;
    private boolean hasValidInput = true;
    private final Font cachedFont;

    // 下拉补全
    private SuggestionProvider suggestionProvider;
    private List<String> suggestions = List.of();
    private int selectedSuggestionIndex = -1;
    private int suggestionScrollOffset = 0;
    private static final int SUGGESTION_HEIGHT = 12;
    private static final int MAX_VISIBLE_SUGGESTIONS = 6;
    private static final int SUGGESTION_BG_COLOR = 0xcc000000;
    private static final int SUGGESTION_TEXT_COLOR = 0xffffffff;

    public enum EditType {
        STRING,
        INTEGER,
        DOUBLE
    }

    public static ValidatedEditBox createStringEditBox(Font font, int x, int y, int width, int height, Component label, String defaultValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.STRING, 0, 0, defaultValue);
    }

    public static ValidatedEditBox createIntEditBox(Font font, int x, int y, int width, int height, Component label, int defaultValue, int minValue, int maxValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.INTEGER, minValue, maxValue, defaultValue);
    }

    public static ValidatedEditBox createDoubleEditBox(Font font, int x, int y, int width, int height, Component label, double defaultValue, double minValue, double maxValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.DOUBLE, minValue, maxValue, defaultValue);
    }

    private ValidatedEditBox(Font font, int x, int y, int width, int height, Component label, EditType type, double minValue, double maxValue, Object defaultValue) {
        super(font, x, y, width, height, label);
        this.cachedFont = font;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.lastValidValue = defaultValue;

        if (type == EditType.STRING) {
            this.setMaxLength(1024);
        }

        setValue(String.valueOf(defaultValue));
        setupValidation();
    }

    public void setSuggestionProvider(SuggestionProvider provider) {
        this.suggestionProvider = provider;
    }

    // 确保每次 setResponder 都会触发 suggestion 刷新
    @Override
    public void setResponder(Consumer<String> responder) {
        super.setResponder(text -> {
            refreshSuggestions();
            responder.accept(text);
        });
    }

    private void refreshSuggestions() {
        if (suggestionProvider != null) {
            String input = getValue();
            suggestions = suggestionProvider.getSuggestions(input);
            selectedSuggestionIndex = -1;
            suggestionScrollOffset = 0;
        }
    }

    private void setupValidation() {
        this.setResponder((newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                hasValidInput = false;
                return;
            }
            hasValidInput = validate(newValue);
        });

        this.setFormatter((text, cursorPos) -> {
            if (text == null || text.isEmpty()) {
                return Component.literal(String.valueOf(lastValidValue)).getVisualOrderText();
            }

            if (validate(text)) {
                lastValidValue = parseValue(text);
                hasValidInput = true;
                return Component.literal(text).getVisualOrderText();
            }

            // 尝试纠正：过大→最大值，过小→最小值，非法字符→恢复旧值
            String corrected = clampToBounds(text);
            if (corrected != null) {
                hasValidInput = true;
                lastValidValue = parseValue(corrected);
                this.setValue(corrected);
                return Component.literal(corrected).getVisualOrderText();
            }

            hasValidInput = false;
            String fallback = String.valueOf(lastValidValue);
            this.setValue(fallback);
            return Component.literal(fallback).getVisualOrderText();
        });
    }

    @Nullable
    private String clampToBounds(String text) {
        if (type == EditType.STRING) return null;
        try {
            if (type == EditType.INTEGER) {
                int parsed = Integer.parseInt(text.trim());
                if (parsed < minValue) return String.valueOf((int) minValue);
                if (parsed > maxValue) return String.valueOf((int) maxValue);
                return text;
            }
            if (type == EditType.DOUBLE) {
                double parsed = Double.parseDouble(text.trim());
                if (parsed < minValue) return String.valueOf(minValue);
                if (parsed > maxValue) return String.valueOf(maxValue);
                return text;
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    public boolean validate(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        try {
            switch (type) {
                case STRING:
                    return true;
                case INTEGER:
                    int intValue = Integer.parseInt(input.trim());
                    return intValue >= minValue && intValue <= maxValue;
                case DOUBLE:
                    double doubleValue = Double.parseDouble(input.trim());
                    return doubleValue >= minValue && doubleValue <= maxValue;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Object parseValue(String input) {
        try {
            switch (type) {
                case STRING:
                    return input;
                case INTEGER:
                    return Integer.parseInt(input.trim());
                case DOUBLE:
                    return Double.parseDouble(input.trim());
                default:
                    return input;
            }
        } catch (NumberFormatException e) {
            return lastValidValue;
        }
    }

    public String getLastValidStringValue() {
        return String.valueOf(lastValidValue);
    }

    public int getLastValidIntValue() {
        if (lastValidValue instanceof Integer) {
            return (Integer) lastValidValue;
        }
        try {
            return Integer.parseInt(String.valueOf(lastValidValue));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getLastValidDoubleValue() {
        if (lastValidValue instanceof Double) {
            return (Double) lastValidValue;
        }
        try {
            return Double.parseDouble(String.valueOf(lastValidValue));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public boolean hasValidInput() {
        return hasValidInput;
    }

    public EditType getType() {
        return type;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void resetToDefault() {
        setValue(String.valueOf(lastValidValue));
        hasValidInput = true;
    }

    // ==================== 下拉补全渲染 ====================

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (!isFocused() || suggestions.isEmpty() || getValue().isEmpty()) {
            return;
        }

        int x = getX();
        int y = getY() + getHeight() + 2;
        int width = getWidth();
        int visibleCount = Math.min(suggestions.size(), MAX_VISIBLE_SUGGESTIONS);
        int dropdownHeight = visibleCount * SUGGESTION_HEIGHT + 2;

        // 提升 Z 层，确保覆盖其他控件
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);

        // 背景
        guiGraphics.fill(x, y, x + width, y + dropdownHeight, SUGGESTION_BG_COLOR);

        // 选项行
        for (int i = 0; i < visibleCount; i++) {
            int idx = suggestionScrollOffset + i;
            if (idx >= suggestions.size()) break;

            int rowY = y + 1 + i * SUGGESTION_HEIGHT;
            String suggestion = suggestions.get(idx);

            guiGraphics.drawString(this.cachedFont, suggestion, x + 4, rowY + 2, SUGGESTION_TEXT_COLOR);
        }

        pose.popPose();
    }

    /**
     * 检测鼠标是否点击在下拉建议上，由 Panel 提前调用以绕过反序迭代问题
     */
    public boolean handleSuggestionClick(double mouseX, double mouseY) {
        if (suggestions.isEmpty()) return false;

        int x = getX();
        int y = getY() + getHeight() + 2;
        int width = getWidth();
        int visibleCount = Math.min(suggestions.size(), MAX_VISIBLE_SUGGESTIONS);
        int dropdownHeight = visibleCount * SUGGESTION_HEIGHT + 2;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y + 1 && mouseY < y + dropdownHeight) {
            int clickedIdx = (int) ((mouseY - y - 1) / SUGGESTION_HEIGHT) + suggestionScrollOffset;
            if (clickedIdx >= 0 && clickedIdx < suggestions.size()) {
                setValue(suggestions.get(clickedIdx));
                suggestions = List.of();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleSuggestionClick(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused() || suggestions.isEmpty()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == 257 || keyCode == 335) { // ENTER
            if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.size()) {
                setValue(suggestions.get(selectedSuggestionIndex));
                suggestions = List.of();
                return true;
            }
        }

        if (keyCode == 264) { // DOWN
            selectedSuggestionIndex = Math.min(selectedSuggestionIndex + 1, suggestions.size() - 1);
            // 自动滚动
            if (selectedSuggestionIndex >= suggestionScrollOffset + MAX_VISIBLE_SUGGESTIONS) {
                suggestionScrollOffset = selectedSuggestionIndex - MAX_VISIBLE_SUGGESTIONS + 1;
            }
            return true;
        }

        if (keyCode == 265) { // UP
            selectedSuggestionIndex = Math.max(selectedSuggestionIndex - 1, 0);
            if (selectedSuggestionIndex < suggestionScrollOffset) {
                suggestionScrollOffset = selectedSuggestionIndex;
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            suggestions = List.of();
        }
    }
}
