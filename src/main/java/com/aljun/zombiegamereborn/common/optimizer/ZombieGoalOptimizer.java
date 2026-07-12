package com.aljun.zombiegamereborn.common.optimizer;

import com.aljun.zombiegamereborn.common.config.StageProperty;
import com.aljun.zombiegamereborn.common.config.ZombieProperty;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import net.minecraft.server.MinecraftServer;

public class ZombieGoalOptimizer {

    private static int newBuilderQuota = 0;
    private static int newMinerQuota = 0;

    public static void requestForEmpowerment(IZombieData data) {
        if (data.isEmpowered()) return;
        if (data.getType() == ZGRZombieTypes.BUILDER) {
            if (newBuilderQuota > 0) {
                data.setEmpowered(true);
                newBuilderQuota--;
            }
        } else if (data.getType() == ZGRZombieTypes.MINER) {
            if (newMinerQuota > 0) {
                data.setEmpowered(true);
                newMinerQuota--;
            }
        }
    }

    public static void refreshQuota() {
        var gameProperty = ZGRGame.getGameProperty();
        newBuilderQuota = gameProperty.maxEmpoweredBuilderCount - ZombieStatic.getEmpoweredBuilderCount();
        newMinerQuota = gameProperty.maxEmpoweredMinerCount - ZombieStatic.getEmpoweredMinerCount();
    }

}
