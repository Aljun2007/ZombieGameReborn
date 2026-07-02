package com.aljun.zombiegamereborn.diplomat.musketmod;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import ewewukek.musketmod.Config;
import ewewukek.musketmod.RangedGunAttackGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

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

                int currentRadius = 15;
                if (this.mob.level() instanceof ServerLevel serverLevel) {
                    currentRadius = ZGRGame.getGameProperty()
                            .getStageProperty(serverLevel.getServer())
                            .zombieProperty.musketModGunFireRadius;
                }
                float fireRadiusF = (float) Math.max(1, currentRadius);

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
                if (dist < fireRadiusF && this.seeTime >= 20) {
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
                    if (dist > fireRadiusF * 0.75F) {
                        this.strafingBackwards = false;
                    } else if (dist < fireRadiusF * 0.25F) {
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

    @Override
    public ItemStack getGunStack() {
        return new ItemStack(ewewukek.musketmod.Items.MUSKET);
    }

    @Override
    public ItemStack getAmmoStack() {
        return new ItemStack(ewewukek.musketmod.Items.CARTRIDGE);
    }
}
