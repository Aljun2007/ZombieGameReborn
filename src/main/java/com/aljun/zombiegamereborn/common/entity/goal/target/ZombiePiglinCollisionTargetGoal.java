package com.aljun.zombiegamereborn.common.entity.goal.target;

import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.network.packet.AdvancementHandler;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class ZombiePiglinCollisionTargetGoal extends TargetGoal {

    private static final double ALERT_RADIUS = 16.0;
    private static final long COOLDOWN_TICKS = 40;
    private static final double COLLISION_RADIUS = 1;
    private static final int CHECK_INTERVAL = 10;

    private long lastAlertTime = 0;
    private long nextCheckTick = 0;

    public ZombiePiglinCollisionTargetGoal(Zombie zombie) {
        super(zombie, true);
    }

    @Override
    public boolean canUse() {
        if (!(this.mob instanceof ZombifiedPiglin)) return false;
        if (!this.mob.isAlive()) return false;

        long gameTime = this.mob.level().getGameTime();
        if (gameTime < this.nextCheckTick) return false;
        this.nextCheckTick = gameTime + CHECK_INTERVAL;

        MinecraftServer server = this.mob.getServer();
        if (server == null) return false;
        var zombieProperty = ZGRGame.getGameProperty().getStageProperty((ServerLevel) this.mob.level(),this.mob.blockPosition()).zombieProperty;
        if (!zombieProperty.enablePiglinCollisionAnger) return false;

        if (gameTime - this.lastAlertTime < COOLDOWN_TICKS) return false;

        if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) return false;

        Player squeezer = findCollidingRunner();
        if (squeezer == null) return false;

        this.lastAlertTime = gameTime;

        if (this.mob.getRandom().nextFloat() >= zombieProperty.piglinCollisionAngerChance) return false;

        AdvancementHandler.grantPiglinCollision((ServerPlayer) squeezer);

        this.mob.setTarget(squeezer);
        alertNearbyPiglins(squeezer);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return target instanceof Player player
                && ZombieUtils.isTargetLegal(player)
                && this.mob.distanceToSqr(player) <= ALERT_RADIUS * ALERT_RADIUS;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        LivingEntity target = this.mob.getTarget();
        if (!ZombieUtils.isTargetLegal(target)) {
            this.mob.setTarget(null);
        }
    }

    @Nullable
    private Player findCollidingRunner() {
        return this.mob.level().getNearestPlayer(
                this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                COLLISION_RADIUS,
                p -> p instanceof Player player
                        && ZombieUtils.isTargetLegal(player)
                        && !player.isShiftKeyDown()
        );
    }

    private void alertNearbyPiglins(LivingEntity target) {
        AABB alertBox = this.mob.getBoundingBox().inflate(ALERT_RADIUS);
        List<ZombifiedPiglin> nearby = this.mob.level().getEntitiesOfClass(
                ZombifiedPiglin.class, alertBox,
                piglin -> piglin != this.mob && piglin.isAlive() && piglin.getTarget() == null
        );
        for (ZombifiedPiglin piglin : nearby) {
            piglin.setTarget(target);
        }
    }
}
