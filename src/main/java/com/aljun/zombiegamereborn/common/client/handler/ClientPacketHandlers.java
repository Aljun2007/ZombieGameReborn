package com.aljun.zombiegamereborn.common.client.handler;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.client.config.ClientConfigManager;
import com.aljun.zombiegamereborn.common.client.gui.config.client.ClientConfigScreen;
import com.aljun.zombiegamereborn.common.client.gui.config.stage.ServerGamePropertyScreen;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import com.aljun.zombiegamereborn.common.game.DayTime;
import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import com.aljun.zombiegamereborn.network.packet.OpenClientConfigScreenPacket;
import com.aljun.zombiegamereborn.network.packet.TimeBroadcastPacket;
import com.aljun.zombiegamereborn.network.packet.ZombieCapacitySyncPacket;
import com.aljun.zombiegamereborn.sounds.ZGRSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    // ==================== GamePropertyDownloadPacket ====================

    public static void handleGamePropertyDownload(GamePropertyDownloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().equals(PacketFlow.CLIENTBOUND)) {
                Minecraft.getInstance().setScreen(new ServerGamePropertyScreen("游戏配置", packet.getSettings()));
            }
        });
    }

    // ==================== OpenClientConfigScreenPacket ====================

    public static void handleOpenClientConfigScreen(OpenClientConfigScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().equals(PacketFlow.CLIENTBOUND)) {
                Minecraft.getInstance().setScreen(new ClientConfigScreen("客户端配置", packet.getSettings()));
            }
        });
    }

    // ==================== TimeBroadcastPacket ====================

    private static long getDisplayDay(long day, long dayTime) {
        return dayTime >= 0 && Math.floorMod(dayTime, 24000L) >= 18000 ? day + 1 : day;
    }

    public static void handleTimeBroadcast(TimeBroadcastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                if (!ClientConfigManager.get().timeBroadcastEnabled) return;

                Minecraft mc = Minecraft.getInstance();
                switch (packet.getDisplayType()) {
                    case CENTER_SUBTITLE -> {
                        DayTime dt = DayTime.values()[packet.getDayTimeID()];
                        Component title = Component.translatable("gui.zombiegamereborn.time_broadcast.day_title",
                                getDisplayDay(packet.getDay(), packet.getDayTime()));
                        MutableComponent subTitle = Component.translatable("daytime.zombiegamereborn." + dt.id);
                        if (packet.isShowTime()) {
                            subTitle.append("§l | §r").append(DayTime.transformToTime(packet.getDayTime()));
                        }
                        subTitle.withStyle(ChatFormatting.GRAY);
                        mc.gui.setTitle(title);
                        mc.gui.setSubtitle(subTitle);
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case DAY_ONLY -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_title",
                                getDisplayDay(packet.getDay(), packet.getDayTime())));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case UNDERGROUND_ESTIMATE -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_estimate", packet.getEstimatedDay()));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case GARBLED -> {
                        mc.gui.setTitle(Component.translatable("gui.zombiegamereborn.time_broadcast.day_garbled", 1));
                        mc.gui.setTimes(10, 70, 20);
                    }
                    case CHAT_MESSAGE -> {
                        if (!packet.getChatComponentJson().isEmpty()) {
                            Component chatMsg = TimeBroadcastPacket.componentFromJson(packet.getChatComponentJson());
                            if (chatMsg != null && mc.player != null) {
                                if (ClientConfigManager.get().timeBroadcastEnabled) {
                                    if (packet.isTimeAlarmEnabled() && ClientConfigManager.get().timeAlarmEnabled) {
                                        mc.player.displayClientMessage(chatMsg, false);
                                        mc.player.playSound(ZGRSoundEvents.CLOCK_RING, 0.5f, 1.5f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    // ==================== LoginWelcomePacket ====================

    public static void handleLoginWelcome(LoginWelcomePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                if (!ClientConfigManager.get().loginMessageEnabled) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;

                MutableComponent link = Component.translatable("gui.zombiegamereborn.login_welcome.link")
                        .withStyle(ChatFormatting.AQUA)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/zombiegamereborn config client"))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("gui.zombiegamereborn.login_welcome.client_hover")))
                        );

                mc.player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.login_welcome.line1", link.copy()),
                        false
                );

                mc.player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.login_welcome.line2"),
                        false
                );

                mc.player.displayClientMessage(
                        Component.translatable("gui.zombiegamereborn.login_welcome.line4"),
                        false
                );

                if (packet.isOp()) {
                    MutableComponent adminLink = Component.translatable("gui.zombiegamereborn.login_welcome.link")
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/zombiegamereborn config gameProperty"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable("gui.zombiegamereborn.login_welcome.admin_hover")))
                            );

                    mc.player.displayClientMessage(
                            Component.translatable("gui.zombiegamereborn.login_welcome.line3", adminLink),
                            false
                    );
                }
            }
        });
    }

    // ==================== ZombieCapacitySyncPacket ====================

    public static void handleZombieCapacitySync(ZombieCapacitySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                if (Minecraft.getInstance().level == null) return;
                Entity entity = Minecraft.getInstance().level.getEntity(packet.getEntityId());
                if (entity instanceof Zombie zombie) {
                    IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
                    ZombieDataProvider.applyData(data, packet.getDataTag());
                }
            }
        });
    }
}
