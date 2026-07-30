package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.optimizer.ZombieGoalOptimizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.*;

public class ZombieSmartBreakAttackGoal extends Goal {
    protected static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    private static final int BREAK_COOLDOWN = 10;
    private static final double FORCE_BREAK_DISTANCE_TO_SQR = 4d;
    private static final long HURT_BUILD_COOLDOWN = 100;
    protected final Zombie zombie;
    protected final double speedModifier;
    protected final boolean followingTargetEvenIfNotSeen;
    protected final int attackInterval = 20;
    private final IZombieData data;
    private final ServerLevel level;
    protected Path path;
    protected double pathedTargetX;
    protected double pathedTargetY;
    protected double pathedTargetZ;
    protected int ticksUntilNextPathRecalculation;
    protected int ticksUntilNextAttack;
    protected int ticksUntilNextPathFallbackCheck = 0;
    protected long lastCanUseCheck;
    protected int failedPathFindingPenalty = 0;
    protected boolean canPenalize = false;
    protected ZombieBreakBlockGoal breakGoal;
    protected List<BlockPos> breakQueue = new ArrayList<>();
    protected BlockPos currentBreakTarget = null;
    protected long lastBreakTime = 0L;
    protected long lastSetMeleeTime = 0L;
    protected boolean isTried = false;
    protected State state = State.MELEE;
    private long lastGiveUpBreakTime = 0L;
    private int breakIndex = 0;
    private long lastHurtAndCanReachPlayerTime = 0L;

    public ZombieSmartBreakAttackGoal(Zombie zombie, IZombieData data) {
        this.zombie = zombie;
        this.level = (ServerLevel) zombie.level();
        this.data = data;
        this.speedModifier = 1.0d;
        this.followingTargetEvenIfNotSeen = true;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private int getRoughPathfindingThreshold() {
        return ZGRGame.getGameProperty().roughPathfindingThreshold;
    }

    private int getRoughPathfindingInterval() {
        return ZGRGame.getGameProperty().roughPathfindingInterval;
    }

    @Override
    public boolean canUse() {
        this.tryGetBreakGoal();
        if (!this.data.isEmpowered()) return false;
        long gameTime = this.level.getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }

        this.lastCanUseCheck = gameTime;
        LivingEntity target = this.zombie.getTarget();

        if (target == null) {
            return false;
        }

        return target.isAlive();
    }

    private void tryGetBreakGoal() {
        if (!this.isTried) {
            this.breakGoal = ZGRZombieControlAPI.getBreakPlaceGoal(zombie);
            this.isTried = true;
        }
    }

    protected double getAttackReachSqr(LivingEntity target) {
        return this.zombie.getBbWidth() * 2.0F * this.zombie.getBbWidth() * 2.0F + target.getBbWidth();
    }

    @Override
    public boolean canContinueToUse() {

        LivingEntity target = this.zombie.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
            return false;
        }

        if (!this.followingTargetEvenIfNotSeen) {
            // 目标在攻击范围内时保持运行，让 tick() 执行攻击
            if (this.zombie.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                return true;
            }
            return !this.zombie.getNavigation().isDone();
        }

        if (!this.zombie.isWithinRestriction(target.blockPosition())) {
            return false;
        }

        if (target instanceof Player player) {
            return !player.isSpectator() && !player.isCreative();
        }

