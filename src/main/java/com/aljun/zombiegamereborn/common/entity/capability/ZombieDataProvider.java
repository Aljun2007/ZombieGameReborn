package com.aljun.zombiegamereborn.common.entity.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZombieDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<IZombieData> ZOMBIE_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private IZombieData data = null;
    private final LazyOptional<IZombieData> optional = LazyOptional.of(this::createZombieData);

    private IZombieData createZombieData() {
        if (data == null) {
            data = new ZombieData();
        }
        return data;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ZOMBIE_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        createZombieData();
        return serializeNBT(data);
    }

    public static CompoundTag serializeNBT(IZombieData data) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isSunSensitive", data.isSunSensitive());
        tag.putString("type", data.getTypeID().toString());
        tag.putDouble("miningSpeed", data.getMiningSpeed());
        tag.putBoolean("canSwim", data.canSwim());
        tag.putBoolean("isTypeInitialized", data.isTypeInitialized());
        tag.putBoolean("fireImmune", data.fireImmune());
        tag.putBoolean("isEmpowered", data.isEmpowered());
        tag.putDouble("movement_speed_modify", data.getTotalMovementSpeedModify());
        tag.putBoolean("canJumpAttack", data.canJumpAttack());
        tag.putBoolean("enhancedSense", data.enhancedSense());
        tag.putBoolean("followMustSee", data.followMustSee());
        tag.putBoolean("canZombieContinueUseWeaponsInHand", data.canZombieContinueUseWeaponsInHand());
        tag.putBoolean("fleeSun",data.fleeSun());
        tag.putDouble("ambientVolumeModify", data.getAmbientVolumeModify());
        tag.putDouble("stepVolumeModify", data.getStepVolumeModify());
        if (data.getCustomLootTable() != null) {
            tag.putString("customLootTable", data.getCustomLootTable().toString());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createZombieData();
        applyData(data, tag);
    }

    public static void applyData(IZombieData data, CompoundTag tag) {
        data.setSunSensitive(tag.getBoolean("isSunSensitive"));
        data.setTypeID(ResourceLocation.parse(tag.getString("type")));
        data.setMiningSpeed(tag.getDouble("miningSpeed"));
        data.enableSwim(tag.getBoolean("canSwim"));
        data.setTypeInitialized(tag.getBoolean("isTypeInitialized"));
        data.setFireImmune(tag.getBoolean("fireImmune"));
        data.setEmpowered(tag.getBoolean("isEmpowered"));
        data.setAttributesMovementSpeedModify(tag.getDouble("movementSpeedModify"));
        data.enableJumpAttack(tag.getBoolean("canJumpAttack"));
        data.setEnhancedSense(tag.getBoolean("enhancedSense"));
        data.setFollowMustSee(tag.getBoolean("followMustSee"));
        data.setZombieContinueUseWeaponsInHand(tag.getBoolean("canZombieContinueUseWeaponsInHand"));
        data.setFleeSun(tag.getBoolean("fleeSun"));
        data.setAmbientVolumeModify(tag.getDouble("ambientVolumeModify"));
        data.setStepVolumeModify(tag.getDouble("stepVolumeModify"));
        if (tag.contains("customLootTable")) {
            data.setCustomLootTable(ResourceLocation.parse(tag.getString("customLootTable")));
        }
    }
}