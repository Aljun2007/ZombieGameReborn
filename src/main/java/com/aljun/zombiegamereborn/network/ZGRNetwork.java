package com.aljun.zombiegamereborn.network;

import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.aljun.zombiegamereborn.network.packet.GamePropertyUploadPacket;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import com.aljun.zombiegamereborn.network.packet.OpenClientConfigScreenPacket;
import com.aljun.zombiegamereborn.network.packet.TimeBroadcastPacket;
import com.aljun.zombiegamereborn.network.packet.ZombieCapacitySyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.reflect.Method;

public class ZGRNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ZGRNetwork::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(
                GamePropertyUploadPacket.TYPE,
                GamePropertyUploadPacket.STREAM_CODEC,
                GamePropertyUploadPacket::handle
        );

        registrar.playToClient(
                GamePropertyDownloadPacket.TYPE,
                GamePropertyDownloadPacket.STREAM_CODEC,
                (packet, context) -> enqueueClientReflective(context, "handleGamePropertyDownload", packet)
        );

        registrar.playToClient(
                OpenClientConfigScreenPacket.TYPE,
                OpenClientConfigScreenPacket.STREAM_CODEC,
                (packet, context) -> enqueueClientReflective(context, "handleOpenClientConfigScreen", packet)
        );

        registrar.playToClient(
                ZombieCapacitySyncPacket.TYPE,
                ZombieCapacitySyncPacket.STREAM_CODEC,
                (packet, context) -> enqueueClientReflective(context, "handleZombieCapacitySync", packet)
        );

        registrar.playToClient(
                TimeBroadcastPacket.TYPE,
                TimeBroadcastPacket.STREAM_CODEC,
                (packet, context) -> enqueueClientReflective(context, "handleTimeBroadcast", packet)
        );

        registrar.playToClient(
                LoginWelcomePacket.TYPE,
                LoginWelcomePacket.STREAM_CODEC,
                (packet, context) -> enqueueClientReflective(context, "handleLoginWelcome", packet)
        );
    }

    /**
     * 保持原版的反射调用行为：客户端 handler 只在客户端环境加载，避免服务端环境加载客户端类
     */
    private static void enqueueClientReflective(IPayloadContext context, String methodName, Object packet) {
        context.enqueueWork(() -> {
            try {
                Class<?> clazz = Class.forName("com.aljun.zombiegamereborn.common.client.handler.ClientPacketHandlers");
                Method method = clazz.getMethod(methodName, packet.getClass(), IPayloadContext.class);
                method.invoke(null, packet, context);
            } catch (Exception ignored) {
            }
        });
    }

    // ==================== 发送方法 ====================

    public static <T extends CustomPacketPayload> void sendToServer(T packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static <T extends CustomPacketPayload> void sendToClient(T packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static <T extends CustomPacketPayload> void sendToAllClients(T packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static <T extends CustomPacketPayload> void sendToNearby(T packet, Level level, BlockPos pos, double radius) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, pos.getX(), pos.getY(), pos.getZ(), radius, packet);
        }
    }

    public static <T extends CustomPacketPayload> void sendToTrackingEntity(T packet, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
}
