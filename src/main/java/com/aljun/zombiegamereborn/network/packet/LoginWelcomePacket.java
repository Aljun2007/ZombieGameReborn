package com.aljun.zombiegamereborn.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class LoginWelcomePacket implements CustomPacketPayload {

    public static final Type<LoginWelcomePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("zombiegamereborn", "login_welcome"));

    public static final StreamCodec<FriendlyByteBuf, LoginWelcomePacket> STREAM_CODEC =
            StreamCodec.ofMember(LoginWelcomePacket::encode, LoginWelcomePacket::decode);

    private final boolean isOp;

    public LoginWelcomePacket(boolean isOp) {
        this.isOp = isOp;
    }

    public LoginWelcomePacket(FriendlyByteBuf buf) {
        this.isOp = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isOp);
    }

    public static LoginWelcomePacket decode(FriendlyByteBuf buffer) {
        return new LoginWelcomePacket(buffer);
    }

    public boolean isOp() {
        return isOp;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
