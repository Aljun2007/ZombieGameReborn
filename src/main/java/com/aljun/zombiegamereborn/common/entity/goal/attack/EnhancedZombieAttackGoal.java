package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * 僵尸近战攻击目标
 * 基于原版 MeleeAttackGoal 优化，增强近距离追踪能力
 */
public class EnhancedZombieAttackGoal extends Goal {
    
    protected static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    
    protected final Mob zombie;
    protected double speedModifier;

    protected double getSpeedModifier() {
        return speedModifier;
    }
    protected final boolean followingTargetEvenIfNotSeen;
    
    protected Path path;
    protected double pathedTargetX;
    protected double pathedTargetY;
    protected double pathedTargetZ;
    
    protected int ticksUntilNextPathRecalculation;
    protected int ticksUntilNextAttack;
    protected final int attackInterval = 20;
    
    protected long lastCanUseCheck;
    protected int failedPathFindingPenalty = 0;
    protected boolean canPenalize = false;

    /**
     * 创建僵尸近战攻击目标
     * 
     * @param zombie 僵尸实体
     * @param speedModifier 移动速度倍数
     * @param followingTargetEvenIfNotSeen 是否在看不见目标时也跟随
     */
    private EnhancedZombieAttackGoal(Mob zombie, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        this.zombie = zombie;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public EnhancedZombieAttackGoal(Mob zombie) {
        this(zombie, 1.0D, !ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(),zombie.blockPosition()).zombieProperty.followMustSee);
    }

    @Override
    public boolean canUse() {
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }
        
        this.lastCanUseCheck = gameTime;
        LivingEntity target = this.zombie.getTarget();
        
        if (target == null) {
            return false;
        }
        
        if (!target.isAlive()) {
            return false;
        }
        
        if (canPenalize) {
            if (--this.ticksUntilNextPathRecalculation <= 0) {
                this.path = this.zombie.getNavigation().createPath(target, 0);
                this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);
                return this.path != null;
            } else {
                return true;
            }
        }
        
        this.path = this.zombie.getNavigation().createPath(target, 0);
        if (this.path != null) {
            return true;
        }
        
        return this.getAttackReachSqr(target) >= this.zombie.distanceToSqr(
            target.getX(), target.getY(), target.getZ()
        );
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.zombie.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
            return false;
        }
        
        if (!this.followingTargetEvenIfNotSeen) {
            return !this.zombie.getNavigation().isDone();
        }
        
        if (!this.zombie.isWithinRestriction(target.blockPosition())) {
            return false;
        }
        
        if (target instanceof Player player) {
            return !player.isSpectator() && !player.isCreative();
        }
        
        return true;
    }

    @Override
    public void start() {
        this.zombie.getNavigation().moveTo(this.path, this.getSpeedModifier());
        this.zombie.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.zombie.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
        }

        this.zombie.setAggressive(false);
        this.zombie.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return;
        }
        
        this.zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        
        // 优化：使用 distanceToSqr 直接计算距离平方
        double distanceSqr = this.zombie.distanceToSqr(target.getX(), target.getY(), target.getZ());
        
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        
        // 优化：近距离时减少路径重新计算间隔，提升追踪精度
        if (this.zombie.position().distanceToSqr(target.position()) <= 25.0D) {
            this.ticksUntilNextPathRecalculation -= 2;
        }
        
        if ((this.followingTargetEvenIfNotSeen || this.zombie.getSensing().hasLineOfSight(target)) 
                && this.ticksUntilNextPathRecalculation <= 0 
                && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D 
                    || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D 
                    || this.zombie.getRandom().nextFloat() < 0.05F)) {
            
            this.pathedTargetX = target.getX();
            this.pathedTargetY = target.getY();
            this.pathedTargetZ = target.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);
            
            if (this.canPenalize) {
                this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                
                if (this.zombie.getNavigation().getPath() != null) {
                    Node finalPathPoint = this.zombie.getNavigation().getPath().getEndNode();
                    if (finalPathPoint != null 
                            && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1) {
                        failedPathFindingPenalty = 0;
                    } else {
                        failedPathFindingPenalty += 10;
                    }
                } else {
                    failedPathFindingPenalty += 10;
                }
            }
            
            // 根据距离调整路径重新计算频率
            if (distanceSqr > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distanceSqr > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            if (!this.zombie.getNavigation().moveTo(target, this.getSpeedModifier())) {
                this.ticksUntilNextPathRecalculation += 15;
            }

            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(target, distanceSqr);
    }

    /**
     * 检查并执行攻击
     */
    protected boolean checkAndPerformAttack(LivingEntity target, double distanceSqr) {
        double attackReachSqr = this.getAttackReachSqr(target);
        
        if (distanceSqr <= attackReachSqr && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.zombie.swing(InteractionHand.MAIN_HAND);
            this.zombie.doHurtTarget(target);
            return true;
        }
        return false;
    }

    /**
     * 重置攻击冷却
     */
    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackInterval);
    }

    /**
     * 获取攻击范围平方
     */
    protected double getAttackReachSqr(LivingEntity target) {
        // 优化：移除不必要的 double 转换
        return this.zombie.getBbWidth() * 2.0F * this.zombie.getBbWidth() * 2.0F + target.getBbWidth();
    }

    /**
     * 是否可以攻击
     */
    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    /**
     * 获取下次攻击的 tick 数
     */
    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    /**
     * 获取攻击间隔
     */
    protected int getAttackInterval() {
        return this.adjustedTickDelay(this.attackInterval);
    }
}
