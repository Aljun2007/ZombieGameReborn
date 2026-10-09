package com.aljun.zombiegamereborn.common.entity.sense;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public class PointblankCallback {
    private static BiConsumer<ServerPlayer, Object> handler;

    public static void setHandler(BiConsumer<ServerPlayer, Object> handler) {
        PointblankCallback.handler = handler;
    }

    public static void onGunSync(ServerPlayer sender, Object packet) {
        if (handler != null) {
            handler.accept(sender, packet);
        }
    }
}
