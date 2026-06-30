package com.aljun.zombiegamereborn.api;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombiePlaceBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@SuppressWarnings("all")
public class ZGRZombieControlAPI {

    // ==================== IZombieData 直通版本 ====================

    @Nullable
    public static ZombieBreakBlockGoal getBreakPlaceGoal(IZombieData data) {
        return data.getZombieBreakBlockGoal();
    }

    @Nullable
    public static ZombiePlaceBlockGoal getPlaceBlockGoal(IZombieData data) {
        return data.getZombiePlaceBlockGoal();
    }

    @Nullable
    public static ZombieSenseTargetGoal getZombieSenseTargetGoal(IZombieData data) {
        return data.getZombieSenseTargetGoalGoal();
    }

    public static boolean startBreakBlock(IZombieData data, BlockPos pos) {
        ZombieBreakBlockGoal breakGoal = data.getZombieBreakBlockGoal();
        if (breakGoal != null) {
            return breakGoal.tryToBreak(pos);
        }
        return false;
    }

    public static void stopBreakBlock(IZombieData data) {
        ZombieBreakBlockGoal breakGoal = data.getZombieBreakBlockGoal();
        if (breakGoal != null) {
            breakGoal.stopBreak();
        }
    }

    public static void failBreakBlock(IZombieData data) {
        ZombieBreakBlockGoal breakGoal = data.getZombieBreakBlockGoal();
        if (breakGoal != null) {
            breakGoal.failBreak();
        }
    }

    public static boolean startPlaceBlock(IZombieData data, BlockPos blockPos, BlockState blockState) {
        ZombiePlaceBlockGoal placeGoal = data.getZombiePlaceBlockGoal();
        if (placeGoal != null) {
            return placeGoal.place(blockPos, blockState);
        }
        return false;
    }

    // ==================== Zombie 包装版本（向后兼容） ====================

    public static boolean startBreakBlock(Zombie zombie, BlockPos pos) {
        ZombieBreakBlockGoal breakGoal = getBreakPlaceGoal(zombie);
        return breakGoal != null && breakGoal.tryToBreak(pos);
    }

    public static boolean startBreakBlock(Zombie zombie, BlockPos pos, BiConsumer<Zombie, IZombieData> halfwayFailureCallback) {
        ZombieBreakBlockGoal breakGoal = getBreakPlaceGoal(zombie);
        return breakGoal != null && breakGoal.tryToBreak(pos, halfwayFailureCallback);
    }

    public static void stopBreakBlock(Zombie zombie) {
        ZombieBreakBlockGoal breakGoal = getBreakPlaceGoal(zombie);
        if (breakGoal != null) {
            breakGoal.stopBreak();
        }
    }

    public static boolean startPlaceBlock(Zombie zombie, BlockPos blockPos, BlockState blockState) {
        ZombiePlaceBlockGoal placeGoal = getPlaceBlockGoal(zombie);
        return placeGoal != null && placeGoal.place(blockPos, blockState);
    }

    public static boolean startPlaceBlock(Zombie zombie, BlockPos blockPos) {
        ZombiePlaceBlockGoal placeGoal = getPlaceBlockGoal(zombie);
        return placeGoal != null && placeGoal.place(blockPos, placeGoal.getPlaceBlock());
    }

    public static ZombieBreakBlockGoal getBreakPlaceGoal(Zombie zombie) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        return data.getZombieBreakBlockGoal();
    }

    public static ZombiePlaceBlockGoal getPlaceBlockGoal(Zombie zombie) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        return data.getZombiePlaceBlockGoal();
    }

    public static Goal getGoal(Zombie zombie, Predicate<Goal> filter) {
        final Goal[] goal = {null};
        zombie.goalSelector.getAvailableGoals().forEach(wrappedGoal -> {
            if (goal[0] == null) {
                if (filter.test(wrappedGoal.getGoal())) {
                    goal[0] = wrappedGoal.getGoal();
                }
            }
        });
        return goal[0];
    }

    public static void failBreakBlock(Zombie zombie) {
        ZombieBreakBlockGoal breakGoal = getBreakPlaceGoal(zombie);
        if (breakGoal != null) {
            breakGoal.failBreak();
        }
    }

    public static final double REACH_DISTANCE = 3.0;
    public static final double REACH_DISTANCE_TO_SQR = REACH_DISTANCE * REACH_DISTANCE;
}
