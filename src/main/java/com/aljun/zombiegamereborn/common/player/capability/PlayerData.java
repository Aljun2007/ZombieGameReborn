package com.aljun.zombiegamereborn.common.player.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class PlayerData implements IPlayerData, INBTSerializable<CompoundTag> {

    private long survivedDay = 1L;
    private long undergroundDay = 0L;
    private long undergroundGameTime = 0L;
    private long lastEstimatedDay = 0L;
    private long totalZombieKills = 0L;

    @Override
    public long getSurvivedDay() { return survivedDay; }

    @Override
    public void setSurvivedDay(long day) { this.survivedDay = Math.max(1L, day); }

    @Override
    public long getUndergroundDay() { return undergroundDay; }

    @Override
    public void setUndergroundDay(long day) { this.undergroundDay = day; }

    @Override
    public long getUndergroundGameTime() { return undergroundGameTime; }

    @Override
    public void setUndergroundGameTime(long gameTime) { this.undergroundGameTime = gameTime; }

    @Override
    public long getLastEstimatedDay() { return lastEstimatedDay; }

    @Override
    public void setLastEstimatedDay(long day) { this.lastEstimatedDay = day; }

    @Override
    public long getTotalZombieKills() { return totalZombieKills; }

    @Override
    public void setTotalZombieKills(long kills) { this.totalZombieKills = Math.max(0L, kills); }

    // ==================== INBTSerializable（原 PlayerDataProvider 的 NBT 逻辑） ====================

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("survivedDay", this.getSurvivedDay());
        tag.putLong("undergroundDay", this.getUndergroundDay());
        tag.putLong("undergroundGameTime", this.getUndergroundGameTime());
        tag.putLong("lastEstimatedDay", this.getLastEstimatedDay());
        tag.putLong("totalZombieKills", this.getTotalZombieKills());

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("survivedDay")) {
            this.setSurvivedDay(tag.getLong("survivedDay"));
        }
        if (tag.contains("undergroundDay")) {
            this.setUndergroundDay(tag.getLong("undergroundDay"));
        }
        if (tag.contains("undergroundGameTime")) {
            this.setUndergroundGameTime(tag.getLong("undergroundGameTime"));
        }
        if (tag.contains("lastEstimatedDay")) {
            this.setLastEstimatedDay(tag.getLong("lastEstimatedDay"));
        }
        if (tag.contains("totalZombieKills")) {
            this.setTotalZombieKills(tag.getLong("totalZombieKills"));
        }
    }
}
