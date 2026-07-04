package com.aljun.zombiegamereborn.network.packet;

import com.aljun.zombiegamereborn.api.ZGRZombieAttributesAPI;
import com.aljun.zombiegamereborn.common.entity.capability.IZombieData;
import com.aljun.zombiegamereborn.common.entity.capability.ZombieDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                if (Minecraft.getInstance().level == null) return;
                Entity entity = Minecraft.getInstance().level.getEntity(this.entityId);
                if (entity instanceof Zombie zombie) {
                    IZombieData data = ZGRZombieAttributesAPI.getZombieData(zombie);
                    ZombieDataProvider.applyData(data, this.dataTag);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
