package com.aljun.zombiegamereborn.common.optimizer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 延迟方块操作队列。
 * 将直接的世界方块修改（破坏/放置）延后到区块锁释放后统一执行，
 * 由 {@link com.aljun.zombiegamereborn.common.events.handler.ChunkOperationFlushHandler} 在每个 LevelTickEvent.Phase.END 刷新。
 */
public class ZombieBlockOperationQueue {

    public enum Type {
        BREAK,
        INSTANT_BREAK,
        PLACE
    }

    private record BlockOperation(Type type, BlockPos pos, BlockState state, Zombie zombie,
                                  ItemStack stack, BlockState placeState) {}

    private static final Map<ServerLevel, List<BlockOperation>> QUEUE = new HashMap<>();

    /** 记录已入队的方块位置，防止同一位置被重复入队 */
    private static final Map<ServerLevel, Set<BlockPos>> PENDING_POSITIONS = new HashMap<>();

    // ======================== 入队方法 ========================

    /**
     * 将一个破坏操作入队（带挖掘进度的破坏，需掉落资源）。
     */
    public static void enqueueBreak(ServerLevel level, BlockPos pos, BlockState state,
                                    Zombie zombie, ItemStack stack) {
        if (isPositionPending(level, pos)) return;
        QUEUE.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new BlockOperation(Type.BREAK, pos, state, zombie, stack, null));
        PENDING_POSITIONS.computeIfAbsent(level, k -> new HashSet<>()).add(pos);
    }

    /**
     * 将一个瞬间破坏操作入队（硬度=0的方块，如火把），不经过挖掘进度。
     */
    public static void enqueueInstantBreak(ServerLevel level, BlockPos pos, BlockState state,
                                           Zombie zombie, ItemStack stack) {
        if (isPositionPending(level, pos)) return;
        QUEUE.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new BlockOperation(Type.INSTANT_BREAK, pos, state, zombie, stack, null));
        PENDING_POSITIONS.computeIfAbsent(level, k -> new HashSet<>()).add(pos);
    }

    /**
     * 将一个放置操作入队。
     */
    public static void enqueuePlace(ServerLevel level, BlockPos pos, BlockState placeState) {
        if (isPositionPending(level, pos)) return;
        QUEUE.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new BlockOperation(Type.PLACE, pos, null, null, null, placeState));
        PENDING_POSITIONS.computeIfAbsent(level, k -> new HashSet<>()).add(pos);
    }

    // ======================== 查询方法 ========================

    /**
     * 检查指定位置是否有延迟操作待处理，用于防止重复入队。
     */
    public static boolean isPositionPending(ServerLevel level, BlockPos pos) {
        Set<BlockPos> pending = PENDING_POSITIONS.get(level);
        return pending != null && pending.contains(pos);
    }

    /** 每tick每个世界最多处理的方块操作数，防止单tick方块更新链过多导致卡顿 */
    private static final int MAX_OPERATIONS_PER_TICK = 32;

    // ======================== 刷新执行 ========================

    /**
     * 刷新指定世界中的所有延迟操作。
     * <p>
     * 每个操作执行前会做以下安全校验：
     * <ul>
     *   <li>区块是否已加载 → 未加载则留到下次刷新</li>
     *   <li>僵尸是否还活着 → 已死则丢弃 BREAK 操作</li>
     *   <li>方块状态是否匹配 → 已被破坏/占用则跳过</li>
     * </ul>
     * <p>
     * 性能优化：
     * <ul>
     *   <li>每tick最多处理 {@value #MAX_OPERATIONS_PER_TICK} 个操作，超额留到下次</li>
     *   <li>PLACE 使用 flag 2|8（仅更新客户端 + 无物理），避免邻居更新链</li>
     * </ul>
     */
    public static void flushLevel(ServerLevel level) {
        List<BlockOperation> operations = QUEUE.remove(level);
        if (operations == null) return;

        List<BlockOperation> remaining = new ArrayList<>();
        int processed = 0;

        for (BlockOperation op : operations) {
            // 每tick上限控制：超额部分留到下次
            if (processed >= MAX_OPERATIONS_PER_TICK) {
                remaining.add(op);
                continue;
            }

            // 区块未加载 → 留到下次 LevelTick 再处理
            if (!level.hasChunk(op.pos().getX() >> 4, op.pos().getZ() >> 4)) {
                remaining.add(op);
                continue;
            }

            BlockState currentState = level.getBlockState(op.pos());

            switch (op.type()) {
                case BREAK, INSTANT_BREAK -> {
                    // 僵尸已死 → 丢弃操作，防止掉落物在死亡僵尸位置生成
                    if (op.zombie() != null && !op.zombie().isAlive()) {
                        continue;
                    }
                    // 方块已被其他过程破坏 → 跳过，防止重复掉落
                    if (currentState.isAir()) {
                        continue;
                    }
                    processed++;
                    // 参考 ZombieBreakBlockGoal.succeedBreakBlock: 只有持有正确工具时才掉落资源
                    if (!op.state().requiresCorrectToolForDrops()
                            || op.stack().isCorrectToolForDrops(op.state())) {
                        Block.dropResources(op.state(), level, op.pos(),
                                level.getBlockEntity(op.pos()), op.zombie(), op.stack());
                    }
                    level.destroyBlock(op.pos(), false, op.zombie());
                }
                case PLACE -> {
                    // 位置已被其他方块占用 → 跳过放置
                    if (!currentState.isAir()) {
                        continue;
                    }
                    processed++;
                    // 使用 flag 2|8：仅更新客户端 + 不触发物理（如沙子下落），避免邻居更新链
                    level.setBlock(op.pos(), op.placeState(), 2 | 8);
                }
            }
        }

        if (!remaining.isEmpty()) {
            // 未完成的操作重新入队，等待下次刷新
            QUEUE.computeIfAbsent(level, k -> new ArrayList<>()).addAll(remaining);
            // 不清除 PENDING_POSITIONS：剩余位置仍需防重复入队
        } else {
            // 全部操作已完成，清理待处理位置记录
            PENDING_POSITIONS.remove(level);
        }
    }

    /**
     * 刷新所有世界中的延迟操作。
     */
    public static void flushAll() {
        for (ServerLevel level : List.copyOf(QUEUE.keySet())) {
            flushLevel(level);
        }
    }
}
