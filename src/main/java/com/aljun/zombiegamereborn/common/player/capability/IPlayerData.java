package com.aljun.zombiegamereborn.common.player.capability;

public interface IPlayerData {

    long getSurvivedDay();
    void setSurvivedDay(long day);

    long getUndergroundDay();
    void setUndergroundDay(long day);

    long getUndergroundGameTime();
    void setUndergroundGameTime(long gameTime);

    long getLastEstimatedDay();
    void setLastEstimatedDay(long day);
}
