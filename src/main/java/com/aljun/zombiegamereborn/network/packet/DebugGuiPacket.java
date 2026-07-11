package com.aljun.zombiegamereborn.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 调试界面数据包（服务端 → 客户端）
 * 
 * 功能：
 * 1. 服务端通知客户端打开指定的调试界面
 * 2. 支持多种界面类型（通过枚举定义）
 * 3. 可携带额外的字符串数据
 * 
 * 传输方向：服务端 → 客户端
 * 
 * 使用示例：
 * <pre>
 * // 打开测试界面
 * DebugGuiPacket packet = new DebugGuiPacket(DebugGuiPacket.GuiType.TEST_SCREEN);
 * ZGRNetwork.sendToClient(packet, player);
 * 
 * // 打开测试界面并携带额外数据
 * DebugGuiPacket packet = new DebugGuiPacket(
 *     DebugGuiPacket.GuiType.TEST_SCREEN, 
 *     "extra_data"
 * );
 * ZGRNetwork.sendToClient(packet, player);
 * </pre>
 */
public class DebugGuiPacket {

    // 仅从服务端发送至客户端！

    /** GUI 类型枚举 */
    private final GuiType guiType;
    
    /** 额外数据（可选，用于传递参数） */
    private final String extraData;

    /**
     * GUI 类型枚举
     * 定义所有支持的界面类型
     */
    public enum GuiType {
        /** 测试界面 */
        TEST_SCREEN("1"),
        LIST_SCREEN("2");

        /** 类型代码（用于网络传输） */
        private final String code;
        
        /** 代码到类型的映射表（用于快速查找） */
        private static final Map<String, GuiType> BY_CODE = new HashMap<>();

        // 静态初始化块：构建代码映射表
        static {
            for (GuiType type : values()) {
                BY_CODE.put(type.code, type);
            }
        }

        /**
         * 构造 GUI 类型
         * 
         * @param code 类型代码
         */
        GuiType(String code) {
            this.code = code;
        }

        /** 获取类型代码 */
        public String getCode() {
            return code;
        }

        /**
         * 从代码获取 GUI 类型
         * 
         * @param code 类型代码
         * @return 对应的 GUI 类型，不存在则返回 TEST_SCREEN
         */
        public static GuiType fromCode(String code) {
            return BY_CODE.getOrDefault(code, TEST_SCREEN);
        }
    }

    /**
     * 构造数据包（无额外数据）
     * 
     * @param guiType GUI 类型
     */
    public DebugGuiPacket(GuiType guiType) {
        this(guiType, "");
    }

    /**
     * 构造数据包（带额外数据）
     * 
     * @param guiType GUI 类型
     * @param extraData 额外数据
     */
    public DebugGuiPacket(GuiType guiType, String extraData) {
        this.guiType = guiType;
        this.extraData = extraData;
    }

    /**
     * 从字节缓冲区解码
     * 
     * @param buf 网络字节缓冲区
     */
    public DebugGuiPacket(FriendlyByteBuf buf) {
        // 读取 GUI 类型代码并转换为枚举
        String code = buf.readUtf();
        this.guiType = GuiType.fromCode(code);
        // 读取额外数据
        this.extraData = buf.readUtf();
    }

    /**
     * 编码到字节缓冲区
     * 
     * @param buf 网络字节缓冲区
     */
    public void toBytes(FriendlyByteBuf buf) {
        // 写入 GUI 类型代码
        buf.writeUtf(this.guiType.getCode());
        // 写入额外数据
        buf.writeUtf(this.extraData);
    }

    /**
     * 处理数据包
     * 在客户端主线程执行，打开对应的界面
     * 
     * @param contextSupplier 网络事件上下文提供者
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        
        // 在主线程执行
        context.enqueueWork(() -> {
            // 确保是服务端→客户端方向
            if (context.getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) {
                openGui();
            }
        });
        
        // 标记数据包已处理
        context.setPacketHandled(true);
    }

    /**
     * 打开对应的 GUI 界面
     * 根据 GUI 类型创建不同的界面
     */
    private void openGui() {

    }
}