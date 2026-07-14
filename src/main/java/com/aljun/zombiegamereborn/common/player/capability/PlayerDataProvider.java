package com.aljun.zombiegamereborn.common.player.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<IPlayerData> PLAYER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private IPlayerData data = null;
    private final LazyOptional<IPlayerData> optional = LazyOptional.of(this::createData);

    private IPlayerData createData() {
        if (data == null) {
            data = new PlayerData();
        }
        return data;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        createData();
        CompoundTag tag = new CompoundTag();
        tag.putLong("survivedDay", data.getSurvivedDay());
        tag.putLong("undergroundDay", data.getUndergroundDay());
        tag.putLong("undergroundGameTime", data.getUndergroundGameTime());
        tag.putLong("lastEstimatedDay", data.getLastEstimatedDay());
        tag.putLong("totalZombieKills", data.getTotalZombieKills());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createData();
        if (tag.contains("survivedDay")) {
            data.setSurvivedDay(tag.getLong("survivedDay"));
        }
        if (tag.contains("undergroundDay")) {
            data.setUndergroundDay(tag.getLong("undergroundDay"));
        }
        if (tag.contains("undergroundGameTime")) {
            data.setUndergroundGameTime(tag.getLong("undergroundGameTime"));
        }
        if (tag.contains("lastEstimatedDay")) {
            data.setLastEstimatedDay(tag.getLong("lastEstimatedDay"));
        }
        if (tag.contains("totalZombieKills")) {
            data.setTotalZombieKills(tag.getLong("totalZombieKills"));
        }
    }
}
