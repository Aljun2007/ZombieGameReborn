package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.zombieType.ZGRZombieTypes;
import net.minecraft.world.entity.monster.Zombie;

import java.util.concurrent.atomic.AtomicInteger;

public class ZombieStatic {
    
    private static final AtomicInteger totalZombieCount = new AtomicInteger(0);
    private static final AtomicInteger empoweredBuilderCount = new AtomicInteger(0);
    private static final AtomicInteger empoweredMinerCount = new AtomicInteger(0);
    
    private static volatile int lastTickZombieCount = 0;
    private static volatile int lastTickEmpoweredBuilderCount = 0;
    private static volatile int lastTickEmpoweredMinerCount = 0;
    
    public static int getTotalZombieCount() {
        return totalZombieCount.get();
    }
    
    public static int getLastTickZombieCount() {
        return lastTickZombieCount;
    }
    
    public static int getEmpoweredBuilderCount() {
        return empoweredBuilderCount.get();
    }
    
    public static int getLastTickEmpoweredBuilderCount() {
        return lastTickEmpoweredBuilderCount;
    }
    
    public static int getEmpoweredMinerCount() {
        return empoweredMinerCount.get();
    }
    
    public static int getLastTickEmpoweredMinerCount() {
        return lastTickEmpoweredMinerCount;
    }
    
    public static void incrementZombieCount(Zombie zombie, IZombieData data) {
        totalZombieCount.incrementAndGet();
        
        if (data.isEmpowered()) {
            if (data.getType() == ZGRZombieTypes.BUILDER) {
                empoweredBuilderCount.incrementAndGet();
            } else if (data.getType() == ZGRZombieTypes.MINER) {
                empoweredMinerCount.incrementAndGet();
            }
        }
    }
    
    public static void resetZombieCount() {
        lastTickZombieCount = totalZombieCount.get();
        lastTickEmpoweredBuilderCount = empoweredBuilderCount.get();
        lastTickEmpoweredMinerCount = empoweredMinerCount.get();
        
        totalZombieCount.set(0);
        empoweredBuilderCount.set(0);
        empoweredMinerCount.set(0);
    }
}
