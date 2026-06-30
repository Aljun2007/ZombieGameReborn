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

    private IZombieData zombieData = null;
    private final LazyOptional<IZombieData> optional = LazyOptional.of(this::createZombieData);

    private IZombieData createZombieData() {
        if (zombieData == null) {
            zombieData = new ZombieData();
        }
        return zombieData;
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
        CompoundTag tag = new CompoundTag();
        createZombieData();
        tag.putBoolean("isSunSensitive", zombieData.isSunSensitive());
        tag.putString("type", zombieData.getTypeID().toString());
        tag.putDouble("miningSpeed", zombieData.getMiningSpeed());
        tag.putBoolean("canSwim", zombieData.canSwim());
        tag.putBoolean("isTypeInitialized",zombieData.isTypeInitialized());
        tag.putBoolean("fireImmune", zombieData.fireImmune());
        tag.putBoolean("isEmpowered", zombieData.isEmpowered());
        tag.putDouble("movement_speed_modify",zombieData.getMovementSpeedModify());
        tag.putBoolean("canJumpAttack", zombieData.canJumpAttack());
        tag.putBoolean("enhancedSense",zombieData.enhancedSense());
        tag.putBoolean("followMustSee", zombieData.followMustSee());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createZombieData();
        zombieData.setSunSensitive(tag.getBoolean("isSunSensitive"));
        zombieData.setTypeID(ResourceLocation.parse(tag.getString("type")));
        zombieData.setMiningSpeed(tag.getDouble("miningSpeed"));
        zombieData.enableSwim(tag.getBoolean("canSwim"));
        zombieData.setTypeInitialized(tag.getBoolean("isTypeInitialized"));
        zombieData.setFireImmune(tag.getBoolean("fireImmune"));
        zombieData.setEmpowered(tag.getBoolean("isEmpowered"));
        zombieData.setMovementSpeedModify(tag.getDouble("movementSpeedModify"));
        zombieData.enableJumpAttack(tag.getBoolean("canJumpAttack"));
        zombieData.setEnhancedSense(tag.getBoolean("enhancedSense"));
        zombieData.setFollowMustSee(tag.getBoolean("followMustSee"));
    }
}