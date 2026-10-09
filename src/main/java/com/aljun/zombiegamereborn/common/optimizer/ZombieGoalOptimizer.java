package com.aljun.zombiegamereborn.common.optimizer;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;

import static com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes.*;

public class ZombieGoalOptimizer {

    private static int newBuilderQuota = 0;
    private static int newMinerQuota = 0;

    public static void requestForEmpowerment(IZombieData data) {
        if (data.isEmpowered()) return;
        ZombieType type = data.getType();
        if (type.equals(BUILDER) || type.equals(DROWNED_BUILDER)) {
            if (newBuilderQuota > 0) {
                data.setEmpowered(true);
                newBuilderQuota--;
            }
        } else if (type.equals(MINER) || type.equals(DROWNED_MINER)) {
            if (newMinerQuota > 0) {
                data.setEmpowered(true);
                newMinerQuota--;
            }
        }
    }

    public static void update() {
        refreshQuota();
    }

    public static void refreshQuota() {
        var gameProperty = ZGRGame.getGameProperty();
        newBuilderQuota = gameProperty.maxEmpoweredBuilderCount - ZombieStatic.getLastTickEmpoweredBuilderCount();
        newMinerQuota = gameProperty.maxEmpoweredMinerCount - ZombieStatic.getLastTickEmpoweredMinerCount();
    }

}
