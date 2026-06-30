package com.aljun.zombiegamereborn.common.entity.sense;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;


public class ZombieSenseManager {
    public static void refresh(ZombieProperty zombieProperty) {
        SenseType.BLEEDING.refresh(zombieProperty.senseBleedingRadius, zombieProperty.senseBleedingLifespan);
        SenseType.BLOCK.refresh(zombieProperty.senseBlockRadius, zombieProperty.senseBlockLifespan);
        SenseType.GUN_SHOT.refresh(zombieProperty.senseGunShotRadius, zombieProperty.senseGunShotLifespan);
        SenseType.GUN_SHOT_SILENCED.refresh(zombieProperty.senseGunShotSilencedRadius, zombieProperty.senseGunShotSilencedLifespan);
    }
    private static final double MAX_SENSE_RADIUS = 256.0;
    public static void broadcastSense(LivingEntity entity, Level level, SenseType senseType) {
        AABB area = entity.getBoundingBox().inflate(MAX_SENSE_RADIUS);
        for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, area)) {
            IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
            ZombieSenseTargetGoal goal = data.getZombieSenseTargetGoalGoal();
            if (goal != null) {
                goal.sense(entity, senseType);
            }
        }
    }
}
