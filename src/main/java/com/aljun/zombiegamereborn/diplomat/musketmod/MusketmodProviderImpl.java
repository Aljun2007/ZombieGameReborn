package com.aljun.zombiegamereborn.diplomat.musketmod;

import ewewukek.musketmod.Config;
import ewewukek.musketmod.RangedGunAttackGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;

public class MusketmodProviderImpl implements IMusketmodProvider {

    @Override
    public Goal createZombieGunGoal(Zombie zombie) {
        return new RangedGunAttackGoal<Zombie>(zombie) {
            private int seeTime;
            private int attackDelay;
            private int strafingTime = -1;
            private boolean strafingClockwise;
            private boolean strafingBackwards;

            @Override
            public boolean canContinueToUse() {
                return (isTargetValid() || !this.mob.getNavigation().isDone()) && canUseGun();
            }

            @Override
            public void start() {
                super.start();
                this.mob.setAggressive(true);
            }

            @Override
            public void stop() {
                super.stop();
                this.seeTime = 0;
                this.attackDelay = 0;
                this.strafingTime = -1;
            }

            @Override
            public void tick() {
                super.tick();
                LivingEntity target = this.mob.getTarget();
                if (target == null) return;

                boolean canSee = this.mob.getSensing().hasLineOfSight(target);
                boolean wasSeeing = this.seeTime > 0;
                if (canSee != wasSeeing) {
                    this.seeTime = 0;
                }
                if (canSee) {
                    ++this.seeTime;
                } else {
                    --this.seeTime;
                }

                float dist = this.mob.distanceTo(target);
                if (dist < 15.0F && this.seeTime >= 20) {
                    this.mob.getNavigation().stop();
                    ++this.strafingTime;
                } else {
                    this.mob.getNavigation().moveTo(target, 1.0);
                    this.strafingTime = -1;
                }

                if (this.strafingTime >= 20) {
                    if (this.mob.getRandom().nextFloat() < 0.3F) {
                        this.strafingClockwise = !this.strafingClockwise;
                    }
                    if (this.mob.getRandom().nextFloat() < 0.3F) {
                        this.strafingBackwards = !this.strafingBackwards;
                    }
                    this.strafingTime = 0;
                }

                if (this.strafingTime > -1) {
                    if (dist > 11.25F) {
                        this.strafingBackwards = false;
                    } else if (dist < 3.75F) {
                        this.strafingBackwards = true;
                    }
                    this.mob.getMoveControl().strafe(
                            this.strafingBackwards ? -0.5F : 0.5F,
                            this.strafingClockwise ? 0.5F : -0.5F
                    );
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }

                if (this.seeTime < -60) {
                    this.attackDelay = Math.max(20, this.attackDelay);
                }

                if (this.attackDelay > 0) {
                    --this.attackDelay;
                } else if (isReady()) {
                    if (canSee) {
                        this.fire(6.0F);
                        this.attackDelay = 20;
                    }
                } else {
                    reload();
                }
            }

            @Override
            public void onReady() {
                this.attackDelay = Math.max(40, this.attackDelay);
            }
        };
    }

    @Override
    public void setMobDamageMultiplier(float multiplier) {
        Config.mobDamageMultiplier = multiplier;
    }
}
