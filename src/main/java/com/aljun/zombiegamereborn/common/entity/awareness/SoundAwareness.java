package com.aljun.zombiegamereborn.common.entity.awareness;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 把“世界里播放了一次声音”翻译成一条感知刺激。
 *
 * <h2>为什么这里必须极度克制</h2>
 * 这个入口会在<b>每一次</b>服务端播音时被调用（原版 {@code Level.playSound} / {@code playSeededSound}）。
 * 一个热闹的世界每 tick 可能有几十次播音，因此这里任何一次字符串拼接、集合分配或实体查询，
 * 都会被放大成可观的卡顿。所以本方法的规则是：
 * <ol>
 *   <li>先做最便宜的判断：客户端直接返回、总开关、{@link SoundSource} 枚举比较、音量下限；</li>
 *   <li>用 {@code instanceof} 过滤掉僵尸自己发出的声音（可配置），避免僵尸群互相“听见”而持续暴走；</li>
 *   <li>脚步声不计入听觉 —— 走路已经通过气味通道被感知，重复计入只会让刺激数组被脚步刷屏；</li>
 *   <li>只做常量时间的运算，最后交给 {@link LevelAwareness#emit} 做同源合并。</li>
 * </ol>
 */
public final class SoundAwareness {

    /** 低于这个音量视为听不见（原版静音/极轻的循环音）。 */
    private static final float MIN_VOLUME = 0.05F;

    private SoundAwareness() {
    }

    /** 声音类别是否完全忽略。AMBIENT/MUSIC/WEATHER 属于环境音，对僵尸没有信息量。 */
    public static boolean isIgnoredCategory(SoundSource category) {
        return switch (category) {
            case AMBIENT, MUSIC, WEATHER -> true;
            default -> false;
        };
    }

    /**
     * 主入口。
     *
     * @param source 声音的归属实体，可能为 null（纯位置播音）
     */
    public static void onSound(Level level, @Nullable Entity source, double x, double y, double z,
                               @Nullable Holder<SoundEvent> soundHolder, SoundSource category,
                               float volume) {
        if (level.isClientSide()) {
            return;
        }
        AwarenessSettings settings = AwarenessTuning.settings();
        if (!settings.enabled) {
            return;
        }
        if (volume < MIN_VOLUME || isIgnoredCategory(category)) {
            return;
        }
        // 僵尸自己的叫声不吸引同类（可在配置里打开）
        if (!settings.hearOwnKind && source instanceof Zombie) {
            return;
        }

        ResourceLocation id = soundHolder == null ? null : soundHolder.value().getLocation();
        if (id != null) {
            if (isFootstep(id)) {
                return;
            }
            // 同类叫声：原版是按“位置”播放的（playSound(null, x, y, z, ...)），拿不到实体来源，
            // 所以只能在 ID 上过滤。不过滤的话，几百只僵尸每 6~8 秒一次的环境音会把刺激池刷屏。
            if (!settings.hearOwnKind && isOwnKindVocalization(id)) {
                return;
            }
        }

        double baseRadius;
        int baseStrength;
        switch (category) {
            case PLAYERS -> {
                baseRadius = settings.soundRadius;
                baseStrength = settings.soundStrength;
            }
            case HOSTILE -> {
                baseRadius = settings.soundRadius * 0.8D;
                baseStrength = Math.max(1, (int) (settings.soundStrength * 0.8F));
            }
            case NEUTRAL, VOICE -> {
                baseRadius = settings.soundRadius * 0.6D;
                baseStrength = Math.max(1, (int) (settings.soundStrength * 0.6F));
            }
            case BLOCKS -> {
                baseRadius = settings.soundRadius * 0.5D;
                baseStrength = Math.max(1, settings.soundStrength / 2);
            }
            default -> {
                baseRadius = settings.soundRadius * 0.7D;
                baseStrength = Math.max(1, (int) (settings.soundStrength * 0.7F));
            }
        }

        // 原版用 volume > 1 表示“传得更远”，这里做同样的事，但夹住上限防止爆炸音把半径撑爆
        double radius = baseRadius * Math.min(Math.max(volume, 0.25F), 3.0F);
        if (radius <= 0.0D) {
            return;
        }
        int strength = baseStrength;

        // 过载保护：刺激已经很多时，丢弃最弱的那一档，优先保住枪声/爆炸这类强信号
        LevelAwareness awareness = AwarenessManager.of(level);
        if (awareness != null && strength <= 3 && awareness.liveCount() > 24) {
            return;
        }
        AwarenessManager.emitSound(level, x, y, z, radius, strength, source);
    }

    /**
     * 脚步声判定。原版与绝大多数模组的脚步声都注册在 {@code *.step} 路径下，
     * 只做一次后缀比较，开销可以忽略。
     */
    private static boolean isFootstep(ResourceLocation id) {
        String path = id.getPath();
        return path.endsWith(".step") || path.endsWith(".step_") || path.equals("step");
    }

    /** 同类生物的命名空间前缀。{@code entity.zombified_piglin} 已被 {@code entity.zombie} 覆盖。 */
    private static final String[] OWN_KIND_PREFIXES = {
            "entity.zombie", "entity.drowned", "entity.husk", "entity.giant"
    };

    /** 只过滤“叫唤”类声音（环境音 / 受伤 / 死亡），门被撞破这类声音仍然算有效刺激。 */
    private static boolean isOwnKindVocalization(ResourceLocation id) {
        String path = id.getPath();
        boolean ownKind = false;
        for (String prefix : OWN_KIND_PREFIXES) {
            if (path.startsWith(prefix)) {
                ownKind = true;
                break;
            }
        }
        if (!ownKind) {
            return false;
        }
        return path.endsWith(".ambient") || path.endsWith(".hurt") || path.endsWith(".death");
    }
}
