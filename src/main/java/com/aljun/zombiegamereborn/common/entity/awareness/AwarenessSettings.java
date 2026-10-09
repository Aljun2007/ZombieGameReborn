package com.aljun.zombiegamereborn.common.entity.awareness;

/**
 * 感知系统的参数集合。纯数据袋，由配置层（{@code ZombieProperty} / {@code GamePropertyRefresher}）填充，
 * 然后整体交给 {@link AwarenessTuning#apply} 原子替换。
 *
 * <p>刻意做成「整对象替换」而不是「逐个字段改」：热路径读取时永远看到一个自洽的快照，
 * 不会出现“半径已更新、寿命还没更新”的中间态；也不需要任何锁。
 */
public final class AwarenessSettings {

    // ---- 总开关 ----
    public boolean enabled = true;

    // ---- 轮询与预算 ----
    /** 僵尸轮询感知的间隔（tick），内部会按实体 id 错峰。 */
    public int pollIntervalTicks = 10;
    /** 每 tick 全局允许传播的警报数上限。 */
    public int alertBudgetPerTick = 8;
    /** 警报最大跳数。 */
    public int maxAlertGeneration = 2;
    /** 单只僵尸两次警报之间的最小间隔（tick）。 */
    public int alertCooldownTicks = 60;

    // ---- 通道：声音 ----
    public double soundRadius = 48.0D;
    public int soundStrength = 10;
    /** 是否把僵尸自身发出的声音也算作刺激（默认关，否则僵尸群会互相“听到”彼此而暴走）。 */
    public boolean hearOwnKind = false;

    // ---- 通道：冲击（爆炸 / 方块破碎） ----
    public double impactRadius = 64.0D;
    public int impactStrength = 20;

    // ---- 通道：光照 ----
    public double lightRadius = 24.0D;
    public int lightStrength = 6;

    // ---- 通道：警报 ----
    public double alertRadius = 24.0D;
    public int alertStrength = 8;

    // ---- 通道：气味 ----
    /** 气味衰减到 0 所需 tick。 */
    public int scentDecayTicks = 600;
    /** 每次沉积的强度（玩家）。 */
    public float scentDepositPlayer = 1.0F;
    /** 每次沉积的强度（其他生物，通常更淡）。 */
    public float scentDepositMob = 0.35F;
    /** 沉积节流间隔（tick），避免每 tick 写网格。 */
    public int scentDepositIntervalTicks = 10;
    /** 跟随气味时，距离多近算“闻到了”。 */
    public double scentFollowRadius = 24.0D;
    /** 追击气味时是否允许把玩家当作目标。 */
    public boolean scentRevealsTarget = true;

    // ---- 调查行为 ----
    public int investigateTimeoutTicks = 200;
    public double investigateArriveDistance = 2.0D;
    public double investigateSpeed = 1.0D;
    /** 调查途中重新寻路的间隔（tick），避免每 tick 触发寻路。 */
    public int investigateRepathIntervalTicks = 20;
    /** 到达后原地观察的时长（tick）。 */
    public int investigateLookAroundTicks = 60;
}
