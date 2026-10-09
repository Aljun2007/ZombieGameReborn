package com.aljun.zombiegamereborn.common.entity.awareness;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 气味轨迹网格。
 *
 * <h2>为什么不用实体</h2>
 * 另一套常见做法是给每个气味生成一个实体（EntityScent）再让它自己 tick：
 * 那意味着每个气味实体都要走完整的实体生命周期、被保存/加载、被同步到客户端，
 * 一次长时间战斗能堆出成百上千个实体 —— 这是最典型的性能塌方点。
 *
 * <p>这里改成<b>稀疏体素网格</b>：
 * <ul>
 *   <li>玩家/生物走过时按节流（默认每 10 tick）往所在格写入强度，O(1)；</li>
 *   <li>衰减是<b>惰性</b>的：读取时用时间戳现算，不需要每 tick 遍历任何东西；</li>
 *   <li>僵尸跟随走<b>梯度上升</b>：只比较自己周围 10 个格子的强度，挑更浓的一格走过去，
 *       每次决策 O(10) 次哈希查找，不触发任何寻路风暴；</li>
 *   <li>内存有硬上限（{@link #MAX_CELLS}），满了先压缩再丢弃，绝不无界增长。</li>
 * </ul>
 */
public final class ScentGrid {

    /** 单元格边长 = 1 << CELL_SHIFT 格。4 格在精度与内存之间比较平衡。 */
    public static final int CELL_SHIFT = 2;

    /** 单格强度上限。 */
    private static final float MAX_INTENSITY = 8.0F;

    /** 单元格数量硬上限，约 8192 × (4³) 格体积。 */
    private static final int MAX_CELLS = 8192;

    /** 压缩间隔（tick）。 */
    private static final int COMPACT_INTERVAL = 200;

    /**
     * 方向判据：邻居必须比当前格<b>新</b>这么多 tick 才值得转向。
     * <p>
     * 这里刻意<b>不用浓度大小</b>判断方向。轨迹上相邻两格的浓度差恒为
     * “沉积间隔 / 衰减时长”（玩家约 10/600，生物约 40/2400，都是 ~1.7%），
     * 任何像样的浓度滞回阈值（试过 ×1.15）都会把它整个滤掉，导致僵尸永远不前进。
     * 而“哪一格更新”才是轨迹真正的方向信息，且与沉积频率、衰减配置都无关。
     */
    private static final long MIN_RECENCY_GAIN = 5L;

    /** 低于这个强度的格子视为没味道（用于判断轨迹是否还存在）。 */
    private static final float MIN_INTENSITY = 0.02F;

    private final Long2ObjectOpenHashMap<ScentCell> cells = new Long2ObjectOpenHashMap<>();
    private long nextCompactTick;
    private int droppedDeposits;

    // ------------------------------------------------------------------
    // 写入
    // ------------------------------------------------------------------

    /**
     * 往指定位置的气味格累加强度。
     *
     * @param amount 本次累加量，通常与“这个实体有多值得追”有关（玩家比小动物浓）
     */
    public void deposit(double x, double y, double z, float amount, long now) {
        if (amount <= 0.0F) return;
        int cx = cellCoord(x);
        int cy = cellCoord(y);
        int cz = cellCoord(z);
        long key = BlockPos.asLong(cx, cy, cz);
        ScentCell cell = this.cells.get(key);
        if (cell == null) {
            if (this.cells.size() >= MAX_CELLS) {
                compact(now);
                if (this.cells.size() >= MAX_CELLS) {
                    this.droppedDeposits++;      // 满了就丢，绝不扩容
                    return;
                }
            }
            cell = new ScentCell(cx, cy, cz);
            this.cells.put(key, cell);
        }
        cell.intensity = Math.min(cell.intensity + amount, MAX_INTENSITY);
        cell.tick = now;
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    /** 单元格当前有效强度（惰性衰减）。 */
    public float effectiveIntensity(int cx, int cy, int cz, long now) {
        ScentCell cell = this.cells.get(BlockPos.asLong(cx, cy, cz));
        return cell == null ? 0.0F : cell.effective(now);
    }

    /**
     * 找出“下一格该往哪走”：在当前位置周围 10 个邻格里挑<b>最近被写入</b>且还有味道的一格。
     * <p>
     * 返回 null 表示“到此为止”（要么自己就是最新的一格，要么轨迹断了）。
     * <p>
     * 返回的 {@link ScentCell} 是网格内部对象，调用方<b>只读</b>，不要持有引用。
     */
    @Nullable
    public ScentCell bestNeighbour(double x, double y, double z, long now) {
        if (this.cells.isEmpty()) {
            return null;
        }
        if (now >= this.nextCompactTick) {
            compact(now);
            if (this.cells.isEmpty()) {
                return null;
            }
        }

        int cx = cellCoord(x);
        int cy = cellCoord(y);
        int cz = cellCoord(z);

        ScentCell here = this.cells.get(BlockPos.asLong(cx, cy, cz));
        long hereTick = here == null ? Long.MIN_VALUE : here.tick;

        ScentCell best = null;
        long bestTick = hereTick + MIN_RECENCY_GAIN;
        // 8 个水平邻格 + 上下两格：共 10 次哈希查找，覆盖“绕圈”和“爬梯子”两种情况
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;           // 同 XZ 列单独处理垂直
                    if (dy != 0 && (dx != 0 || dz != 0)) continue; // 只取同层 8 邻 + 垂直 2
                    ScentCell candidate = this.cells.get(BlockPos.asLong(cx + dx, cy + dy, cz + dz));
                    if (candidate == null) continue;
                    if (candidate.effective(now) <= MIN_INTENSITY) continue;
                    if (candidate.tick > bestTick) {
                        bestTick = candidate.tick;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /** 某个位置是否还有可追的气味（用于快速判断）。 */
    public boolean hasScentNear(double x, double y, double z, long now, int cellRadius) {
        int cx = cellCoord(x);
        int cy = cellCoord(y);
        int cz = cellCoord(z);
        for (int dy = -cellRadius; dy <= cellRadius; dy++) {
            for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                for (int dx = -cellRadius; dx <= cellRadius; dx++) {
                    float intensity = effectiveIntensity(cx + dx, cy + dy, cz + dz, now);
                    if (intensity > 0.05F) return true;
                }
            }
        }
        return false;
    }

    /**
     * 清理已衰减干净的格子。带时间戳判断，所以没有“每 tick 遍历全场”的隐藏成本。
     */
    public void compact(long now) {
        this.nextCompactTick = now + COMPACT_INTERVAL;
        if (this.cells.isEmpty()) {
            return;
        }
        var iterator = this.cells.long2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().effective(now) <= 0.0F) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        this.cells.clear();
        this.nextCompactTick = 0L;
        this.droppedDeposits = 0;
    }

    public int cellCount() {
        return this.cells.size();
    }

    public int droppedDeposits() {
        return this.droppedDeposits;
    }

    public static int cellCoord(double worldCoord) {
        return (int) Math.floor(worldCoord) >> CELL_SHIFT;
    }

    public static double cellCenter(int cellCoord) {
        return (cellCoord << CELL_SHIFT) + (1 << (CELL_SHIFT - 1));
    }

    /** 网格内的一个气味格。惰性衰减，不做任何主动 tick。 */
    public static final class ScentCell {
        public final int cx;
        public final int cy;
        public final int cz;
        float intensity;
        long tick;

        ScentCell(int cx, int cy, int cz) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
        }

        /** 线性衰减到 0。{@code AwarenessTuning.scentDecayTicks} 控制寿命。 */
        public float effective(long now) {
            int decayTicks = AwarenessTuning.scentDecayTicks();
            long age = now - this.tick;
            if (age <= 0) return this.intensity;
            if (age >= decayTicks) return 0.0F;
            return this.intensity * (1.0F - (float) age / (float) decayTicks);
        }

        /** 格子中心坐标，作为僵尸前往的目标点。 */
        public double centerX() {
            return cellCenter(this.cx);
        }

        public double centerY() {
            return cellCenter(this.cy);
        }

        public double centerZ() {
            return cellCenter(this.cz);
        }
    }
}
