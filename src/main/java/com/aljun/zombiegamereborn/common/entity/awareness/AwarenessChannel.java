package com.aljun.zombiegamereborn.common.entity.awareness;

/**
 * 感知通道。每个通道代表一类“吸引僵尸”的刺激来源。
 * <p>
 * 设计原则：通道只描述<b>刺激的性质</b>，具体半径/寿命/权重全部来自配置，
 * 不在代码里写死（旧的 {@code SenseType} 把半径写死在静态常量里，再靠 refresh 回写，既绕又容易失配）。
 */
public enum AwarenessChannel {

    /** 声音：脚步以外的可听事件（枪声、爆炸、方块破坏、门、音符盒……）。 */
    SOUND,

    /** 气味：玩家/生物留下的会衰减的轨迹（对应“血腥味”）。 */
    SCENT,

    /** 冲击：爆炸、方块破碎等物理震动，通常范围大、优先级高。 */
    IMPACT,

    /** 警报：同类发现目标后发出的短距通知，带 generation 上限防止链式扩散。 */
    ALERT,

    /** 光照：亮度导致的可见性变化（黑暗中被看得更远 / 亮处更容易暴露）。 */
    LIGHT;

    private static final AwarenessChannel[] VALUES = values();

    public static AwarenessChannel byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : SOUND;
    }
}
