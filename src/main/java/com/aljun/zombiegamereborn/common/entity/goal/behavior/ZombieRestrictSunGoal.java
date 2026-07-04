package com.aljun.zombiegamereborn.common.entity.goal.behavior;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieRestrictSunGoal extends RestrictSunGoal {
    private final Zombie zombie;
    private final IZombieData data;

    public ZombieRestrictSunGoal(Zombie zombie, IZombieData data) {
        super(zombie);
        this.zombie = zombie;
        this.data = data;
    }

    @Override
    public boolean canUse() {
        // 检查僵尸是否对阳光敏感，如果不敏感则不执行此目标
        if (!ZGRZombieAttributesAPI.isSunSensitive(zombie)) {
            return false;
        }

        // 如果存在攻击目标，根据僵尸类型和距离判断是否回避阳光
        if (this.zombie.getTarget() != null) {
            ZombieType zombieType = data.getType();

            if (zombieType == null || zombieType.onlyMelee()) {
                // 纯近战僵尸在有目标时始终不回避阳光（优先攻击）
                return false;
            }
        }

        // 默认行为：检查是否在阳光下等条件
        return super.canUse();
    }

}
