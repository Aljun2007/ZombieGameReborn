package com.aljun.zombiegamereborn.common.entity.zombieType.type;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZombieType;
import net.minecraft.world.entity.monster.Zombie;

public class DummyType extends ZombieType {
    /**
     * 创建僵尸类型
     *
     */
    public DummyType() {
        super(ZGRZombieTypes.IDs.DUMMY_ID);
    }

    @Override
    public void onInitializeZombieGoals(Zombie zombie, IZombieData data) {
        zombie.targetSelector.removeAllGoals(goal -> true);
        zombie.goalSelector.removeAllGoals(goal -> true);
    }
}
