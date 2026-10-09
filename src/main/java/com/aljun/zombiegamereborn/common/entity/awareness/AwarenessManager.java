package com.aljun.zombiegamereborn.common.entity.awareness;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 感知系统对外的唯一入口。
 *
 * <p>调用方（事件处理器、 diplomat 桥接、mixin）只需要说“这里发生了声音/气味/冲击”，
 * <b>不要</b>自己去查询附近僵尸 —— 查询是僵尸的责任，见 {@link LevelAwareness#findStrongest}。
 * 这条纪律是整套性能设计成立的前提。
 */
public final class AwarenessManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 维度 → 状态。按 {@link ResourceKey} 存，避免强引用 Level 造成跨存档泄漏。 */
    private static final Map<ResourceKey<Level>, LevelAwareness> LEVELS = new ConcurrentHashMap<>();

    // 通道位掩码，方便僵尸按自身类型选择关心哪些通道
    public static final long MASK_SOUND = 1L << AwarenessChannel.SOUND.ordinal();
    public static final long MASK_SCENT = 1L << AwarenessChannel.SCENT.ordinal();
    public static final long MASK_IMPACT = 1L << AwarenessChannel.IMPACT.ordinal();
    public static final long MASK_ALERT = 1L << AwarenessChannel.ALERT.ordinal();
    public static final long MASK_LIGHT = 1L << AwarenessChannel.LIGHT.ordinal();
    public static final long MASK_ALL = MASK_SOUND | MASK_SCENT | MASK_IMPACT | MASK_ALERT | MASK_LIGHT;

    private AwarenessManager() {
    }

    @Nullable
    public static LevelAwareness of(@Nullable LevelAccessor level) {
        if (!(level instanceof Level realLevel) || realLevel.isClientSide()) {
            return null;
        }
        return LEVELS.computeIfAbsent(realLevel.dimension(), key -> new LevelAwareness());
    }

    // ------------------------------------------------------------------
    // 写入接口
    // ------------------------------------------------------------------

    /** 声音刺激：某处发出了可听事件。 */
    public static void emitSound(Level level, double x, double y, double z, double radius, int strength,
                                 @Nullable Entity source) {
        emit(level, AwarenessChannel.SOUND, x, y, z, radius, strength, source, 0);
    }

    /** 冲击刺激：爆炸、方块破碎等。 */
    public static void emitImpact(Level level, double x, double y, double z, double radius, int strength,
                                  @Nullable Entity source) {
        emit(level, AwarenessChannel.IMPACT, x, y, z, radius, strength, source, 0);
    }

    /** 光照刺激：亮度变化导致的可感知性。 */
    public static void emitLight(Level level, double x, double y, double z, double radius, int strength,
                                 @Nullable Entity source) {
        emit(level, AwarenessChannel.LIGHT, x, y, z, radius, strength, source, 0);
    }

    /**
     * 警报传播：一只僵尸发现了什么，通知附近同类。
     * <p>
     * 这是唯一会“自传播”的通道，因此有双重限流：每 tick 全局配额 + 跳数上限。
     * 收到警报的僵尸不会无限接力，最多传到 {@code maxAlertGeneration} 跳。
     */
    public static void emitAlert(Level level, double x, double y, double z, double radius, int strength,
                                 @Nullable Entity source, int generation) {
        LevelAwareness awareness = of(level);
        if (awareness == null) return;
        if (generation > AwarenessTuning.maxAlertGeneration()) return;
        if (!awareness.tryConsumeAlertBudget(level.getGameTime(), AwarenessTuning.alertBudgetPerTick())) {
            return;
        }
        emit(level, AwarenessChannel.ALERT, x, y, z, radius, strength, source, generation);
    }

    private static void emit(Level level, AwarenessChannel channel, double x, double y, double z,
                             double radius, int strength, @Nullable Entity source, int generation) {
        if (!AwarenessTuning.enabled()) return;
        LevelAwareness awareness = of(level);
        if (awareness == null) return;
        awareness.emit(channel, x, y, z, radius, strength, defaultLifespan(channel), generation, source,
                level.getGameTime());
        if (AwarenessTuning.debugLog()) {
            LOGGER.info("[ZGR awareness] EMIT {} at ({}, {}, {}) r={} strength={} gen={} source={}",
                    channel, fmt(x), fmt(y), fmt(z), fmt(radius), strength, generation,
                    source == null ? "none" : source.getName().getString());
        }
    }

    private static String fmt(double value) {
        return String.format("%.1f", value);
    }

    private static int defaultLifespan(AwarenessChannel channel) {
        return switch (channel) {
            case SCENT -> AwarenessTuning.scentDecayTicks();
            // 声音/冲击只作为“去调查”的触发点，几十 tick 内有效即可，不需要长期驻留
            case SOUND, IMPACT, LIGHT -> 100;
            case ALERT -> 60;
        };
    }

    /** 写入气味。由节流后的气味发射器调用，不要每 tick 每实体调用。 */
    public static void depositScent(Level level, double x, double y, double z, float amount) {
        if (!AwarenessTuning.enabled()) return;
        LevelAwareness awareness = of(level);
        if (awareness == null) return;
        awareness.scent().deposit(x, y, z, amount, level.getGameTime());
    }

    // ------------------------------------------------------------------
    // 感知反馈音
    // ------------------------------------------------------------------

    /**
     * 重入标记：模组自己播放的音效不能被自己的感知系统听见。
     * <p>
     * 否则会形成正反馈：僵尸 A 因为发现玩家而低吼 → 这声低吼被 B 听见 → B 也低吼 →
     * C 听见 B …… 没有这层保护，再加多少限流都只是把雪崩推迟几秒。
     * 服务端音效播放发生在 tick 线程内且是同步的，所以一个普通字段就够了。
     */
    private static boolean internalSound;

    public static boolean isInternalSound() {
        return internalSound;
    }

    /** 播放感知反馈音（带重入保护）。玩家能听见，但不会产生任何刺激。 */
    public static void playFeedbackSound(Level level, double x, double y, double z,
                                         SoundEvent sound, float volume, float pitch) {
        if (!AwarenessTuning.settings().feedbackSounds) return;
        internalSound = true;
        try {
            level.playSound(null, x, y, z, sound, SoundSource.HOSTILE, volume, pitch);
        } finally {
            internalSound = false;
        }
    }

    // ------------------------------------------------------------------
    // 语义化入口：调用方不要自己算半径，统一在这里调参
    // ------------------------------------------------------------------

    /**
     * 枪声。消音器不是“换个通道”，而是显著缩小半径与强度 —— 这样消音依然有意义，
     * 但不会像旧实现那样变成两套互不相干的常量。
     */
    public static void emitGunShot(Level level, double x, double y, double z, boolean silenced,
                                   @Nullable Entity source) {
        AwarenessSettings settings = AwarenessTuning.settings();
        double radius = silenced ? settings.soundRadius * 0.35D : settings.soundRadius * 1.5D;
        int strength = silenced ? Math.max(1, settings.soundStrength / 3) : settings.soundStrength * 2;
        emit(level, AwarenessChannel.SOUND, x, y, z, radius, strength, source, 0);
    }

    /** 方块被破坏：属于冲击，范围比普通声音大。 */
    public static void emitBlockBreak(Level level, double x, double y, double z, @Nullable Entity source) {
        AwarenessSettings settings = AwarenessTuning.settings();
        emit(level, AwarenessChannel.IMPACT, x, y, z, settings.impactRadius, settings.impactStrength,
                source, 0);
    }

    /** 方块被放置：比破坏弱得多（放置本身很安静）。 */
    public static void emitBlockPlace(Level level, double x, double y, double z, @Nullable Entity source) {
        AwarenessSettings settings = AwarenessTuning.settings();
        emit(level, AwarenessChannel.SOUND, x, y, z, settings.soundRadius * 0.5D,
                Math.max(1, settings.soundStrength / 2), source, 0);
    }

    /** 爆炸：最强的冲击源。 */
    public static void emitExplosion(Level level, double x, double y, double z, @Nullable Entity source) {
        AwarenessSettings settings = AwarenessTuning.settings();
        emit(level, AwarenessChannel.IMPACT, x, y, z, settings.impactRadius * 1.5D,
                settings.impactStrength * 2, source, 0);
    }

    /** 生物受伤：血腥味 + 短促声响。 */
    public static void emitHurt(Level level, double x, double y, double z, @Nullable Entity source) {
        AwarenessSettings settings = AwarenessTuning.settings();
        emit(level, AwarenessChannel.SOUND, x, y, z, settings.soundRadius * 0.5D,
                Math.max(1, settings.soundStrength / 2), source, 0);
        depositScent(level, x, y, z, settings.scentDepositMob * 2.0F);
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    /**
     * 周期性维护。<b>不是</b>感知的主要开销来源：
     * 刺激过期判定和气味压缩都是惰性的，这里只是保证“没有任何僵尸来读”时状态也会被回收。
     */
    public static void maintenance(Level level, long now) {
        LevelAwareness awareness = LEVELS.get(level.dimension());
        if (awareness == null) return;
        awareness.sweep(now);
        awareness.scent().compact(now);
    }

    public static void unload(Level level) {
        LevelAwareness awareness = LEVELS.remove(level.dimension());
        if (awareness != null) {
            awareness.clear();
        }
    }

    public static void shutdown() {
        for (LevelAwareness awareness : LEVELS.values()) {
            awareness.clear();
        }
        LEVELS.clear();
    }

    /** 调试统计。 */
    public static int trackedLevels() {
        return LEVELS.size();
    }
}
