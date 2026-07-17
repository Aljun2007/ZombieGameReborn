package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraftforge.common.Tags;

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
    protected Path path;
    protected double pathedTargetX;
    protected double pathedTargetY;
    protected double pathedTargetZ;
    protected int ticksUntilNextPathRecalculation;
    protected int ticksUntilNextAttack;
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
    private final IZombieData data;
    private int breakIndex = 0;
    private long lastHurtAndCanReachPlayerTime = 0L;

    public ZombieSmartBreakAttackGoal(Zombie zombie,IZombieData data) {
        this.zombie = zombie;
        this.data = data;
        this.speedModifier = 1.0d;
        this.followingTargetEvenIfNotSeen = true;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.tryGetBreakGoal();
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }

        this.lastCanUseCheck = gameTime;
        LivingEntity target = this.zombie.getTarget();

        if (target == null) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (canPenalize) {
            if (--this.ticksUntilNextPathRecalculation <= 0) {
                this.path = this.zombie.getNavigation().createPath(target, 0);
                this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);
                return this.path != null;
            } else {
                return true;
            }
        }

        this.path = this.zombie.getNavigation().createPath(target, 0);
        if (this.path != null) {
            return true;
        }

        return this.getAttackReachSqr(target) >= this.zombie.distanceToSqr(
                target.getX(), target.getY(), target.getZ()
        );
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
        this.zombie.getNavigation().moveTo(this.path, this.speedModifier);
        this.zombie.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.zombie.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
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

        if (this.zombie.position().distanceToSqr(target.position()) <= 25.0D) {
            this.ticksUntilNextPathRecalculation -= 2;
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
            this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);

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

            if (distanceSqr > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distanceSqr > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
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
        if (this.zombie.level().getGameTime() - this.lastSetMeleeTime >= BREAK_COOLDOWN && this.zombie.level().getGameTime() - this.lastHurtAndCanReachPlayerTime >= HURT_BUILD_COOLDOWN) {
            this.state = State.BREAK;
            this.breakIndex = 0;
            this.breakQueue.addAll(this.findBlockingBlocks(this.zombie.blockPosition(), targetPos));
        }
    }

    /**
     * 查找从from到to路径上需要破坏的方块
     * 核心原则：
     * 1. 只破坏3格内的方块
     * 2. 只破坏硬度 > 0 的方块
     * 3. 不挖脚下方块（除非向下走时前方脚底被阻挡）
     */
    private List<BlockPos> findBlockingBlocks(BlockPos from, BlockPos to) {
        List<BlockPos> blockingBlocks = new ArrayList<>();

        // 最大挖掘距离3格
        final int MAX_REACH = 3;
        final double MAX_REACH_SQR = MAX_REACH * MAX_REACH;

        // 计算方向向量
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());

        // 计算最大步数
        int maxSteps = Math.max(Math.abs(to.getX() - from.getX()),
                Math.max(Math.abs(to.getY() - from.getY()),
                        Math.abs(to.getZ() - from.getZ()))) + 1;

        // 限制步数，避免超出3格范围
        maxSteps = Math.min(maxSteps, MAX_REACH * 2 + 1);

        for (int i = 0; i < maxSteps; i++) {
            BlockPos current = new BlockPos(
                    from.getX() + dx * i,
                    from.getY() + dy * i,
                    from.getZ() + dz * i
            );

            // 检查距离
            if (current.distSqr(from) > MAX_REACH_SQR) {
                break;
            }

            // 根据高度差选择检查策略
            int heightDiff = to.getY() - current.getY();

            if (heightDiff > 0) {
                // 目标在上方（向上走）
                this.checkBlocksForGoingUp(current, to, blockingBlocks);
            } else if (heightDiff < 0) {
                // 目标在下方（向下走）
                this.checkBlocksForGoingDown(current, to, blockingBlocks);
            } else {
                // 水平移动
                this.checkBlocksForHorizontal(current, to, blockingBlocks);
            }

            if (current.equals(to)) {
                break;
            }
        }

        // 过滤、去重、排序
        return this.filterAndSortBlocks(blockingBlocks, from);
    }

    /**
     * 检查向上走时需要破坏的方块
     */
    private void checkBlocksForGoingUp(BlockPos current, BlockPos to, List<BlockPos> blockingBlocks) {
        Direction direction = this.getFacingDirection(current, to);
        BlockPos front = current.relative(direction);

        // 1. 检查前方身体位置 (y+1) - 最重要，防止撞头
        BlockPos frontBody = front.above();
        this.addIfBlocking(frontBody, blockingBlocks);

        // 2. 检查前方头顶位置 (y+2)
        BlockPos frontHead = front.above(2);
        this.addIfBlocking(frontHead, blockingBlocks);

        // 3. 检查前方脚底位置 (y) - 如果是完整方块且阻挡前进
        if (this.isBlockingBlock(front) && this.isFullBlock(front)) {
            blockingBlocks.add(front);
        }
    }

    /**
     * 检查向下走时需要破坏的方块
     */
    private void checkBlocksForGoingDown(BlockPos current, BlockPos to, List<BlockPos> blockingBlocks) {
        Direction direction = this.getFacingDirection(current, to);
        BlockPos front = current.relative(direction);

        // 1. 检查前方身体位置 (y+1) - 防止撞头
        BlockPos frontBody = front.above();
        this.addIfBlocking(frontBody, blockingBlocks);

        // 2. 检查前方脚底位置 (y) - 如果阻挡下坡
        // 注意：这里只检查前方脚底，不检查当前脚底
        if (this.isBlockingBlock(front)) {
            // 检查下方是否有落脚点
            BlockPos belowFront = front.below();
            if (!this.isBlockingBlock(belowFront)) {
                // 如果前方下面是空的，可能需要挖掉前方脚底的方块
                blockingBlocks.add(front);
            }
        }

        // 3. 检查前方头顶位置 (y+2) - 防止头顶撞到
        BlockPos frontHead = front.above(2);
        this.addIfBlocking(frontHead, blockingBlocks);
    }

    /**
     * 检查水平移动时需要破坏的方块
     */
    private void checkBlocksForHorizontal(BlockPos current, BlockPos to, List<BlockPos> blockingBlocks) {
        Direction direction = this.getFacingDirection(current, to);
        BlockPos front = current.relative(direction);

        // 1. 检查前方身体位置 (y+1) - 最重要
        BlockPos frontBody = front.above();
        this.addIfBlocking(frontBody, blockingBlocks);

        // 2. 检查前方头顶位置 (y+2)
        BlockPos frontHead = front.above(2);
        this.addIfBlocking(frontHead, blockingBlocks);

        // 3. 检查两侧身体位置 - 防止卡在狭窄通道
        for (Direction sideDir : new Direction[]{direction.getClockWise(), direction.getCounterClockWise()}) {
            BlockPos side = current.relative(sideDir);
            BlockPos sideBody = side.above();
            this.addIfBlocking(sideBody, blockingBlocks);

            BlockPos sideHead = side.above(2);
            this.addIfBlocking(sideHead, blockingBlocks);
        }

        // 4. 检查前方脚底 - 如果有方块阻挡且是完整方块
        if (this.isBlockingBlock(front) && this.isFullBlock(front)) {
            // 检查这个方块是否高于地面（比如多出来的半砖）
            BlockPos belowFront = front.below();
            if (this.isBlockingBlock(belowFront)) {
                // 如果下面有方块支撑，说明这是个台阶或障碍物
                blockingBlocks.add(front);
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
        if (this.zombie.level().isOutsideBuildHeight(pos)) {
            return false;
        }

        BlockState state = this.zombie.level().getBlockState(pos);

        // 3. 空气不挖
        if (state.isAir()) {
            return false;
        }

        // 4. 液体不挖
        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        // 5. 检查硬度（唯一核心规则）
        float hardness = state.getDestroySpeed(this.zombie.level(), pos);

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
        BlockState state = this.zombie.level().getBlockState(pos);
        return state.isCollisionShapeFullBlock(this.zombie.level(), pos);
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
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : uniqueSet) {
            if (this.isBlockingBlock(pos)) {
                result.add(pos);
            }
        }

        // 按距离排序（近的优先）
        result.sort((pos1, pos2) -> {
            double dist1 = pos1.distSqr(from);
            double dist2 = pos2.distSqr(from);
            return Double.compare(dist1, dist2);
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

        if (this.zombie.level().getGameTime() - this.lastGiveUpBreakTime > 100) {
            Path path = this.zombie.getNavigation().createPath(target, 0);
            if (path != null) {
                Node finalNode = path.getEndNode();
                if (finalNode != null) {
                    BlockPos pathEnd = finalNode.asBlockPos();
                    BlockPos breakEnd = this.breakQueue.get(this.breakQueue.size() - 1);
                    if ((Math.sqrt(pathEnd.distSqr(target.blockPosition()) + 10) < Math.sqrt(breakEnd.distSqr(target.blockPosition())))) {
                        this.setMelee();
                        this.zombie.getNavigation().moveTo(path, this.speedModifier);
                        this.lastGiveUpBreakTime = this.zombie.level().getGameTime();
                        return;
                    }
                }
            }
        }

        while (true) {
            if (this.breakIndex >= this.breakQueue.size()) {
                this.setMelee();
                break;
            }

            BlockPos pos = this.breakQueue.get(this.breakIndex);

            if (this.isPosIllegal(pos)) {
                this.setMelee();
                break;
            }

            if (!this.isBlockingBlock(pos)) {
                this.breakIndex++;
                continue;
            }

            if (this.breakGoal != null) {
                if (this.breakGoal.isDone()) {
                    if (this.breakGoal.tryToBreak(pos)) {
                        this.currentBreakTarget = pos;
                        this.lastBreakTime = this.zombie.level().getGameTime();

                        Path path = this.zombie.getNavigation().createPath(pos, 0);
                        if (path != null) {
                            this.zombie.getNavigation().moveTo(path, this.speedModifier);
                        }
                        break;
                    } else {
                        this.setMelee();
                    }
                }
            }

            break;
        }
    }

    private boolean isPosIllegal(BlockPos pos) {
        return this.zombie.level().isOutsideBuildHeight(pos);
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
                            this.lastHurtAndCanReachPlayerTime = this.zombie.level().getGameTime();
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
        this.lastSetMeleeTime = this.zombie.level().getGameTime();
    }

    protected enum State {
        MELEE,
        BREAK
    }
}
