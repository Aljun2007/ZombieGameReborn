package com.aljun.zombiegamereborn.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class ZombieCapacitySyncPacket {

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
}
