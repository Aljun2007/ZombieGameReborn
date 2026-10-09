package com.aljun.zombiegamereborn.common.entity.zombieType;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.accessor.INearestAttackableTargetGoalAccessor;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import com.aljun.zombiegamereborn.common.entity.goal.behavior.*;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieNearestAttackableTargetGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombiePiglinCollisionTargetGoal;
import com.aljun.zombiegamereborn.common.entity.goal.target.ZombieSenseTargetGoal;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.common.game.ZombieStatic;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.AdvancementHandler;
import com.aljun.zombiegamereborn.network.packet.ZombieCapacitySyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;

public class ZombieTypeManager {

    public static void initializeZombie(Zombie zombie, ResourceLocation typeID) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null) return;
        if (data.isTypeInitialized()) {
            return;
        }
        data.setTypeInitialized(true);
        ZGRZombieAttributesAPI.setTypeID(data, typeID);
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).zombieProperty.loadZombieAttributes(zombie);
            type.onInitializeZombieAttributes(zombie, data);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            type.onInitializeZombieEquipment(zombie, data);
            type.onInitializeZombieEnchantment(zombie, data);
        }

        if (zombie instanceof ZombifiedPiglin && zombie.getMainHandItem().isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }

        if (!zombie.level().isClientSide) {
            syncToClient(zombie);
        }
    }

    public static void syncToClient(Zombie zombie) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null) return;
        CompoundTag tag = ZombieDataProvider.serializeNBT(data);
        ZGRNetwork.sendToTrackingEntity(new ZombieCapacitySyncPacket(zombie.getId(), tag), zombie);
    }

    public static void initializeZombieWithNoWeaponAndArmor(Zombie zombie, ResourceLocation typeID) {
        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null) return;
        if (data.isTypeInitialized()) {
            return;
        }
        data.setTypeInitialized(true);
        ZGRZombieAttributesAPI.setTypeID(data, typeID);
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).zombieProperty.loadZombieAttributes(zombie);
            type.onInitializeZombieAttributes(zombie, data);
        }

        if (!zombie.level().isClientSide) {
            syncToClient(zombie);
        }
    }

    public static void tickZombie(Zombie zombie) {
        if (zombie.level().isClientSide) {
            return;
        }

        IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
        if (data == null) return;
        if (!data.isTypeInitialized()) {
            return;
        }
        ZombieType type = ZGRZombieAttributesAPI.getType(data);
        if (type != null) {
            int tickCount = data.getTickCount();
            if (tickCount == 0) {
                initialGoal(zombie, data, type);
            }
            ZombieStatic.incrementZombieCount(zombie, data);

            ZombieSenseTargetGoal senseGoal = data.getZombieSenseTargetGoalGoal();
            if (senseGoal != null) {
                senseGoal.tickDecay();
            }

            type.onTick(zombie, data, tickCount);

            // 每 tick 重建 Empower 计数（onTick 中授权可能刚生效，getAndSet(0) 在下个 START 清零）
            ZombieStatic.countEmpoweredIfApplicable(data);

            data.incrementTick();

            if (tickCount > 0 && tickCount % 100 == 0) {
                checkArmorAchievements(zombie);
            }
        }
    }

    private static void initialGoal(Zombie zombie, IZombieData data, ZombieType type) {
        type.onInitializeZombieGoals(zombie, data);
        if (type.canBreakBlocks()) {
            ZombieBreakBlockGoal breakBlockGoal = new ZombieBreakBlockGoal(zombie, data);
            data.setZombieBreakBlockGoal(breakBlockGoal);
            zombie.goalSelector.addGoal(1, breakBlockGoal);
            zombie.goalSelector.addGoal(2, new ClearHeadBlockGoal(zombie, data));
            {
                zombie.goalSelector.addGoal(4, new ZombieRemoveLightSourceGoal(zombie, data));
            }
        }
        if (type.canPlaceBlock()) {
            ZombiePlaceBlockGoal placeBlockGoal = new ZombiePlaceBlockGoal(zombie, data);
            if (zombie.getOffhandItem().getItem() instanceof BlockItem blockItem) {
                placeBlockGoal.setDefaultPlaceBlock(() -> blockItem.getBlock().defaultBlockState());
            }
            data.setZombiePlaceBlockGoal(placeBlockGoal);
            zombie.goalSelector.addGoal(1, placeBlockGoal);
            if (zombie instanceof Drowned) {
                zombie.goalSelector.addGoal(3, new DrownedWaterBridgeBuildGoal(zombie, data));
            } else {
                zombie.goalSelector.addGoal(3, new ZombieWaterBridgeBuildGoal(zombie, data));
            }
        }
        if (data.canSwim()) {
            if (!(zombie instanceof Drowned || zombie instanceof ZombieVillager)) {
                zombie.goalSelector.addGoal(1, new ZombieFloatGoal(zombie));
            }
        }
        if (data.canJumpAttack()) {
            zombie.goalSelector.addGoal(3, new JumpAttackGoal(zombie));
        }
        // 梯子攀爬已通过 WalkNodeEvaluatorMixin 注入 PathFinder 实现，无需独立的 Goal
        boolean piglinAngry = false;
        if (zombie instanceof ZombifiedPiglin) {
            zombie.targetSelector.addGoal(2, new ZombiePiglinCollisionTargetGoal(zombie));
            if (zombie.getServer() != null && ZGRGame.getGameProperty().getStageProperty((ServerLevel) zombie.level(), zombie.blockPosition()).zombieProperty.piglinAngryMode) {
                zombie.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(zombie, Player.class, true));
                zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, AbstractPiglin.class, true));
                zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, Hoglin.class, true));
                piglinAngry = true;
            }
        }
        if (data.enhancedSense()) {
            ZombieSenseTargetGoal senseGoal = new ZombieSenseTargetGoal(zombie);
            data.setZombieSenseTargetGoalGoal(senseGoal);
            if (zombie instanceof ZombifiedPiglin && piglinAngry) {
                senseGoal.setPiglinAngryMode();
            }
            zombie.targetSelector.addGoal(4, senseGoal);
        }
        if (data.fleeSun()&& !(zombie instanceof Drowned)) {
            zombie.goalSelector.addGoal(2, new ZombieRestrictSunGoal(zombie, data));
            zombie.goalSelector.addGoal(3, new ZombieFleeSunGoal(zombie));
        }
        // Targeting — 合并所有 NearestAttackableTargetGoal 为统一目标管理
        // mustSee 始终为 false，保留原版村民穿墙感知行为
        // followMustSee 仅控制寻路行为，由攻击 Goal 处理
        ZombieNearestAttackableTargetGoal newTargetGoal = new ZombieNearestAttackableTargetGoal(zombie, false, false);

        for (WrappedGoal wrapped : zombie.targetSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof INearestAttackableTargetGoalAccessor accessor) {
                newTargetGoal.addTarget(
                        wrapped.getPriority(),
                        accessor.get_targetType(),
                        accessor.get_targetingConditions()
                );
            }
        }

        // 2. 注册新目标（先注册再加入，确保目标选取不中断）
        zombie.targetSelector.addGoal(3, newTargetGoal);

        // 3. 移除所有已迁移的旧目标
        zombie.targetSelector.removeAllGoals(goal ->
                goal instanceof NearestAttackableTargetGoal<?>
        );

        // 4. mustSee 已在构造中设为 false，无需额外同步
    }

    private static void checkArmorAchievements(Zombie zombie) {
        int netheritePieces = 0;
        int diamondPieces = 0;

        for (ItemStack stack : zombie.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armor)) continue;
            if (armor.getMaterial() == ArmorMaterials.NETHERITE) {
                netheritePieces++;
            } else if (armor.getMaterial() == ArmorMaterials.DIAMOND) {
                diamondPieces++;
            }
        }

        // 既没有下界合金也没有钻石 → 直接返回
        if (netheritePieces < 2 && diamondPieces < 2) return;

        AABB range = zombie.getBoundingBox().inflate(30.0);
        for (Player player : zombie.level().getEntitiesOfClass(Player.class, range)) {
            if (!(player instanceof ServerPlayer sp)) continue;
            if (netheritePieces >= 2) {
                AdvancementHandler.grantCoverInDebris(sp);
            }
            if (diamondPieces >= 2) {
                AdvancementHandler.grantDiamondProtects(sp);
            }
        }
    }
}

