package com.aljun.zombiegamereborn.common.player.capability;

public class PlayerData implements IPlayerData {

    private long survivedDay = 1L;
    private long undergroundDay = 0L;
    private long undergroundGameTime = 0L;
    private long lastEstimatedDay = 0L;
    private long totalZombieKills = 0L;

    @Override
    public long getSurvivedDay() { return survivedDay; }

    @Override
    public void setSurvivedDay(long day) { this.survivedDay = Math.max(0L, day); }

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
}
