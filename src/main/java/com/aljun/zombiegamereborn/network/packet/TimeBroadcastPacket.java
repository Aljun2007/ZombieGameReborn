package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.common.client.config.ClientConfigManager;
import com.aljun.zombiegamereborn.common.game.DayTime;
import com.aljun.zombiegamereborn.sounds.ZGRSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TimeBroadcastPacket {

    private final DisplayType displayType;
    private final long day;
    private final int dayTimeID;
    private final String chatComponentJson;
    private final long dayTime;
    private final long estimatedDay;
    private final boolean showTime;
    private boolean timeAlarmEnabled = false; // 默认不响，仅 CHAT_MESSAGE 时传 true

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

    private static long getDisplayDay(long day, long dayTime) {
        return dayTime >= 0 && Math.floorMod(dayTime, 24000L) >= 18000 ? day + 1 : day;
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                if (!ClientConfigManager.get().timeBroadcastEnabled) return;

                Minecraft mc = Minecraft.getInstance();
                switch (displayType) {
                    case CENTER_SUBTITLE -> {
                        DayTime dt = DayTime.values()[dayTimeID];
                        Component title = Component.translatable("gui.zombiegamereborn.time_broadcast.day_title",
                                getDisplayDay(this.day, this.dayTime));
                        MutableComponent subTitle = Component.translatable("daytime.zombiegamereborn." + dt.id);
                        if (showTime) {
                            subTitle.append("§l | §r").append(DayTime.transformToTime(this.dayTime));
                        }
                        subTitle.withStyle(ChatFormatting.GRAY);
                        mc.gui.setTitle(title);
                        mc.gui.setSubtitle(subTitle);
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case DAY_ONLY -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_title",
                                getDisplayDay(this.day, this.dayTime)));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case UNDERGROUND_ESTIMATE -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_estimate", this.estimatedDay));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case GARBLED -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_garbled", 1));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case CHAT_MESSAGE -> {
                        if (!chatComponentJson.isEmpty()) {
                            Component chatMsg = Component.Serializer.fromJson(chatComponentJson);
                            if (chatMsg != null && mc.player != null) {
                                if (ClientConfigManager.get().timeBroadcastEnabled) {
                                    mc.player.displayClientMessage(chatMsg, false);
                                    if (timeAlarmEnabled && ClientConfigManager.get().timeAlarmEnabled) {
                                        mc.player.playSound(ZGRSoundEvents.CLOCK_RING, 0.5f, 1.5f);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        });
        context.setPacketHandled(true);
    }

    public enum DisplayType {
        CENTER_SUBTITLE,
        DAY_ONLY,
        UNDERGROUND_ESTIMATE,
        GARBLED,
        CHAT_MESSAGE
    }
}
