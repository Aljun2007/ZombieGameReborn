package com.aljun.zombiegamereborn.common.commands;

import com.aljun.zombiegamereborn.common.config.GameProperty;
import com.aljun.zombiegamereborn.common.game.ZGRGame;
import com.aljun.zombiegamereborn.network.ZGRNetwork;
import com.aljun.zombiegamereborn.network.packet.GamePropertyDownloadPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class ConfigCommand {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(GameProperty.class, new GameProperty.GamePropertyAdapter())
            .create();

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("config").then(
                Commands.literal("gui").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    GameProperty currentProperty = ZGRGame.getGameProperty();
                    JsonObject configData = currentProperty.toJsonObject();
                    ZGRNetwork.sendToClient(new GamePropertyDownloadPacket(configData), player);
                    return 0;
                })
        ));
    }
}