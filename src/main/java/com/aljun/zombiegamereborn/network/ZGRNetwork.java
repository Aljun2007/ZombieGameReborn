package com.aljun.zombiegamereborn.network;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.aljun.zombiegamereborn.network.packet.GamePropertyUploadPacket;
import com.aljun.zombiegamereborn.network.packet.LoginWelcomePacket;
import com.aljun.zombiegamereborn.network.packet.OpenClientConfigScreenPacket;
import com.aljun.zombiegamereborn.network.packet.TimeBroadcastPacket;
import com.aljun.zombiegamereborn.network.packet.ZombieCapacitySyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ZGRNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ZombieGameReborn.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.messageBuilder(GamePropertyUploadPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GamePropertyUploadPacket::encode)
                .decoder(GamePropertyUploadPacket::decode)
                .consumerMainThread(GamePropertyUploadPacket::handle)
                .add();

        CHANNEL.messageBuilder(GamePropertyDownloadPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GamePropertyDownloadPacket::encode)
                .decoder(GamePropertyDownloadPacket::decode)
                .consumerMainThread(GamePropertyDownloadPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenClientConfigScreenPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenClientConfigScreenPacket::encode)
                .decoder(OpenClientConfigScreenPacket::decode)
                .consumerMainThread(OpenClientConfigScreenPacket::handle)
                .add();

        CHANNEL.messageBuilder(ZombieCapacitySyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ZombieCapacitySyncPacket::encode)
                .decoder(ZombieCapacitySyncPacket::decode)
                .consumerMainThread(ZombieCapacitySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(TimeBroadcastPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TimeBroadcastPacket::encode)
                .decoder(TimeBroadcastPacket::new)
                .consumerMainThread(TimeBroadcastPacket::handle)
                .add();

        CHANNEL.messageBuilder(LoginWelcomePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LoginWelcomePacket::encode)
                .decoder(LoginWelcomePacket::decode)
                .consumerMainThread(LoginWelcomePacket::handle)
                .add();

    }

    // ==================== 发送方法 ====================

    public static <T> void sendToServer(T packet) {
        CHANNEL.sendToServer(packet);
    }

    public static <T> void sendToClient(T packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <T> void sendToAllClients(T packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static <T> void sendToNearby(T packet, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, double radius) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX(), pos.getY(), pos.getZ(), radius, level.dimension())), packet);
    }

    public static <T> void sendToTrackingEntity(T packet, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
}