package com.aljun.zombiegamereborn.common.game;

public enum DayTime {

    DAWN(0, 999, "dawn"),
    MORNING(1000, 5999, "morning"),
    NOON(6000, 6999, "noon"),
    AFTERNOON(7000, 11999, "afternoon"),
    SUNSET(12000, 12999, "sunset"),
    EARLY_NIGHT(13000, 17999, "early_night"),
    MIDNIGHT(18000, 18999, "midnight"),
    LATE_NIGHT(19000, 22999, "late_night"),
    PRE_DAWN(23000, 23999, "pre_dawn");

    public final int start;
    public final int end;
    public final String id;

    DayTime(int start, int end, String id) {
        this.start = start;
        this.end = end;
        this.id = id;
    }

    public static DayTime fromDayTime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        for (DayTime dt : values()) {
            if (time >= dt.start && time <= dt.end) {
                return dt;
            }
        }
        return DAWN;
    }

    public static String transformToTime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        int totalMinutes = (int) (time * 60 / 1000);
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
