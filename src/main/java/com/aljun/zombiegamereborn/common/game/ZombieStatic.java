package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ZombieStatic {

    private static final Map<ResourceLocation, AtomicInteger> TOTAL_ZOMBIE_COUNT = new ConcurrentHashMap<>();
    private static final AtomicInteger EMPOWERED_BUILDER_COUNT = new AtomicInteger(0);
    private static final AtomicInteger EMPOWERED_MINER_COUNT = new AtomicInteger(0);

    private static volatile Map<ResourceLocation, Integer> LAST_TICK_ZOMBIE_COUNT = Map.of();
    private static volatile int lastTickEmpoweredBuilderCount = 0;
    private static volatile int lastTickEmpoweredMinerCount = 0;

    private static AtomicInteger getOrCreate(Map<ResourceLocation, AtomicInteger> map, ResourceLocation dim) {
        return map.computeIfAbsent(dim, k -> new AtomicInteger(0));
    }

    public static int getTotalZombieCount(ResourceLocation dim) {
        return getOrCreate(TOTAL_ZOMBIE_COUNT, dim).get();
    }

    public static int getLastTickZombieCount(ResourceLocation dim) {
        return LAST_TICK_ZOMBIE_COUNT.getOrDefault(dim, 0);
    }

    public static int getEmpoweredBuilderCount() {
        return EMPOWERED_BUILDER_COUNT.get();
    }

    public static int getLastTickEmpoweredBuilderCount() {
        return lastTickEmpoweredBuilderCount;
    }

    public static int getEmpoweredMinerCount() {
        return EMPOWERED_MINER_COUNT.get();
    }

    public static int getLastTickEmpoweredMinerCount() {
        return lastTickEmpoweredMinerCount;
    }

    public static int getGlobalTotalZombieCount() {
        return TOTAL_ZOMBIE_COUNT.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public static int getGlobalLastTickZombieCount() {
        return LAST_TICK_ZOMBIE_COUNT.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static void incrementZombieCount(Zombie zombie, IZombieData data) {
        ResourceLocation dim = zombie.level().dimension().location();
        getOrCreate(TOTAL_ZOMBIE_COUNT, dim).incrementAndGet();

        if (data.isEmpowered()) {
            if (data.getType() == ZGRZombieTypes.BUILDER) {
                EMPOWERED_BUILDER_COUNT.incrementAndGet();
            } else if (data.getType() == ZGRZombieTypes.MINER) {
                EMPOWERED_MINER_COUNT.incrementAndGet();
            }
        }
    }

    public static void resetZombieCount() {
        Map<ResourceLocation, Integer> snapshotTotal = new ConcurrentHashMap<>();
        for (ResourceLocation dim : TOTAL_ZOMBIE_COUNT.keySet()) {
            snapshotTotal.put(dim, TOTAL_ZOMBIE_COUNT.get(dim).getAndSet(0));
        }
        LAST_TICK_ZOMBIE_COUNT = snapshotTotal;
        lastTickEmpoweredBuilderCount = EMPOWERED_BUILDER_COUNT.getAndSet(0);
        lastTickEmpoweredMinerCount = EMPOWERED_MINER_COUNT.getAndSet(0);
    }
}
