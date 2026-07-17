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
import com.aljun.zombiegamereborn.utils.ZombieUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
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
    private static final double TARGET_CLOSE_DIST_SQR = 25.0;
    private static final long GIVE_UP_BUILD_TIME = 200L;
    protected final Zombie zombie;
    protected final int attackInterval = 20;
    public ZombieBreakBlockGoal breakGoal = null;
    public ZombiePlaceBlockGoal placeGoal = null;
    protected double speedModifier = 1;
    protected Path path;
    protected double pathedTargetX;
    protected double pathedTargetY;
    protected double pathedTargetZ;
    protected int ticksUntilNextPathRecalculation;
    protected int ticksUntilNextAttack;
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
    private PathConstructor.Style style = randomStyle();

    private PathConstructor.Style randomStyle() {
        if (RandomUtils.booleanByChance(0.6)) {
            return PathConstructor.Style.JUMP_PRIORITIZED;
        } else return PathConstructor.Style.NORMAL;

    }

    private Boolean cachedCanBreak = null;
    private Boolean cachedCanPlace = null;
    private IZombieData data;

    public ZombieMeleeAndPathBuildGoal(Zombie zombie,IZombieData data) {
        this.zombie = zombie;
        this.pathConstructor = new PathConstructor();
        this.data = data;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
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
                            this.lastHurtAndCanReachPlayerTime = this.zombie.level().getGameTime();
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
        this.lastSetMeleeTime = this.zombie.level().getGameTime();
        this.cachedCanBreak = null;
        this.cachedCanPlace = null;
    }

    protected void setBuild(BlockPos target) {
        long gameTime = this.zombie.level().getGameTime();
        if (gameTime - this.lastSetMeleeTime >= BUILD_COOLDOWN &&
                gameTime - this.lastHurtAndCanReachPlayerTime >= HURT_BUILD_COOLDOWN) {
            this.state = State.BUILD;
            this.buildTargetPos = target;
            this.selfPos = this.zombie.blockPosition();
            this.zombie.getNavigation().stop();
        }
    }

    @Override
    public boolean canUse() {
        this.ensureGoalsInitialized();
        long currentTime = this.zombie.level().getGameTime();
        if (currentTime - this.lastCanUseCheck < COOLDOWN_BETWEEN_CAN_USE_CHECKS) {
            return false;
        } else {
            this.lastCanUseCheck = currentTime;
            LivingEntity livingentity = this.zombie.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else {
                if (canPenalize) {
                    if (--this.ticksUntilNextPathRecalculation <= 0) {
                        this.path = this.zombie.getNavigation().createPath(livingentity, 0);
                        this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);
                        return this.path != null;
                    } else {
                        return true;
                    }
                }
                this.path = this.zombie.getNavigation().createPath(livingentity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingentity) >= this.zombie.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                }
            }
        }
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

    protected double getAttackReachSqr(LivingEntity p_25556_) {
        return this.zombie.getBbWidth() * 2.0F * this.zombie.getBbWidth() * 2.0F + p_25556_.getBbWidth();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.zombie.getTarget();
        if (target == null) {
            return false;
        } else if (!target.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen()) {
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

    @Override
    public void start() {
        this.zombie.getNavigation().moveTo(this.path, this.speedModifier);
        this.zombie.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    @Override
    public void stop() {
        LivingEntity livingentity = this.zombie.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
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

            if (this.state.is(State.BUILD)) {
                this.tickBuildState(livingentity, distanceToTarget);
            }
        }
    }

    private void tickMeleeState(LivingEntity livingentity, double distanceToTarget) {
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if (this.zombie.position().distanceToSqr(livingentity.position()) <= TARGET_CLOSE_DIST_SQR) {
            this.ticksUntilNextPathRecalculation -= 2;
        }

        if ((this.followingTargetEvenIfNotSeen() || this.zombie.getSensing().hasLineOfSight(livingentity))
                && this.ticksUntilNextPathRecalculation <= 0 &&
                (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D ||
                        livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D ||
                        this.zombie.getRandom().nextFloat() < 0.05F)) {
            this.pathedTargetX = livingentity.getX();
            this.pathedTargetY = livingentity.getY();
            this.pathedTargetZ = livingentity.getZ();
            this.ticksUntilNextPathRecalculation = 4 + this.zombie.getRandom().nextInt(7);

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

            if (distanceToTarget > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distanceToTarget > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
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
                    if (this.canPathConstruct() && (this.bridgeGoal == null || !this.bridgeGoal.isPathBuildCooldown())) {
                        this.setBuild(livingentity.blockPosition());
                    }
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
        if (this.checkAndPerformAttack(livingentity, distanceToTarget)) {
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
            this.handlePathPackInitialization(livingentity);
            return;
        }

        this.adjustYPos();

        this.executePathConstruction();
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

            if (this.zombie.level().getGameTime() - this.lastGiveUpBuildTime > GIVE_UP_BUILD_TIME) {
                Path path = this.zombie.getNavigation().createPath(livingentity, 0);

                if (path != null) {
                    Node finalNode = path.getEndNode();
                    if (finalNode != null) {
                        BlockPos pathEnd = finalNode.asBlockPos();
                        BlockPos buildEnd = pathPack.pathStructure().getEndPos(pathPack.horizontalDirection(), this.selfPos);

                        if ((Math.sqrt(pathEnd.distSqr(livingentity.blockPosition()) + 10) < Math.sqrt(buildEnd.distSqr(livingentity.blockPosition())))) {
                            this.setMelee();
                            this.zombie.getNavigation().moveTo(path, this.speedModifier / this.data.getTotalMovementSpeedModify());
                            this.lastGiveUpBuildTime = this.zombie.level().getGameTime();
                            return;
                        }
                    }
                }
            }

            if (this.pathPack != null) {
                if (this.pathPack.pathStructure().is(PathConstructor.PathStructure.SITU)) {
                    this.setMelee();
                    return;
                }
            }

            Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 0);
            if (path1 != null) {
                this.zombie.getNavigation().moveTo(path1, this.speedModifier / this.data.getTotalMovementSpeedModify());
            }
        } else if (distToSelf >= RETURN_TO_SELF_MAX_DIST_SQR) {
            this.setMelee();
        } else {
            if (this.zombie.getNavigation().isDone()) {
                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 0);
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
                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 0);
                if (path1 != null) {
                    this.zombie.getNavigation().moveTo(path1, this.speedModifier);
                }
            }
        }

        while (pathIndex <= this.pathPack.pathStructure().maxIndex() && pathIndex < MAX_PATH_ITERATIONS) {
            BlockPos pos = this.pathPack.pathStructure().getPos(pathIndex, pathPack.horizontalDirection(), this.selfPos);

            if (isPosIllegal(pos)) {
                this.setMelee();
                return;
            }

            BlockState blockState = this.zombie.level().getBlockState(pos);
            PathConstructor.BlockType type = this.pathPack.pathStructure().getType(pathIndex);

            if (!this.verify(pos, blockState, type)) {
                if (!this.execute(pos, blockState, type)) {
                    this.setMelee();
                    return;
                }
                break;
            }
            pathIndex++;
        }

        if (pathIndex > this.pathPack.pathStructure().maxIndex()) {
            this.selfPos = this.pathPack.pathStructure().getEndPos(pathPack.horizontalDirection(), this.selfPos);
            this.pathPack = null;
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
        return this.zombie.level().isOutsideBuildHeight(pos);
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
                    if (this.zombie.blockPosition().above().equals(blockPos) || this.zombie.blockPosition().equals(blockPos)) {
                        this.zombie.getJumpControl().jump();
                        return true;
                    }
                    if (this.zombie.distanceToSqr(MathUtils.blockPosToVec3(blockPos)) <= PLACE_BLOCK_DISTANCE_SQR) {
                        return this.placeBlock(blockPos);
                    } else if (this.zombie.distanceToSqr(MathUtils.blockPosToVec3(blockPos)) > TOO_FAR_FROM_BLOCK_SQR) {
                        return false;
                    } else {
                        if (this.zombie.blockPosition().distSqr(this.selfPos) >= 2) {
                            if (this.zombie.getNavigation().isDone()) {
                                Path path1 = this.zombie.getNavigation().createPath(this.selfPos, 0);
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
        return Block.isShapeFullBlock(blockState.getCollisionShape(this.zombie.level(), blockPos));
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
        MELEE(0), BUILD(1);
        final int ID;

        State(int i) {
            this.ID = i;
        }

        boolean is(State state) {
            return this == state;
        }
    }
}
