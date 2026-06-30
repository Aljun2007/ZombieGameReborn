package com.aljun.zombiegamereborn.network;

import com.aljun.zombiegamereborn.ZombieGameReborn;
import com.aljun.zombiegamereborn.network.packet.DebugGuiPacket;
import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.aljun.zombiegamereborn.network.packet.GamePropertyUploadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        // 包ID: 0 - DebugGuiPacket (服务端 -> 客户端)
        CHANNEL.messageBuilder(DebugGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DebugGuiPacket::toBytes)
                .decoder(DebugGuiPacket::new)
                .consumerMainThread(DebugGuiPacket::handle)
                .add();

        // 包ID: 1 - SettingsPacket (客户端 -> 服务端)
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
        // 包ID: 2 - 下一个包
        // CHANNEL.messageBuilder(YourPacket.class, packetId++, direction)
        //         .encoder(YourPacket::encode)
        //         .decoder(YourPacket::decode)
        //         .consumerMainThread(YourPacket::handle)
        //         .add();
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
}