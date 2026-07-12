package com.aljun.zombiegamereborn.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public class LoginWelcomePacket {

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
}
