package com.aljun.zombiegamereborn.common.player;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家地表检测工具类
 * 使用5秒滑动窗口比例检验判断玩家是否在地表
 */
public class PlayerStatic {

    private static final Map<UUID, SlidingWindowData> playerDataMap = new ConcurrentHashMap<>();

    // 配置参数
    private static final int WINDOW_SIZE = 25;
    private static final int CHECK_INTERVAL_TICKS = 2;
    private static final double SURFACE_THRESHOLD = 0.6;
    private static final int SAMPLE_RADIUS = 10;
    private static final int SAMPLE_COUNT = 12;
    private static final int DEPTH_THRESHOLD = 15; //地表15格以下被视为地下

    /**
     * 判断玩家是否在主世界地表
     * @param player 玩家实例
     * @return true=主世界地表, false=主世界地下/洞穴, 或非主世界
     */
    public static boolean isOnSurfaceOfOverworld(Player player) {
        if (player == null) return false;

        UUID uuid = player.getGameProfile().getId();
        SlidingWindowData data = playerDataMap.computeIfAbsent(uuid, k -> new SlidingWindowData(WINDOW_SIZE));

        // 检查是否需要进行新的检测（每2刻检测一次）
        int currentTick = player.tickCount;
        boolean isSurface;

        if (currentTick - data.lastCheckTick >= CHECK_INTERVAL_TICKS) {
            // 执行检测（无论是否主世界，都记录结果）
            isSurface = detect(player);
            data.addResult(isSurface);
            data.lastCheckTick = currentTick;
        }

        // 冷启动：窗口数据不足5个时使用单次检测结果
        if (data.windowSize() < 5) {
            return data.getLastResult();
        }

        return data.isSurfaceByRatio(SURFACE_THRESHOLD);
    }

    /**
     * 核心检测逻辑
     * @param player 玩家实例
     * @return true=地表, false=地下/非主世界
     */
    private static boolean detect(Player player) {
        // 非主世界 → false（地下/洞穴状态）
        if (player.level().dimension() != Level.OVERWORLD) {
            return false;
        }

        return detectSingleSample(player);
    }

    /**
     * 单次地表检测（仅主世界）
     * 只判断头顶是否有遮挡，不受时间/天气影响
     */
    private static boolean detectSingleSample(Player player) {
        Level level = player.level();

        // ----- 阶段一：射线检测头顶是否有遮挡 -----
        Vec3 start = player.getEyePosition();
        Vec3 end = new Vec3(start.x, level.getMaxBuildHeight(), start.z);
        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,    // 检测碰撞箱
                ClipContext.Fluid.NONE,        // 忽略液体
                player
        );
        BlockHitResult result = level.clip(context);

        // 如果射线没有碰到任何方块 → 头顶无遮挡 → 地表
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }

        // 如果射线碰到了方块，但方块是液体（比如水面上），也视为地表
        BlockState hitState = level.getBlockState(result.getBlockPos());
        if (hitState.getBlock() instanceof LiquidBlock) {
            return true;
        }

        // ----- 阶段二：随机地表采样（头顶有遮挡时执行）-----
        // 注意：头顶有遮挡不一定就是地下，可能是树下、屋檐下
        // 通过采样附近地形高度来进一步判断
        BlockPos playerPos = player.blockPosition();
        List<Integer> surfaceHeights = new ArrayList<>(SAMPLE_COUNT);
        Random random = new Random();
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            int dx = random.nextInt(SAMPLE_RADIUS * 2 + 1) - SAMPLE_RADIUS;
            int dz = random.nextInt(SAMPLE_RADIUS * 2 + 1) - SAMPLE_RADIUS;
            BlockPos samplePos = playerPos.offset(dx, 0, dz);

            for (int y = maxBuildHeight; y >= minBuildHeight; y--) {
                BlockPos checkPos = new BlockPos(samplePos.getX(), y, samplePos.getZ());
                if (!level.isEmptyBlock(checkPos)) {
                    surfaceHeights.add(y);
                    break;
                }
            }
        }

        if (surfaceHeights.isEmpty()) {
            return false;
        }

        Collections.sort(surfaceHeights);
        double medianHeight;
        int size = surfaceHeights.size();
        if (size % 2 == 0) {
            medianHeight = (surfaceHeights.get(size / 2 - 1) + surfaceHeights.get(size / 2)) / 2.0;
        } else {
            medianHeight = surfaceHeights.get(size / 2);
        }

        // 玩家Y >= 中位数 - 阈值 → 地表（可能在山谷/凹陷处）
        return player.getY() >= (medianHeight - DEPTH_THRESHOLD);
    }

    /**
     * 重置所有玩家数据（世界重载时调用）
     */
    public static void resetAllData() {
        playerDataMap.clear();
    }

    /**
     * 重置指定玩家的数据
     */
    public static void resetPlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    /**
     * 滑动窗口数据类
     */
    private static class SlidingWindowData {
        private final Queue<Boolean> window;
        private final int maxSize;
        private int trueCount;
        public int lastCheckTick;

        private SlidingWindowData(int size) {
            this.maxSize = size;
            this.window = new LinkedList<>();
            this.trueCount = 0;
            this.lastCheckTick = -100;
        }

        private void addResult(boolean result) {
            if (window.size() == maxSize) {
                Boolean oldest = window.poll();
                if (oldest != null && oldest) {
                    trueCount--;
                }
            }
            window.add(result);
            if (result) {
                trueCount++;
            }
        }

        private double getRatio() {
            if (window.isEmpty()) {
                return 0.0;
            }
            return (double) trueCount / window.size();
        }

        private boolean isSurfaceByRatio(double threshold) {
            return getRatio() >= threshold;
        }

        private int windowSize() {
            return window.size();
        }

        private boolean getLastResult() {
            if (window.isEmpty()) {
                return false;
            }
            return ((LinkedList<Boolean>) window).getLast();
        }
    }
}