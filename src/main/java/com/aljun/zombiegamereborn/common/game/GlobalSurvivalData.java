package com.aljun.zombiegamereborn.common.game;

import com.aljun.zombiegamereborn.api.ZGRCommonAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class GlobalSurvivalData extends SavedData {
    private static final String DATA_NAME = "global_survival_days";

    private long survivedDays = 1;
    private long lastGameTime = -1;
    private boolean wasAnyPlayerOnline = false;
    private boolean wasNight = false;
    private long lastIncrementDayGameTime = -1;

    public GlobalSurvivalData() {}

    public void setDays(long days) {
        // 双重检测，即使 API 层没检测，底层也会保护
        if (days < ZGRCommonAPI.SurvivalDaysAPI.MIN_DAYS) days = ZGRCommonAPI.SurvivalDaysAPI.MIN_DAYS;
        if (days > ZGRCommonAPI.SurvivalDaysAPI.MAX_DAYS) days = ZGRCommonAPI.SurvivalDaysAPI.MAX_DAYS;
        this.survivedDays = days;
        this.setDirty();
    }

    // 加载时的检测
    public GlobalSurvivalData(CompoundTag tag) {
        this.survivedDays = tag.getLong("Days");
        if (this.survivedDays < ZGRCommonAPI.SurvivalDaysAPI.MIN_DAYS) this.survivedDays = ZGRCommonAPI.SurvivalDaysAPI.MIN_DAYS;
        if (this.survivedDays > ZGRCommonAPI.SurvivalDaysAPI.MAX_DAYS) this.survivedDays = ZGRCommonAPI.SurvivalDaysAPI.MAX_DAYS;
        this.lastGameTime = tag.getLong("LastGameTime");
        this.wasAnyPlayerOnline = tag.getBoolean("WasAnyPlayerOnline");
        this.wasNight = tag.getBoolean("WasNight");
        this.lastIncrementDayGameTime = tag.getLong("LastIncrementDayGameTime");
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putLong("Days", survivedDays);
        tag.putLong("LastGameTime", lastGameTime);
        tag.putBoolean("WasAnyPlayerOnline", wasAnyPlayerOnline);
        tag.putBoolean("WasNight", wasNight);
        tag.putLong("LastIncrementDayGameTime", lastIncrementDayGameTime);
        return tag;
    }

    public static GlobalSurvivalData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalSurvivalData::new,
                GlobalSurvivalData::new,
                DATA_NAME
        );
    }

    // Getter
    public long getDays() { return survivedDays; }
    public long getLastGameTime() { return lastGameTime; }
    public boolean wasAnyPlayerOnline() { return wasAnyPlayerOnline; }
    public boolean wasNight() { return wasNight; }

    public void addDays(long delta) {
        setDays(this.survivedDays + delta);
    }

    public void incrementDay() {
        this.survivedDays++;
        this.lastIncrementDayGameTime = this.lastGameTime;
        this.setDirty();
    }

    public void setWasAnyPlayerOnline(boolean value) {
        this.wasAnyPlayerOnline = value;
        this.setDirty();
    }

    public void setWasNight(boolean value) {
        this.wasNight = value;
        this.setDirty();
    }

    public void updateTickState(long gameTime, boolean hasPlayers, boolean isNight) {
        this.lastGameTime = gameTime;
        this.wasAnyPlayerOnline = hasPlayers;
        this.wasNight = isNight;
        this.setDirty();
    }

    public long getLastIncrementDayGameTime() {
        return lastIncrementDayGameTime;
    }
}