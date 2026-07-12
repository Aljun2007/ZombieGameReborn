package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.game.DayTime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class TimeBroadcastPacket {

    private final DisplayType displayType;
    private final long day;
    private final int dayTimeID;
    private final String chatComponentJson;
    private final long dayTime;
    private final long estimatedDay;
    private final boolean showTime;
    private boolean timeAlarmEnabled = false;

    // CENTER_SUBTITLE: (day, dayTimeID, dayTime, showTime)
    public TimeBroadcastPacket(long day, DayTime dayTimeID, long dayTime, boolean showTime) {
        this.displayType = DisplayType.CENTER_SUBTITLE;
        this.day = day;
        this.dayTimeID = dayTimeID.ordinal();
        this.dayTime = dayTime;
        this.showTime = showTime;
        this.chatComponentJson = "";
        this.estimatedDay = -1;
    }

    // DAY_ONLY: (day, false) — 仅显示 "第 X 天"
    // UNDERGROUND_ESTIMATE: (estimatedDay, true) — "第 X 天 ？"
    public TimeBroadcastPacket(long value, boolean isEstimate) {
        this.displayType = isEstimate ? DisplayType.UNDERGROUND_ESTIMATE : DisplayType.DAY_ONLY;
        this.day = isEstimate ? -1 : value;
        this.estimatedDay = isEstimate ? value : -1;
        this.dayTimeID = -1;
        this.dayTime = -1;
        this.showTime = false;
        this.chatComponentJson = "";
    }

    // GARBLED: () — 乱码标题
    public TimeBroadcastPacket() {
        this.displayType = DisplayType.GARBLED;
        this.day = -1;
        this.dayTimeID = -1;
        this.dayTime = -1;
        this.showTime = false;
        this.estimatedDay = -1;
        this.chatComponentJson = "";
    }

    // CHAT_MESSAGE 构造器：
    public TimeBroadcastPacket(Component chatComponent) {
        this.displayType = DisplayType.CHAT_MESSAGE;
        this.chatComponentJson = Component.Serializer.toJson(chatComponent);
        this.timeAlarmEnabled = true;
        this.day = -1;
        this.dayTimeID = -1;
        this.dayTime = -1;
        this.showTime = false;
        this.estimatedDay = -1;
    }

    public TimeBroadcastPacket(FriendlyByteBuf buf) {
        this.displayType = buf.readEnum(DisplayType.class);
        this.day = buf.readLong();
        this.dayTimeID = buf.readInt();
        this.dayTime = buf.readLong();
        this.showTime = buf.readBoolean();
        this.estimatedDay = buf.readLong();
        this.chatComponentJson = buf.readUtf();
        this.timeAlarmEnabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.displayType);
        buf.writeLong(this.day);
        buf.writeInt(this.dayTimeID);
        buf.writeLong(this.dayTime);
        buf.writeBoolean(this.showTime);
        buf.writeLong(this.estimatedDay);
        buf.writeUtf(this.chatComponentJson);
        buf.writeBoolean(this.timeAlarmEnabled);
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public long getDay() {
        return day;
    }

    public int getDayTimeID() {
        return dayTimeID;
    }

    public String getChatComponentJson() {
        return chatComponentJson;
    }

    public long getDayTime() {
        return dayTime;
    }

    public long getEstimatedDay() {
        return estimatedDay;
    }

    public boolean isShowTime() {
        return showTime;
    }

    public boolean isTimeAlarmEnabled() {
        return timeAlarmEnabled;
    }

    public enum DisplayType {
        CENTER_SUBTITLE,
        DAY_ONLY,
        UNDERGROUND_ESTIMATE,
        GARBLED,
        CHAT_MESSAGE
    }
}
