package com.aljun.zombiegamereborn.network.packet;


import com.aljun.zombiegamereborn.common.client.config.ClientConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


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

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
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

                if (isOp) {
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
        context.setPacketHandled(true);
    }
}
