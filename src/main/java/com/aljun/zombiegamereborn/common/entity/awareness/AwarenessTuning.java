package com.aljun.zombiegamereborn.common.entity.awareness;

/**
 * 感知系统的全局调参出口（热路径统一从这里读）。
 *
 * <p>内部持有一个不可变语义的 {@link AwarenessSettings} 快照，配置变化时<b>整体替换</b>。
 * 引用是 {@code volatile}，所以服务端 tick 线程无需加锁即可读到自洽的参数。
 */
public final class AwarenessTuning {

    /** 默认快照。字段是 public 的，但约定：apply 之后不得再修改已发布的对象。 */
    private static final AwarenessSettings DEFAULT = new AwarenessSettings();

    private static volatile AwarenessSettings current = DEFAULT;

    /**
     * 感知链路调试日志开关。
     * <p>
     * 用系统属性而不是游戏配置：这是排查工具，不是玩法参数，不应该出现在玩家的配置界面里。
     * 开发环境用法：{@code gradlew runServer -Dzgr.awareness.debug=true}
     */
    private static final boolean DEBUG_LOG = Boolean.getBoolean("zgr.awareness.debug");

    private AwarenessTuning() {
    }

    public static boolean debugLog() {
        return DEBUG_LOG;
    }

    public static AwarenessSettings settings() {
        return current;
    }

    /** 用一份新构建的设置整体替换当前参数。 */
    public static void apply(AwarenessSettings settings) {
        current = settings == null ? DEFAULT : settings;
    }

    public static void resetToDefault() {
        current = DEFAULT;
    }

    public static boolean enabled() {
        return current.enabled;
    }

    public static float channelWeight(AwarenessChannel channel) {
        // 权重不暴露给玩家，按通道固定：冲击 > 声音 > 气味 > 警报 > 光照
        return switch (channel) {
            case IMPACT -> 1.6F;
            case SOUND -> 1.0F;
            case SCENT -> 0.8F;
            case ALERT -> 0.6F;
            case LIGHT -> 0.5F;
        };
    }

    public static int pollIntervalTicks() {
        return Math.max(1, current.pollIntervalTicks);
    }

    public static int scentDecayTicks() {
        return Math.max(20, current.scentDecayTicks);
    }

    public static int alertBudgetPerTick() {
        return Math.max(0, current.alertBudgetPerTick);
    }

    public static int maxAlertGeneration() {
        return Math.max(0, current.maxAlertGeneration);
    }

    public static int alertCooldownTicks() {
        return Math.max(0, current.alertCooldownTicks);
    }
}
