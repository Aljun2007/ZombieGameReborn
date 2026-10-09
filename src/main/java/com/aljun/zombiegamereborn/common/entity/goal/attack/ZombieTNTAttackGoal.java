package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

public class ZombieTNTAttackGoal extends EnhancedZombieAttackGoal {
    private static final double THROW_RANGE = 16.0d;
    private static final double EXPLORE_RADIUS = 5.0d;
    private static final double THROW_RANGE_SQR = THROW_RANGE * THROW_RANGE;
    private static final double EXPLORE_RADIUS_SQR = EXPLORE_RADIUS * EXPLORE_RADIUS;
    private static final int COOLDOWN_TICKS = 80;
    private PrimedTnt tnt = null;
    private long lastLitTime = 0;
    private final IZombieData data;

    public boolean canThrowTNT() {
        return this.data.canThrowTNT();
    }

    public ZombieTNTAttackGoal(Mob zombie, IZombieData data) {
        super(zombie);
        this.data = data;
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = zombie.getTarget();
        if (target == null) return;
        double distanceSq = zombie.distanceToSqr(target);
        if (this.tnt == null) {
            // 冷却检查
            if (this.zombie.level().getGameTime() - this.lastLitTime < COOLDOWN_TICKS) return;

            // 没有 TNT → 检查是否要点火
            if (this.canExplode()) {
                if (this.canThrowTNT()) {
                    if (distanceSq <= THROW_RANGE_SQR) {
                        this.lit();
                    }
                } else {
                    if (distanceSq <= EXPLORE_RADIUS_SQR) {
                        this.lit();
                    }
                }
            }
        } else {
            // 已有 TNT → 时机到了就扔出去，只扔一次
            if (this.canThrowTNT()) {
                if (this.tnt.getFuse() * this.tnt.getFuse() + 3 <= distanceSq) {
                    this.throwTNT(target.position());
                }
            }
        }
    }

    private boolean canExplode() {
        return zombie.getServer() != null
                && zombie.getServer().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && zombie.getItemBySlot(EquipmentSlot.HEAD).is(Items.TNT);
    }

    private void lit() {
        this.tnt = new PrimedTnt(zombie.level(), zombie.getX(), zombie.getEyeY(), zombie.getZ(), zombie);
        this.tnt.setFuse(80);
        this.tnt.startRiding(zombie);
        zombie.level().addFreshEntity(this.tnt);
        this.tnt.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        zombie.getItemBySlot(EquipmentSlot.HEAD).shrink(1);
    }

    public void throwTNT(Vec3 targetPos) {

        this.tnt.stopRiding();
        this.tnt.moveTo(this.zombie.getEyePosition());

        double d0 = targetPos.x - this.tnt.getX();
        double d1 = targetPos.y - this.tnt.getY();
        double d2 = targetPos.z - this.tnt.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        // 目标在正上方/下方时防止除零
        if (d3 < 0.01) {
            // 垂直向上抛出
            this.tnt.setDeltaMovement(0, 1.2, 0);
            return;
        }

        // 计算速度向量，类似骷髅射箭的抛物线
        double speed = 1.5;
        double yOffset = d3 * 0.2F;

        double vx = d0 * speed / d3;
        double vy = (d1 + yOffset) * speed / d3;
        double vz = d2 * speed / d3;

        this.tnt.setDeltaMovement(vx, vy, vz);
        this.tnt = null;
        this.lastLitTime = this.zombie.level().getGameTime();
    }

}
