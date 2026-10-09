package com.aljun.zombiegamereborn.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ZombieCapacitySyncPacket implements CustomPacketPayload {

    public static final Type<ZombieCapacitySyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "zombie_capacity_sync"));

    public static final StreamCodec<FriendlyByteBuf, ZombieCapacitySyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(ZombieCapacitySyncPacket::encode, ZombieCapacitySyncPacket::decode);

    private final int entityId;
    private final CompoundTag dataTag;

    public ZombieCapacitySyncPacket(int entityId, CompoundTag dataTag) {
        this.entityId = entityId;
        this.dataTag = dataTag;
    }

    public ZombieCapacitySyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.dataTag = buffer.readNbt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeNbt(this.dataTag);
    }

    public static ZombieCapacitySyncPacket decode(FriendlyByteBuf buffer) {
        return new ZombieCapacitySyncPacket(buffer);
    }

    public int getEntityId() {
        return entityId;
    }

    public CompoundTag getDataTag() {
        return dataTag;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
