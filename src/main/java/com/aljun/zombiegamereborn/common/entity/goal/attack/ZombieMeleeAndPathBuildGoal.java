package com.aljun.zombiegamereborn.common.entity.goal.attack;

import com.aljun.zombiegamereborn.api.ZGRZombieControlAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieBreakBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombiePlaceBlockGoal;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.ZombieWaterBridgeBuildGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.utils.MathUtils;
import com.aljun.zombiegamereborn.utils.PathConstructor;
import com.aljun.zombiegamereborn.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ZombieMeleeAndPathBuildGoal extends Goal {
    protected static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;
    private static final int BUILD_COOLDOWN = 10;
    private static final double FORCE_BUILD_DISTANCE_TO_SQRT = 4d;
    private static final int HURT_BUILD_COOLDOWN = 40;
    private static final int MAX_PATH_ITERATIONS = 50;
    private static final double RETURN_TO_SELF_MIN_DIST_SQR = 4.0;
    private static final double RETURN_TO_SELF_MAX_DIST_SQR = 8.0;
    private static final double PLACE_BLOCK_DISTANCE_SQR = 9.0;
    private static final double TOO_FAR_FROM_BLOCK_SQR = 25.0;
    private static final double POST_JUMP_MIN_DIST_SQR = 1.0;
    private static final double TARGET_CLOSE_DIST_SQR = 9.0;
    private static final long GIVE_UP_BUILD_TIME = 200L;
    private static final long PRE_BUILD_DURATION = 80L;
    private static final int PRE_BUILD_RANGE = 10;
    private static final int PRE_BUILD_VERTICAL_RANGE = 7;
    private static final double PRE_BUILD_REACH_DIST_SQR = 4.0;
    protected final Zombie zombie;
    protected final int attackInterval = 20;
    private final ServerLevel level;
    private final boolean giveUpHalfway = RandomUtils.booleanByChance(0.5d);
    private final IZombieData data;
    public ZombieBreakBlockGoal breakGoal = null;
    public ZombiePlaceBlockGoal placeGoal = null;
    protected double speedModifier = 1;
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
    protected PathConstructor pathConstructor;
    protected State state = State.MELEE;
    protected BlockPos buildTargetPos = null;
    protected PathConstructor.PathPack pathPack;
    protected BlockPos selfPos;
    private ZombieWaterBridgeBuildGoal bridgeGoal = null;
    private long lastSetMeleeTime = 0L;
    private long lastGiveUpBuildTime = 0L;
    private boolean isTried = false;
    private long lastHurtAndCanReachPlayerTime = 0L;
    private Vec3 preBuildTargetPos = null;
    private long preBuildStartTime = 0L;
    private PathConstructor.Style style = randomStyle();
    private Boolean cachedCanBreak = null;
    private Boolean cachedCanPlace = null;

    // 简化建造模式下结构间冷却（tick-- 倒计时）
    private int ticksUntilNextStructureBuild = 0;

    // 连续未修改任何方块的 path pack 计数，超过 6 次则放弃搭路
    private int consecutiveEmptyPacks = 0;

    // 跳跃后需要位移多少格才能放方块（防止原地跳放）

    public ZombieMeleeAndPathBuildGoal(Zombie zombie, IZombieData data) {
        this.zombie = zombie;
        this.level = (ServerLevel) zombie.level();
        this.pathConstructor = new PathConstructor();
        this.data = data;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean simplifiedBuilderMovement() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return false;
        }
        double distanceToTarget = this.zombie.distanceToSqr(target);
        int roughThreshold = getRoughPathfindingThreshold();
        // 只有在距离大于粗略寻路阈值的平方时，才启用简化建造移动
        return distanceToTarget > (double) (roughThreshold * roughThreshold) && ZGRGame.getGameProperty().simplifiedBuilderMovenment;
    }

    private int getRoughPathfindingThreshold() {
        return ZGRGame.getGameProperty().roughPathfindingThreshold;
    }

    private int getRoughPathfindingInterval() {
        return ZGRGame.getGameProperty().roughPathfindingInterval;
    }

    private PathConstructor.Style randomStyle() {
        if (RandomUtils.booleanByChance(0.5)) {
            return PathConstructor.Style.JUMP_PRIORITIZED;
        } else return PathConstructor.Style.NORMAL;

    }

    private BlockState getPlaceBlock() {
        if (this.placeGoal != null) {
            return this.placeGoal.getPlaceBlock();
        }
        return Blocks.DIRT.defaultBlockState();
    }

    public void onZombieHurt() {
        if (this.zombie.getLastDamageSource() != null) {
            // 检查是否为近战伤害 (Melee Attack)
            if (this.zombie.getLastDamageSource().is(DamageTypes.MOB_ATTACK) || this.zombie.getLastDamageSource().is(DamageTypes.PLAYER_ATTACK)) {
                if (this.state == State.BUILD) {
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

    protected void setMelee() {
        this.state = State.MELEE;
        this.buildTargetPos = null;
        this.pathPack = null;
        this.zombie.getNavigation().stop();
        if (this.breakGoal != null) {
            this.breakGoal.stopBreak();
        }
        this.lastSetMeleeTime = this.level.getGameTime();
        this.preBuildTargetPos = null;
        this.cachedCanBreak = null;
        this.cachedCanPlace = null;
    }

    protected void setBuild(BlockPos target) {
        long gameTime = this.level.getGameTime();
        if (gameTime - this.lastSetMeleeTime >= BUILD_COOLDOWN &&
                gameTime - this.lastHurtAndCanReachPlayerTime >= HURT_BUILD_COOLDOWN) {
            this.state = State.PRE_BUILD;
            this.buildTargetPos = target;
            this.preBuildStartTime = gameTime;
            this.preBuildTargetPos = null;
            this.selfPos = this.zombie.blockPosition();
            this.zombie.getNavigation().stop();
        }
    }

    @Override
    public boolean canUse() {
        if (!this.data.isEmpowered()) return false;
        this.ensureGoalsInitialized();
        long currentTime = this.level.getGameTime();
        if (currentTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        }

        this.lastCanUseCheck = currentTime;
        LivingEntity livingentity = this.zombie.getTarget();
        if (livingentity == null) {
            return false;
        }

        return livingentity.isAlive();
    }

    private void ensureGoalsInitialized() {
        if (!this.isTried) {
            this.breakGoal = ZGRZombieControlAPI.getBreakPlaceGoal(zombie);
            this.placeGoal = ZGRZombieControlAPI.getPlaceBlockGoal(zombie);
            this.bridgeGoal = (ZombieWaterBridgeBuildGoal) ZGRZombieControlAPI.getGoal(
                    this.zombie, goal -> goal instanceof ZombieWaterBridgeBuildGoal);
            this.isTried = true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 当 giveUpHalfway 为 false 且在 BUILD 状态有搭路目标时，即使目标实体消失也坚持搭路
        if (!this.giveUpHalfway && this.buildTargetPos != null && this.state.is(State.BUILD)) {
            return true;
        }

        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return false;
        } else if (!target.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen()) {
            // 目标在攻击范围内时保持运行，让 tick() 执行攻击
            if (this.zombie.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                return true;
            }
            return !this.zombie.getNavigation().isDone();
        } else if (!this.zombie.isWithinRestriction(target.blockPosition())) {
            return false;
        } else if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.zombie.setTarget(null);
            return false;
        }
        return true;
    }

    protected boolean followingTargetEvenIfNotSeen() {
        return true;
    }

    protected double getAttackReachSqr(LivingEntity p_25556_) {
        return this.zombie.getBbWidth() * 2.0F * this.zombie.getBbWidth() * 2.0F + p_25556_.getBbWidth();
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
        LivingEntity livingentity = this.zombie.getTarget();
        if (livingentity != null && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.zombie.setTarget(null);
        }

        this.zombie.setAggressive(false);
        this.zombie.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingentity = this.zombie.getTarget();
        if (livingentity != null) {
            this.zombie.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
            double distanceToTarget = this.zombie.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());

            if (this.state.is(State.MELEE)) {
                this.tickMeleeState(livingentity, distanceToTarget);
            }

            if (this.state.is(State.PRE_BUILD)) {
                this.tickPreBuildState(livingentity, distanceToTarget);
            }

            if (this.state.is(State.BUILD)) {
                this.tickBuildState(livingentity, distanceToTarget);
            }
        } else if (!this.giveUpHalfway && this.buildTargetPos != null && this.state.is(State.BUILD)) {
            // giveUpHalfway=false 且无目标实体时，继续坚持搭路到目标位置
            this.tickBuildState(null, 0);
        } else {
            this.stop();
        }
    }

    private void tickMeleeState(LivingEntity livingentity, double distanceToTarget) {
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        // 当僵尸进入更近的距离范围时，若剩余延迟超过阈值则直接归零，立即重算路径
        if (distanceToTarget <= 16.0D) {
            if (this.ticksUntilNextPathRecalculation > 10) this.ticksUntilNextPathRecalculation = 0;
        } else if (distanceToTarget <= (double) (getRoughPathfindingThreshold() * getRoughPathfindingThreshold())) {
            if (this.ticksUntilNextPathRecalculation > 60) this.ticksUntilNextPathRecalculation = 0;
        }

        // 路径兜底：每10tick检查导航是否完成，大幅度缩短僵尸原地发呆时间
        // 当导航完成时直接强制moveTo，不依赖主寻路块（绕过 hasLineOfSight 限制）
        if (--this.ticksUntilNextPathFallbackCheck <= 0) {
            this.ticksUntilNextPathFallbackCheck = 25;
            if (this.zombie.getNavigation().isDone() && distanceToTarget > this.getAttackReachSqr(livingentity)) {
                this.ticksUntilNextPathRecalculation = 0;
                this.zombie.getNavigation().moveTo(livingentity, this.speedModifier);
            }
        }

        if ((this.followingTargetEvenIfNotSeen() || this.zombie.getSensing().hasLineOfSight(livingentity))
                && this.ticksUntilNextPathRecalculation <= 0 &&
                (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D ||
                        livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D ||
                        this.zombie.getRandom().nextFloat() < 0.05F)) {
            this.pathedTargetX = livingentity.getX();
            this.pathedTargetY = livingentity.getY();
            this.pathedTargetZ = livingentity.getZ();

            // 梯度寻路频率：4格内高频(10tick)、粗略范围内中频(60tick)、超粗略范围低频(配置值)
            if (distanceToTarget <= 16.0D) {
                this.ticksUntilNextPathRecalculation = 10;
            } else if (distanceToTarget <= (double) (getRoughPathfindingThreshold() * getRoughPathfindingThreshold())) {
                this.ticksUntilNextPathRecalculation = 60;
            } else {
                this.ticksUntilNextPathRecalculation = getRoughPathfindingInterval();
            }

            if (this.canPenalize) {
                this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                if (this.zombie.getNavigation().getPath() != null) {
                    Node finalPathPoint = this.zombie.getNavigation().getPath().getEndNode();
                    if (finalPathPoint != null && livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                        failedPathFindingPenalty = 0;
                    else
                        failedPathFindingPenalty += 10;
                } else {
                    failedPathFindingPenalty += 10;
                }
            }

            if (this.breakGoal != null && this.breakGoal.isDone()) {
                Path path = this.zombie.getNavigation().createPath(livingentity, 0);
                boolean moved = false;
                if (path != null) {

                    if (this.canPathConstruct()) {
                        if ((this.bridgeGoal != null && this.bridgeGoal.isPathBuildCooldown())) {
                            moved = this.zombie.getNavigation().moveTo(path, this.speedModifier / data.getTotalMovementSpeedModify());
                        } else {
                            moved = this.zombie.getNavigation().moveTo(path, this.speedModifier);
                            Node finalPathPoint = path.getEndNode();
                            if (finalPathPoint != null) {
                                if (this.zombie.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) <= FORCE_BUILD_DISTANCE_TO_SQRT &&
                                        livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) > FORCE_BUILD_DISTANCE_TO_SQRT) {
                                    this.setBuild(livingentity.blockPosition());
                                } else if (livingentity.distanceToSqr(this.zombie) <= 3) {
                                    this.setBuild(livingentity.blockPosition());
                                }
                            } else {
                                this.setBuild(livingentity.blockPosition());
                            }
                        }
                    } else {
                        moved = this.zombie.getNavigation().moveTo(path, this.speedModifier);
                    }

                } else {
                    this.setBuild(livingentity.blockPosition());
                }
                if (!moved) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            }

            this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
        }

        this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
        this.checkAndPerformAttack(livingentity, distanceToTarget);
    }

    private void tickBuildState(LivingEntity livingentity, double distanceToTarget) {
        // 简化建造模式下结构间冷却计数
        if (this.ticksUntilNextStructureBuild > 0) {
            this.ticksUntilNextStructureBuild--;
        }

        if (livingentity != null && this.checkAndPerformAttack(livingentity, distanceToTarget)) {
            this.setMelee();
            return;
        }

        if (this.buildTargetPos == null || !this.canPathConstruct()) {
            this.setMelee();
            return;
        }

        if (this.buildTargetPos.equals(this.selfPos)) {
            this.setMelee();
            return;
        }

        if (this.pathPack == null) {
            // 简化建造模式下，结构间冷却15 tick（倒计时方式）
            if (simplifiedBuilderMovement() && this.ticksUntilNextStructureBuild > 0) {
                return;
            }
            this.handlePathPackInitialization(livingentity);
            return;
        }

        this.adjustYPos();

        if (this.breakGoal != null && !this.breakGoal.isDone()) {
            return;
        }

        this.executePathConstruction();
    }

    private void tickPreBuildState(LivingEntity target, double distanceToTarget) {
        long elapsed = this.level.getGameTime() - this.preBuildStartTime;

        if (!this.canPathConstruct()) {
            this.setMelee();
            return;
        }

        if (elapsed > PRE_BUILD_DURATION) {
            this.state = State.BUILD;
            this.zombie.getNavigation().stop();
            this.selfPos = this.zombie.blockPosition();
            return;
        }

        if (this.preBuildTargetPos == null) {
            Vec3 randomPos = DefaultRandomPos.getPos(this.zombie, PRE_BUILD_RANGE, PRE_BUILD_VERTICAL_RANGE);
            if (randomPos != null) {
                this.preBuildTargetPos = randomPos;
                this.zombie.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, this.speedModifier);
            } else {
                if (elapsed > 10) {
                    this.state = State.BUILD;
                    this.zombie.getNavigation().stop();
                    this.selfPos = this.zombie.blockPosition();
                }
            }
        } else {
            if (this.zombie.getNavigation().isDone() ||
                    this.zombie.position().distanceToSqr(this.preBuildTargetPos) < PRE_BUILD_REACH_DIST_SQR) {
                this.state = State.BUILD;
                this.zombie.getNavigation().stop();
                this.selfPos = this.zombie.blockPosition();
            }
        }

        this.zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void adjustYPos() {
        if (this.style.is(PathConstructor.Style.JUMP_PRIORITIZED)) {
            if (zombie.getTarget() != null) {
                BlockPos presentTargetPos = this.buildTargetPos;
                BlockPos targetEntityPos = zombie.getTarget().blockPosition();
                if (presentTargetPos.getY() < targetEntityPos.getY()) {
                    this.buildTargetPos = presentTargetPos;
                }
            }
        }
    }

    private void handlePathPackInitialization(LivingEntity livingentity) {
        double distToSelf = this.zombie.blockPosition().distSqr(this.selfPos);

        if (distToSelf <= RETURN_TO_SELF_MIN_DIST_SQR) {
            PathConstructor.PathPack newPathPack = this.pathConstructor.create(this.selfPos, this.buildTargetPos, this.getBuildStyle());

            if (newPathPack == null) {
                this.setMelee();
                return;
            }

            this.pathPack = newPathPack;

            if (this.giveUpHalfway && livingentity != null && this.level.getGameTime() - this.lastGiveUpBuildTime > GIVE_UP_BUILD_TIME) {
                Path path = this.zombie.getNavigation().createPath(livingentity, 0);

                if (path != null) {
                    Node finalNode = path.getEndNode();
                    if (finalNode != null) {
                        BlockPos pathEnd = finalNode.asBlockPos();
                        BlockPos buildEnd = pathPack.pathStructure().getEndPos(pathPack.horizontalDirection(), this.selfPos);

                        if ((Math.sqrt(pathEnd.distSqr(livingentity.blockPosition()) + 10) < Math.sqrt(buildEnd.distSqr(livingentity.blockPosition())))) {
                            this.setMelee();
                            this.zombie.getNavigation().moveTo(path, this.speedModifier / this.data.getTotalMovementSpeedModify());
                            this.lastGiveUpBuildTime = this.level.getGameTime();
                            return;
                        }
                    }
                }
            }

            if (this.pathPack.pathStructure().is(PathConstructor.PathStructure.SITU)) {
                this.setMelee();
                return;
            }

            Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 1);
            if (path1 != null) {
                this.zombie.getNavigation().moveTo(path1, this.speedModifier / this.data.getTotalMovementSpeedModify());
            }
        } else if (distToSelf >= RETURN_TO_SELF_MAX_DIST_SQR) {
            this.setMelee();
        } else {
            if (this.zombie.getNavigation().isDone()) {
                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 1);
                if (path1 != null) {
                    this.zombie.getNavigation().moveTo(path1, this.speedModifier / this.data.getTotalMovementSpeedModify());
                } else {
                    this.setMelee();
                }
            }
        }
    }

    private void executePathConstruction() {
        int pathIndex = 0;
        double distToSelf = this.zombie.blockPosition().distSqr(this.selfPos);

        if (distToSelf >= 1.0) {
            if (this.zombie.getNavigation().isDone()) {
                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 1);
                if (path1 != null) {
                    this.zombie.getNavigation().moveTo(path1, this.speedModifier);
                } else if (distToSelf >= RETURN_TO_SELF_MAX_DIST_SQR) {
                    this.setMelee();
                    return;
                }
            }
        }

        while (pathIndex <= this.pathPack.pathStructure().maxIndex() && pathIndex < MAX_PATH_ITERATIONS) {
            BlockPos pos = this.pathPack.pathStructure().getPos(pathIndex, pathPack.horizontalDirection(), this.selfPos);

            if (isPosIllegal(pos)) {
                this.setMelee();
                return;
            }

            BlockState blockState = this.level.getBlockState(pos);
            PathConstructor.BlockType type = this.pathPack.pathStructure().getType(pathIndex);

            if (!this.verify(pos, blockState, type)) {
                if (!this.execute(pos, blockState, type)) {
                    this.setMelee();
                    return;
                }
                this.consecutiveEmptyPacks = 0;
                break;
            }
            pathIndex++;
        }

        if (pathIndex > this.pathPack.pathStructure().maxIndex()) {
            this.consecutiveEmptyPacks++;
            if (this.consecutiveEmptyPacks >= 6) {
                this.setMelee();
                return;
            }
            this.selfPos = this.pathPack.pathStructure().getEndPos(pathPack.horizontalDirection(), this.selfPos);
            if (simplifiedBuilderMovement() && !(this.pathPack.pathStructure() == PathConstructor.PathStructure.UP || this.pathPack.pathStructure() == PathConstructor.PathStructure.DOWN)) {
                Vec3 newPos = Vec3.atBottomCenterOf(this.selfPos);
                LivingEntity target = this.zombie.getTarget();
                float yRot = this.zombie.getYRot();
                this.zombie.moveTo(newPos.x, newPos.y, newPos.z, yRot, this.zombie.getXRot());
                this.zombie.yBodyRot = yRot;
                this.zombie.yBodyRotO = yRot;
                this.zombie.yHeadRot = yRot;
                this.zombie.yHeadRotO = yRot;
                if (!this.zombie.getNavigation().isDone()) {
                    this.zombie.getNavigation().stop();
                }
            }
            this.pathPack = null;
            if (simplifiedBuilderMovement()) {
                this.ticksUntilNextStructureBuild = 15;
            }
        }
    }

    protected boolean canPathConstruct() {
        return this.pathConstructor != null && (this.canBreak() || this.canPlace()) && this.data.isEmpowered();
    }

    protected boolean checkAndPerformAttack(LivingEntity livingEntity, double distance) {
        double attackReach = this.getAttackReachSqr(livingEntity);
        if (distance <= attackReach && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.zombie.swing(InteractionHand.MAIN_HAND);
            this.zombie.doHurtTarget(livingEntity);
            return true;
        }
        return false;
    }

    private PathConstructor.Style getBuildStyle() {
        return this.style;
    }

    public void setBuildStyle(PathConstructor.Style style) {
        this.style = style;
    }

    protected boolean isPosIllegal(BlockPos pos) {
        return this.level.isOutsideBuildHeight(pos);
    }

    protected boolean verify(BlockPos blockPos, BlockState blockState, PathConstructor.@NotNull BlockType type) {
        if (type.is(PathConstructor.BlockType.EMPTY)) {
            return isEmpty(blockState);
        } else if (type.is(PathConstructor.BlockType.SOLID)) {
            return isSolid(blockPos, blockState);
        } else return false;
    }

    protected boolean execute(BlockPos blockPos, BlockState blockState, PathConstructor.@NotNull BlockType type) {
        if (type.is(PathConstructor.BlockType.SOLID)) {
            if (this.isEmpty(blockState)) {
                if (blockState.getBlock() instanceof PowderSnowCauldronBlock) {
                    return this.destroyBlock(blockPos);
                } else {
                    BlockPos zombiePos = this.zombie.blockPosition();
                    if (zombiePos.above().equals(blockPos) || zombiePos.equals(blockPos)) {
                        this.zombie.getJumpControl().jump();
                        return true;
                    }

                    double distSqr = this.zombie.distanceToSqr(MathUtils.blockPosToVec3(blockPos));
                    if (distSqr <= PLACE_BLOCK_DISTANCE_SQR) {
                        return this.placeBlock(blockPos);
                    } else if (distSqr > TOO_FAR_FROM_BLOCK_SQR) {
                        return false;
                    } else {
                        if (this.simplifiedBuilderMovement()) {
                            return false;
                        }
                        if (zombiePos.distSqr(this.selfPos) >= 4) {
                            if (this.zombie.getNavigation().isDone()) {
                                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 1);
                                if (path1 != null) {
                                    this.zombie.getNavigation().moveTo(path1, this.speedModifier / this.data.getTotalMovementSpeedModify());
                                } else {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                }
            } else {
                return this.destroyBlock(blockPos);
            }
        } else if (type.is(PathConstructor.BlockType.EMPTY)) {
            return this.destroyBlock(blockPos);
        } else {
            this.setMelee();
        }
        return false;
    }

    protected boolean canBreak() {
        if (this.cachedCanBreak == null) {
            this.cachedCanBreak = this.breakGoal != null && ZGRGame.Rules.canZombieBreakBlock(zombie.getServer());
        }
        return this.cachedCanBreak;
    }

    protected boolean canPlace() {
        if (this.cachedCanPlace == null) {
            this.cachedCanPlace = this.placeGoal != null && ZGRGame.Rules.canZombiePlaceBlock(zombie.getServer());
        }
        return this.cachedCanPlace;
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.adjustedTickDelay(20);
    }

    public boolean isInBuildState() {
        return this.state.is(State.BUILD);
    }

    protected boolean isEmpty(BlockState blockState) {
        return (blockState.isAir()
                || !blockState.getFluidState().isEmpty())
                && !(blockState.getBlock() instanceof PowderSnowCauldronBlock);
    }

    protected boolean isSolid(BlockPos blockPos, BlockState blockState) {
        return Block.isShapeFullBlock(blockState.getCollisionShape(this.level, blockPos));
    }

    protected boolean destroyBlock(BlockPos blockPos) {
        if (this.canBreak()) {
            if (this.breakGoal != null && this.breakGoal.isDone()) {
                return this.breakGoal.tryToBreak(blockPos, this::failBreak);
            }
            return true;
        } else return false;
    }

    protected boolean placeBlock(BlockPos blockPos) {
        if (this.canPlace()) {
            return this.placeGoal != null && this.placeGoal.place(blockPos, getPlaceBlock());
        } else return false;
    }

    private void failBreak(Zombie zombie, IZombieData iZombieDataLazyOptional) {
        this.setMelee();
    }

    protected enum State {
        MELEE(0), BUILD(1), PRE_BUILD(2);
        final int ID;

        State(int i) {
            this.ID = i;
        }

        boolean is(State state) {
            return this == state;
        }
    }
}
