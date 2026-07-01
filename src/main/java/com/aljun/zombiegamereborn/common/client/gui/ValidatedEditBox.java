package com.aljun.zombiegamereborn.common.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 带验证功能的 EditBox
 * <p>
 * 设计理念：
 * - 将验证逻辑封装在组件内部
 * - 支持三种类型：String、Integer、Double
 * - 提供统一的验证接口
 * - 失去焦点时自动纠正无效输入
 */
@OnlyIn(Dist.CLIENT)
public class ValidatedEditBox extends EditBox {

    private final EditType type;
    private final double minValue;
    private final double maxValue;
    private Object lastValidValue;
    private boolean hasValidInput = true;

    /**
     * 编辑器类型枚举
     */
    public enum EditType {
        STRING,
        INTEGER,
        DOUBLE
    }

    /**
     * 构造函数 - String 类型
     */
    public static ValidatedEditBox createStringEditBox(Font font, int x, int y, int width, int height, Component label, String defaultValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.STRING, 0, 0, defaultValue);
    }

    /**
     * 构造函数 - Integer 类型
     */
    public static ValidatedEditBox createIntEditBox(Font font, int x, int y, int width, int height, Component label, int defaultValue, int minValue, int maxValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.INTEGER, minValue, maxValue, defaultValue);
    }

    /**
     * 构造函数 - Double 类型
     */
    public static ValidatedEditBox createDoubleEditBox(Font font, int x, int y, int width, int height, Component label, double defaultValue, double minValue, double maxValue) {
        return new ValidatedEditBox(font, x, y, width, height, label, EditType.DOUBLE, minValue, maxValue, defaultValue);
    }

    private ValidatedEditBox(Font font, int x, int y, int width, int height, Component label, EditType type, double minValue, double maxValue, Object defaultValue) {
        super(font, x, y, width, height, label);
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

    /**
     * 设置验证逻辑
     */
    private void setupValidation() {
        // setResponder: 实时验证，但不阻止输入
        this.setResponder((newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                hasValidInput = false;
                return;
            }

            hasValidInput = validate(newValue);
        });

        // setFormatter: 失去焦点时强制纠正
        this.setFormatter((text, cursorPos) -> {
            if (text == null || text.isEmpty()) {
                return Component.literal(String.valueOf(lastValidValue)).getVisualOrderText();
            }

            if (validate(text)) {
                lastValidValue = parseValue(text);
                hasValidInput = true;
                return Component.literal(text).getVisualOrderText();
            } else {
                hasValidInput = false;
                String fallback = String.valueOf(lastValidValue);
                this.setValue(fallback);
                return Component.literal(fallback).getVisualOrderText();
            }
        });
    }

    /**
     * 验证输入值
     * 
     * @param input 用户输入
     * @return 是否有效
     */
    public boolean validate(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        try {
            switch (type) {
                case STRING:
                    return true; // String 类型总是有效

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

    /**
     * 解析字符串为对应类型的值
     */
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

    /**
     * 获取最后有效的值（原始字符串）
     */
    public String getLastValidStringValue() {
        return String.valueOf(lastValidValue);
    }

    /**
     * 获取最后有效的整数值
     */
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

    /**
     * 获取最后有效的浮点数值
     */
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

    /**
     * 检查当前输入是否有效
     */
    public boolean hasValidInput() {
        return hasValidInput;
    }

    /**
     * 获取编辑器类型
     */
    public EditType getType() {
        return type;
    }

    /**
     * 获取最小值（仅对数值类型有效）
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * 获取最大值（仅对数值类型有效）
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * 重置为默认值
     */
    public void resetToDefault() {
        setValue(String.valueOf(lastValidValue));
        hasValidInput = true;
    }
}