        return true;
    }

    @Override
    public void start() {
        LivingEntity target = this.zombie.getTarget();
        if (target != null) {
            this.path = this.zombie.getNavigation().createPath(target, 0);
            if (this.path != null) {
                this.zombie.getNavigation().moveTo(this.path, this.speedModifier);
            }
        }
        this.zombie.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
        this.ticksUntilNextPathFallbackCheck = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.zombie.getTarget();
        if (target != null && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
        }

        this.zombie.setAggressive(false);
        this.zombie.getNavigation().stop();

        if (this.breakGoal != null && !this.breakGoal.isDone()) {
            this.breakGoal.stopBreak();
        }

        this.breakQueue.clear();
        this.currentBreakTarget = null;
        this.state = State.MELEE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }


    //------------智能找方块----------------------------------------------------

    @Override
    public void tick() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return;
        }

        this.zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distanceSqr = this.zombie.distanceToSqr(target.getX(), target.getY(), target.getZ());

        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        // 当僵尸进入更近的距离范围时，缩短剩余延迟，消除远距离低频寻路的滞后效应
        if (distanceSqr <= 16.0D) {
            if (this.ticksUntilNextPathRecalculation > 10) this.ticksUntilNextPathRecalculation = 0;
        } else if (distanceSqr <= (double)(getRoughPathfindingThreshold() * getRoughPathfindingThreshold())) {
            if (this.ticksUntilNextPathRecalculation > 60) this.ticksUntilNextPathRecalculation = 0;
        }

        // 路径兜底：每10tick检查导航是否完成，大幅度缩短僵尸原地发呆时间
        // 当导航完成时直接强制moveTo，不依赖主寻路块（绕过 hasLineOfSight 限制）
        if (--this.ticksUntilNextPathFallbackCheck <= 0) {
            this.ticksUntilNextPathFallbackCheck = 25;
            if (this.zombie.getNavigation().isDone() && distanceSqr > this.getAttackReachSqr(target)) {
                this.ticksUntilNextPathRecalculation = 0;
                this.zombie.getNavigation().moveTo(target, this.speedModifier);
            }
        }

        if (this.state == State.MELEE) {
            this.handleMeleeState(target, distanceSqr);
        } else if (this.state == State.BREAK) {
            this.handleBreakState(target, distanceSqr);
        }
    }

    private void handleMeleeState(LivingEntity target, double distanceSqr) {
        if ((this.followingTargetEvenIfNotSeen || this.zombie.getSensing().hasLineOfSight(target))
                && this.ticksUntilNextPathRecalculation <= 0
                && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D
                || target.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D
                || this.zombie.getRandom().nextFloat() < 0.05F)) {

            this.pathedTargetX = target.getX();
            this.pathedTargetY = target.getY();
            this.pathedTargetZ = target.getZ();
            // 梯度寻路频率：4格内高频(10tick)、粗略范围内中频(60tick)、超粗略范围低频(配置值)
            if (distanceSqr <= 16.0D) {
                this.ticksUntilNextPathRecalculation = 10;
            } else if (distanceSqr <= (double)(getRoughPathfindingThreshold() * getRoughPathfindingThreshold())) {
                this.ticksUntilNextPathRecalculation = 60;
            } else {
                this.ticksUntilNextPathRecalculation = getRoughPathfindingInterval();
            }

            if (this.canPenalize) {
                this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;

                if (this.zombie.getNavigation().getPath() != null) {
                    Node finalPathPoint = this.zombie.getNavigation().getPath().getEndNode();
                    if (finalPathPoint != null
                            && target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1) {
                        failedPathFindingPenalty = 0;
                    } else {
                        failedPathFindingPenalty += 10;
                    }
                } else {
                    failedPathFindingPenalty += 10;
                }
            }



            if (this.breakGoal != null && this.breakGoal.isDone()) {
                Path path = this.zombie.getNavigation().createPath(target, 0);
                boolean moved = false;

                if (path != null) {
                    moved = this.zombie.getNavigation().moveTo(path, this.speedModifier);

                    if (this.canBreakBlocks()) {
                        Node finalPathPoint = path.getEndNode();
                        if (finalPathPoint != null) {
                            if (this.zombie.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) <= FORCE_BREAK_DISTANCE_TO_SQR &&
                                    target.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) > FORCE_BREAK_DISTANCE_TO_SQR) {
                                this.setBreakMode(target.blockPosition());
                            } else if (target.distanceToSqr(this.zombie) <= 3) {
                                this.setBreakMode(target.blockPosition());
                            }
                        } else {
                            this.setBreakMode(target.blockPosition());
                        }
                    }
                } else {
                    if (this.canBreakBlocks()) {
                        this.setBreakMode(target.blockPosition());
                    }
                }

                if (!moved) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            }

            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(target, distanceSqr);
    }

    private void setBreakMode(BlockPos targetPos) {
        if (this.level.getGameTime() - this.lastSetMeleeTime >= BREAK_COOLDOWN && this.level.getGameTime() - this.lastHurtAndCanReachPlayerTime >= HURT_BUILD_COOLDOWN) {
            this.state = State.BREAK;
            this.breakIndex = 0;
            this.breakQueue.addAll(this.findBlockingBlocks(this.zombie.blockPosition(), targetPos));
        }
    }

    /**
     * 查找从from到to路径上需要破坏的方块
     * 根据目标相对角度分为5种策略：
     * 1. 斜上 >45°：陡峭向上，清空垂直通道
     * 2. 斜上 <45°：浅向上，先清头顶再向前上
     * 3. =0°：水平移动，检查前方身体/头顶/两侧
     * 4. 斜下 <45°：浅向下，检查前方下落路径
     * 5. 斜下 >45°：陡峭向下，挖掘下方通道
     */
    private List<BlockPos> findBlockingBlocks(BlockPos from, BlockPos to) {
        List<BlockPos> blockingBlocks = new ArrayList<>();
        final int MAX_REACH = 3;

        int hDist = Math.max(Math.abs(to.getX() - from.getX()), Math.abs(to.getZ() - from.getZ()));
        int vDist = Math.abs(to.getY() - from.getY());
        Direction direction = this.getFacingDirection(from, to);

        if (vDist == 0) {
            // Case 3: =0° 水平
            this.scanLevel(from, direction, MAX_REACH, blockingBlocks);
        } else if (to.getY() > from.getY()) {
            if (vDist >= hDist) {
                // Case 1: 斜上 >45°
                this.scanSteepUp(from, direction, MAX_REACH, blockingBlocks);
            } else {
                // Case 2: 斜上 <45°
                this.scanShallowUp(from, direction, MAX_REACH, blockingBlocks);
            }
        } else {
            if (vDist >= hDist) {
                // Case 5: 斜下 >45°
                this.scanSteepDown(from, direction, MAX_REACH, blockingBlocks);
            } else {
                // Case 4: 斜下 <45°
                this.scanShallowDown(from, direction, MAX_REACH, blockingBlocks);
            }
        }

        return this.filterAndSortBlocks(blockingBlocks, from);
    }

    /**
     * Case 3: 水平移动 (=0°) — 检查前方3格的身体、头顶和两侧
     */
    private void scanLevel(BlockPos from, Direction direction, int maxReach, List<BlockPos> result) {
        for (int i = 1; i <= maxReach; i++) {
            BlockPos front = from.relative(direction, i);

            // 前方身体 (y+1) 和头顶 (y+2)
            this.addIfBlocking(front.above(), result);
            this.addIfBlocking(front.above(2), result);

            // 两侧身体和头顶
            for (Direction side : new Direction[]{direction.getClockWise(), direction.getCounterClockWise()}) {
                BlockPos sidePos = front.relative(side);
                this.addIfBlocking(sidePos.above(), result);
                this.addIfBlocking(sidePos.above(2), result);
            }

            // 前方脚底 — 完整方块且下方有支撑则视为障碍
            if (this.isBlockingBlock(front) && this.isFullBlock(front)) {
                BlockPos below = front.below();
                if (this.isBlockingBlock(below)) {
                    result.add(front);
                }
            }
        }
    }

    /**
     * Case 2: 斜上 <45° — 目标略高，先清头顶空间，再向前向上
     */
    private void scanShallowUp(BlockPos from, Direction direction, int maxReach, List<BlockPos> result) {
        // 先清空当前头顶空间，为向上走做准备
        this.addIfBlocking(from.above(), result);
        this.addIfBlocking(from.above(2), result);

        for (int i = 1; i <= maxReach; i++) {
            BlockPos front = from.relative(direction, i);

            // 前方身体和头顶
            this.addIfBlocking(front.above(), result);
            this.addIfBlocking(front.above(2), result);

            // 抬高一层的前方空间（向上阶梯）
            BlockPos elevated = front.above();
            this.addIfBlocking(elevated.above(), result);
            this.addIfBlocking(elevated.above(2), result);

            // 前方脚底障碍
            if (this.isBlockingBlock(front) && this.isFullBlock(front)) {
                result.add(front);
            }
        }
    }

    /**
     * Case 1: 斜上 >45° — 目标远高于僵尸，需要挖掘垂直通道
     */
    private void scanSteepUp(BlockPos from, Direction direction, int maxReach, List<BlockPos> result) {
        // 清空头顶所有可达层的方块
        for (int y = 1; y <= maxReach; y++) {
            this.addIfBlocking(from.above(y), result);
        }

        for (int i = 1; i <= maxReach; i++) {
            BlockPos front = from.relative(direction, i);

            // 前方各高度的垂直空间
            for (int y = 1; y <= maxReach; y++) {
                this.addIfBlocking(front.above(y), result);
            }
        }
    }

    /**
     * Case 4: 斜下 <45° — 目标略低，检查前方下落路径
     */
    private void scanShallowDown(BlockPos from, Direction direction, int maxReach, List<BlockPos> result) {
        for (int i = 1; i <= maxReach; i++) {
            BlockPos front = from.relative(direction, i);

            // 前方身体和头顶
            this.addIfBlocking(front.above(), result);
            this.addIfBlocking(front.above(2), result);

            // 前方地面 — 下方为空时需要挖掉来下坡
            if (this.isBlockingBlock(front)) {
                BlockPos belowFront = front.below();
                if (!this.isBlockingBlock(belowFront)) {
                    result.add(front);
                }
            }
        }
    }

    /**
     * Case 5: 斜下 >45° — 目标远低于僵尸，需要挖掘下方通道
     */
    private void scanSteepDown(BlockPos from, Direction direction, int maxReach, List<BlockPos> result) {
        // 检查脚下方块 — 向下挖掘需要清除脚下
        for (int y = 0; y <= maxReach; y++) {
            BlockPos below = from.below(y);
            this.addIfBlocking(below, result);
        }

        for (int i = 1; i <= maxReach; i++) {
            BlockPos front = from.relative(direction, i);

            // 前方身体和头顶
            this.addIfBlocking(front.above(), result);
            this.addIfBlocking(front.above(2), result);

            // 前方各高度向下的通道
            for (int y = 0; y <= maxReach; y++) {
                this.addIfBlocking(front.below(y), result);
            }
        }
    }

    /**
     * 添加方块到列表（如果它阻挡路径）
     */
    private void addIfBlocking(BlockPos pos, List<BlockPos> blockingBlocks) {
        if (this.isBlockingBlock(pos)) {
            blockingBlocks.add(pos);
        }
    }

    /**
     * 检查方块是否阻挡路径
     * 核心判断：距离<=3格，硬度>0，不是空气/液体
     */
    private boolean isBlockingBlock(BlockPos pos) {
        // 1. 检查是否在3格内（使用僵尸脚底位置）
        BlockPos feetPos = this.zombie.blockPosition();
        if (pos.distSqr(feetPos) > 9.0) {
            return false;
        }

        // 2. 检查世界高度
        if (this.level.isOutsideBuildHeight(pos)) {
            return false;
        }

        BlockState state = this.level.getBlockState(pos);

        // 3. 空气不挖
        if (state.isAir()) {
            return false;
        }

        // 4. 液体不挖
        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        // 5. 检查硬度（唯一核心规则）
        float hardness = state.getDestroySpeed(this.level, pos);

        // 硬度 < 0：不可破坏（基岩等），不挖
        if (hardness < 0) {
            return false;
        }

        // 硬度 = 0：瞬间破坏（植物、火把等），不需要挖
        return hardness != 0;

        // 硬度 > 0：可以破坏
    }
    //--------------------------------------------------------------------

    /**
     * 检查是否是完全方块（有完整碰撞箱）
     */
    private boolean isFullBlock(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(this.level, pos);
    }

    /**
     * 获取僵尸面对的方向（朝向目标）
     */
    private Direction getFacingDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * 过滤、去重、排序方块列表
     */
    private List<BlockPos> filterAndSortBlocks(List<BlockPos> blocks, BlockPos from) {
        // 去重
        Set<BlockPos> uniqueSet = new LinkedHashSet<>(blocks);

        // 转为列表并再次确认
        List<BlockPos> result = new ArrayList<>(uniqueSet);

        // 按距离排序（近的优先），相同水平位置由下到上
        result.sort((pos1, pos2) -> {
            double hDist1 = Math.sqrt(Math.pow(pos1.getX() - from.getX(), 2) + Math.pow(pos1.getZ() - from.getZ(), 2));
            double hDist2 = Math.sqrt(Math.pow(pos2.getX() - from.getX(), 2) + Math.pow(pos2.getZ() - from.getZ(), 2));
            int hCompare = Double.compare(hDist1, hDist2);
            if (hCompare != 0) return hCompare;
            // 相同水平距离时，由下到上（Y小的优先）
            return Integer.compare(pos1.getY(), pos2.getY());
        });

        // 限制最大数量，避免性能问题
        if (result.size() > 20) {
            result = result.subList(0, 20);
        }

        return result;
    }

    private void handleBreakState(LivingEntity target, double distanceSqr) {
        if (this.checkAndPerformAttack(target, distanceSqr)) {
            this.setMelee();
            return;
        }

        if (this.breakQueue.isEmpty()) {
            this.setMelee();
            return;
        }

        if (this.level.getGameTime() - this.lastGiveUpBreakTime > 100) {
            Path path = this.zombie.getNavigation().createPath(target, 0);

            if (path != null) {
                Node finalNode = path.getEndNode();
                if (finalNode != null) {
                    BlockPos pathEnd = finalNode.asBlockPos();
                    BlockPos breakEnd = this.breakQueue.get(this.breakQueue.size() - 1);
                    if ((Math.sqrt(pathEnd.distSqr(target.blockPosition()) + 10) < Math.sqrt(breakEnd.distSqr(target.blockPosition())))) {
                        this.setMelee();
                        this.zombie.getNavigation().moveTo(path, this.speedModifier);
                        this.lastGiveUpBreakTime = this.level.getGameTime();
                        return;
                    }
                }
            }
        }

        // 单步检查：每tick最多处理当前索引的一个方块，
        // 避免 while(true) 在已破坏方块上循环扫表造成大量 getBlockState 调用
        singleStepBreakCheck: while (true) {
            if (this.breakIndex >= this.breakQueue.size()) {
                this.setMelee();
                return;
            }

            BlockPos pos = this.breakQueue.get(this.breakIndex);

            if (this.isPosIllegal(pos)) {
                this.setMelee();
                return;
            }

            // breakGoal 正在破坏中，跳过方块检查
            if (this.breakGoal != null && !this.breakGoal.isDone()) {
                return;
            }

            if (!this.isBlockingBlock(pos)) {
                this.breakIndex++;
                // 返回，等下一 tick 再检查下一个方块，避免批量扫表
                return;
            }

            if (this.breakGoal != null) {
                if (this.breakGoal.isDone()) {
                    // 破坏方块前如果太远（3格以上）且 Navigation isDone，触发新寻路
                    if (this.zombie.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 9.0D
                            && this.zombie.getNavigation().isDone()) {
                        Path path = this.zombie.getNavigation().createPath(pos, 0);
                        if (path != null) {
                            this.zombie.getNavigation().moveTo(path, this.speedModifier);
                        }
                        return;
                    }

                    if (this.breakGoal.tryToBreak(pos)) {
                        this.currentBreakTarget = pos;
                        this.lastBreakTime = this.level.getGameTime();
                        return;
                    } else {
                        this.setMelee();
                    }
                }
            }

            return;
        }
    }

    private boolean isPosIllegal(BlockPos pos) {
        return this.level.isOutsideBuildHeight(pos);
    }

    private boolean canBreakBlocks() {
        return this.breakGoal != null && this.data.isEmpowered();
    }

    protected boolean checkAndPerformAttack(LivingEntity target, double distanceSqr) {
        double attackReachSqr = this.getAttackReachSqr(target);

        if (distanceSqr <= attackReachSqr && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.zombie.swing(InteractionHand.MAIN_HAND);
            this.zombie.doHurtTarget(target);
            return true;
        }
        return false;
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(this.attackInterval);
    }

    public void onZombieHurt() {
        if (this.zombie.getLastDamageSource() != null) {
            // 检查是否为近战伤害 (Melee Attack)
            if (this.zombie.getLastDamageSource().is(DamageTypes.MOB_ATTACK) || this.zombie.getLastDamageSource().is(DamageTypes.PLAYER_ATTACK)) {
                if (this.state == State.BREAK) {
                    LivingEntity attacker = this.zombie.getLastDamageSource().getEntity() instanceof LivingEntity livingEntity ? livingEntity : null;
                    if (attacker != null && this.zombie.getTarget() == attacker) {
                        // 判断玩家位置，小于2格
                        if (this.zombie.distanceToSqr(attacker) < 4.0D) {
                            this.setMelee();
                            this.lastHurtAndCanReachPlayerTime = this.level.getGameTime();
                        }
                    }
                }
            }
        }
    }

    private void setMelee() {
        this.state = State.MELEE;
        this.breakQueue.clear();
        this.currentBreakTarget = null;
        this.zombie.getNavigation().stop();
        if (this.breakGoal != null && !this.breakGoal.isDone()) {
            this.breakGoal.stopBreak();
        }
        this.lastSetMeleeTime = this.level.getGameTime();
    }

    protected enum State {
        MELEE,
        BREAK
    }


}
