package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.game.DayTime;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class TimeBroadcastPacket implements CustomPacketPayload {

    public static final Type<TimeBroadcastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "time_broadcast"));

    public static final StreamCodec<FriendlyByteBuf, TimeBroadcastPacket> STREAM_CODEC =
            StreamCodec.ofMember(TimeBroadcastPacket::encode, TimeBroadcastPacket::decode);

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
        this.chatComponentJson = ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, chatComponent)
                .getOrThrow().toString();
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

    public static TimeBroadcastPacket decode(FriendlyByteBuf buf) {
        return new TimeBroadcastPacket(buf);
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

    /**
     * 从 JSON 字符串反序列化 Component（替代 1.20.1 的 Component.Serializer.fromJson）
     */
    public static Component componentFromJson(String json) {
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                    .getOrThrow();
        } catch (Exception e) {
            return Component.empty();
        }
    }

    public enum DisplayType {
        CENTER_SUBTITLE,
        DAY_ONLY,
        UNDERGROUND_ESTIMATE,
        GARBLED,
        CHAT_MESSAGE
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
