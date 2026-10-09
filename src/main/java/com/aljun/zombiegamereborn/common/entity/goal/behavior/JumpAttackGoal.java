package com.aljun.zombiegamereborn.common.entity.goal.behavior;


import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;

public class JumpAttackGoal extends Goal {
    private final Zombie zombie;
    private LivingEntity target;
    private int cooldown = 0;

    // 跳跃参数
    private static final double MIN_HEIGHT_DIFF = 0.5;
    private static final double MAX_HEIGHT_DIFF = 2.5;
    private static final double MIN_HORIZONTAL_DIST = 0.5;
    private static final double MAX_HORIZONTAL_DIST = 4.0;

    public JumpAttackGoal(Zombie zombie) {
        this(zombie, 1.0, 20);
    }

    public JumpAttackGoal(Zombie zombie, double jumpSpeed, int jumpCooldown) {
        this.zombie = zombie;
    }

    @Override
    public boolean canUse() {
        return this.shouldJump();
    }

    @Override
    public void tick() {
        // 更新冷却
        if (this.cooldown > 0) {
            this.cooldown--;
        }

        // 如果还没跳跃，执行跳跃
        if (this.zombie.onGround() && this.target != null) {
            this.zombie.getJumpControl().jump();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * 判断是否应该跳跃
     */
    private boolean shouldJump() {
        if (this.target == null) {
            return false;
        }

        // 计算水平距离
        double dx = this.target.getX() - this.zombie.getX();
        double dz = this.target.getZ() - this.zombie.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // 水平距离太远或太近都不跳
        if (horizontalDist < MIN_HORIZONTAL_DIST || horizontalDist > MAX_HORIZONTAL_DIST) {
            return false;
        }

        // 计算高度差
        double heightDiff = this.target.getY() - this.zombie.getY();

        // 目标不在上方或太高/太低
        if (heightDiff < MIN_HEIGHT_DIFF || heightDiff > MAX_HEIGHT_DIFF) {
            return false;
        }

        return true;
    }
}