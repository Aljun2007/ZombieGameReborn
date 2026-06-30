package com.aljun.zombiegamereborn.utils;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ZombieDecisionUtils {

    public static boolean isZombieVeryCloseToTarget(@NotNull Zombie zombie, LivingEntity entity) {
        return zombie.distanceToSqr(entity) < ZGRZombieControlAPI.REACH_DISTANCE_TO_SQR;
    }

    public static boolean isTargetLegal(@Nullable Entity entity) {
        if (entity == null) return false;
        boolean b = true;
        if (entity instanceof Player player) {
            b = !player.isCreative() && !player.isSpectator();
        }
        return b && entity.isAlive();
    }
    @SuppressWarnings("all")
    public static boolean zombieAttackableEntity(LivingEntity livingEntity) {
        if (!isTargetLegal(livingEntity)) return false;
        if (livingEntity instanceof Player) return true;
        if (livingEntity instanceof IronGolem) return true;
        if (livingEntity instanceof AbstractVillager) return true;
        if (livingEntity instanceof Turtle turtle&& turtle.isBaby()) return true;
        return false;
    }

    public static int threatLevel(LivingEntity entity) {
        if (entity instanceof Player) return 0;
        if (entity instanceof AbstractVillager || entity instanceof IronGolem) return 1;
        if (entity instanceof Turtle) return 2;
        return 3;
    }

}
